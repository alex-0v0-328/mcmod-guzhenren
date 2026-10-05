package net.alex.guzhenren.entity;

import java.util.EnumSet;
import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Server-authoritative action state machine shared by the wild boar and the big beasts (bears, tigers).
 *
 * <p>Every pose is a synchronized action {@code A} with a server game-time start and a monotonic sequence, so
 * late trackers replay one-shot actions from their first frame: {@link #action} is the current one,
 * {@link #actionTicks} the elapsed server game ticks since its start, and {@link #actionSequence} tells
 * repeated actions of one type apart. Damage lands on animation hit frames, never on contact. Each attack has
 * its own {@link Cooldown}, which starts with the attack; a finished attack adds
 * {@link #ATTACK_RECOVERY_TICKS} before the next one.
 *
 * <p>The subclass supplies the action set ({@link #actionValues}, with {@link #idleAction}, the target-keeping
 * {@link #alertAction} and {@link #deathAction}), its cooldowns and save keys, the attack choice
 * ({@link #chooseAttack}), every non-idle action's timing ({@link #tickAction}), the reaction to a hit
 * ({@link #reactToHurt}) and each non-idle action's animation ({@link #actionAnimation}). {@code registerGoals}
 * and {@code defineSynchedData} run inside the entity constructor, before subclass fields are assigned, so the
 * hooks they reach ({@link #registerSpeciesGoals}, {@link #idleAction}, {@link #cooldowns}) return constants.
 *
 * <p>{@link #startAction} starts a server action: attacks refuse to start while another attack is in
 * progress, during recovery or under their cooldown, and the target is captured at once so a new attacker
 * cannot redirect an attack already in progress. {@link #hitSettled} marks the running attack's hit as
 * resolved, landed or missed. In {@link #validateTarget}, a target lost to range, sight or the rules ends a
 * running alert and an unsettled attack; a settled hit still plays its recovery pose, even when it killed
 * the target.
 *
 * <p>{@link CombatGoal} pursues the target while idle and holds still otherwise; {@link WanderGoal} strolls
 * while idle and untargeted and calls {@link #onWanderComplete} after a completed walk. A save keeps only the
 * cooldowns and the recovery; every load starts idle, or dead.
 *
 * <p>Past 300 lines on purpose: the state machine, its goals, persistence and combat geometry share
 * private state and read as one unit.
 */

public abstract class ActionEntity<A extends Enum<A> & ActionEntity.ActionFlags> extends PathfinderMob
        implements GeoEntity {

    public static final int ATTACK_RECOVERY_TICKS = 10;
    public static final int SIGHT_LOSS_TICKS = 5 * Ticks.SECOND;

    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_ACTION_START = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_ACTION_SEQUENCE = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PURSUING = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    protected LivingEntity actionTarget;
    protected boolean hitSettled;
    private int lostSightTicks;
    private int recoveryTicks;

    protected ActionEntity(EntityType<? extends ActionEntity<A>> type, Level level) {
        super(type, level);
    }

    //region hooks
    protected abstract A[] actionValues();

    protected abstract A idleAction();

    protected abstract A alertAction();

    protected abstract A deathAction();

    protected abstract List<Cooldown> cooldowns();

    protected abstract Cooldown cooldownOf(A attack);

    protected abstract String recoverySaveKey();

    protected abstract int deathRemoveTick();

    protected abstract double pursuitSpeed();

    protected abstract void chooseAttack(LivingEntity target);

    protected abstract void tickAction(A current, long ticks);

    protected abstract void reactToHurt(@Nullable LivingEntity attacker);

    protected abstract boolean facesTargetInAction();

    protected abstract void onWanderComplete();

    protected abstract RawAnimation actionAnimation(A action);

    protected abstract String animationPrefix();

    protected void registerSpeciesGoals() {}

    protected void onBeginAction(A next) {}

    protected void onActionCleared() {}

    protected boolean keepsMomentum(long ticks) {
        return false;
    }

    protected double targetRange() {
        return this.getAttributeValue(Attributes.FOLLOW_RANGE);
    }
    //endregion

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CombatGoal(this));
        this.registerSpeciesGoals();
        this.goalSelector.addGoal(6, new WanderGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, id(this.idleAction()));
        builder.define(DATA_ACTION_START, 0L);
        builder.define(DATA_ACTION_SEQUENCE, 0);
        for (Cooldown cooldown : this.cooldowns()) builder.define(cooldown.data(), 0);
        builder.define(DATA_PURSUING, false);
    }

    //region action state
    public A action() {
        A[] actions = this.actionValues();
        byte id = this.entityData.get(DATA_ACTION);
        return id >= 0 && id < actions.length ? actions[id] : this.idleAction();
    }

    public long actionTicks() {
        if (this.isIdle()) return 0L;
        long elapsed = this.level().getGameTime() - this.entityData.get(DATA_ACTION_START);
        return Math.max(0L, elapsed);
    }

    public int actionSequence() {
        return this.entityData.get(DATA_ACTION_SEQUENCE);
    }

    public boolean pursuing() {
        return this.entityData.get(DATA_PURSUING);
    }

    protected boolean isIdle() {
        return this.action() == this.idleAction();
    }

    protected int cooldown(Cooldown cooldown) {
        return this.entityData.get(cooldown.data());
    }

    public boolean startAction(A next) {
        if (this.level().isClientSide() || next == null || this.isDeadOrDying() || this.action().isAttack()) {
            return false;
        }
        if (next.isAttack() && (this.recoveryTicks > 0 || !this.canAttackTarget(this.getTarget())
                || this.cooldown(this.cooldownOf(next)) > 0)) return false;
        return this.beginAction(next, true);
    }

    protected boolean beginAction(A next, boolean applyCooldown) {
        if (next == this.idleAction() && this.isIdle()) return false;
        if (next.isAttack()) {
            this.actionTarget = this.getTarget();
            this.hitSettled = false;
            if (applyCooldown) this.setCooldown(this.cooldownOf(next), this.cooldownOf(next).maxTicks());
        } else if (next != this.alertAction()) {
            this.actionTarget = null;
        }
        this.onBeginAction(next);
        this.writeAction(next, this.level().getGameTime());
        this.getNavigation().stop();
        return true;
    }

    protected void finishAction() {
        A current = this.action();
        if (current == this.idleAction() || current == this.deathAction()) return;
        this.recoveryTicks = current.isAttack() ? ATTACK_RECOVERY_TICKS : 0;
        this.actionTarget = null;
        this.hitSettled = false;
        this.onActionCleared();
        this.writeAction(this.idleAction(), this.level().getGameTime());
    }

    protected void switchAction(A next) {
        this.actionTarget = null;
        this.onActionCleared();
        this.writeAction(next, this.level().getGameTime());
        this.getNavigation().stop();
    }

    private void writeAction(A next, long start) {
        this.entityData.set(DATA_ACTION, id(next));
        this.entityData.set(DATA_ACTION_START, start);
        this.entityData.set(DATA_ACTION_SEQUENCE, this.actionSequence() + 1);
    }

    private static byte id(Enum<?> action) {
        return (byte) action.ordinal();
    }
    //endregion

    //region server tick
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.isDeadOrDying()) return;
        this.tickCooldowns();
        this.validateTarget();
        LivingEntity target = this.getTarget();
        this.entityData.set(DATA_PURSUING, target != null);
        A current = this.action();
        if (target != null && current.isAmbient()) {
            this.finishAction();
            current = this.action();
        }
        if (current != this.idleAction()) {
            this.tickCurrentAction(current);
        } else if (this.recoveryTicks == 0 && target != null && this.canAttackTarget(target)) {
            this.chooseAttack(target);
        }
    }

    private void tickCurrentAction(A current) {
        long ticks = this.actionTicks();
        if (!this.keepsMomentum(ticks)) {
            this.stopInPlace();
            this.setZza(0.0F);
            Vec3 velocity = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, velocity.y, 0.0D);
        }
        this.tickAction(current, ticks);
    }

    private void tickCooldowns() {
        for (Cooldown cooldown : this.cooldowns()) {
            int ticks = this.cooldown(cooldown);
            if (ticks > 0) this.entityData.set(cooldown.data(), ticks - 1);
        }
        if (this.recoveryTicks > 0) this.recoveryTicks--;
    }

    private void setCooldown(Cooldown cooldown, int ticks) {
        this.entityData.set(cooldown.data(), Mth.clamp(ticks, 0, cooldown.maxTicks()));
    }

    private void validateTarget() {
        LivingEntity target = this.getTarget();
        if (target != null && this.canAttackTarget(target)) {
            if (this.hasLineOfSight(target)) {
                this.lostSightTicks = 0;
                return;
            }
            if (++this.lostSightTicks < SIGHT_LOSS_TICKS) return;
        }
        if (target != null) this.setTarget(null);
        this.lostSightTicks = 0;
        if (this.action() == this.alertAction() || this.action().isAttack() && !this.hitSettled) this.finishAction();
    }
    //endregion

    //region combat geometry
    protected boolean canAttackTarget(@Nullable LivingEntity target) {
        if (target == null || target == this || target.isRemoved() || !target.isAlive() || !target.isAttackable()) {
            return false;
        }
        double range = this.targetRange();
        if (target.level() != this.level() || this.isAlliedTo(target)
                || this.distanceToSqr(target) > range * range) return false;
        return !(target instanceof Player player
                && (player.isSpectator() || player.isCreative()
                || this.level().getDifficulty() == Difficulty.PEACEFUL));
    }

    protected boolean canStartLunge(LivingEntity target, double maxDistance) {
        if (!this.canAttackTarget(target) || !this.onGround() || !this.hasLineOfSight(target)) return false;
        Vec3 direction = this.directionTo(target);
        double distance = Math.min(maxDistance, this.distanceTo(target));
        for (double step = 0.3D; step <= distance; step += 0.3D) {
            Vec3 offset = direction.scale(step);
            if (!this.level().noCollision(this, this.getBoundingBox().move(offset))
                    || !this.hasGroundAhead(this.position().add(offset))) return false;
        }
        return true;
    }

    protected boolean hasGroundAhead(Vec3 position) {
        double radius = this.getBbWidth() * 0.45D;
        for (double x : new double[] { -radius, radius }) {
            for (double z : new double[] { -radius, radius }) {
                BlockPos below = BlockPos.containing(position.x + x, position.y - 0.15D, position.z + z);
                if (!this.level().loadedAndEntityCanStandOn(below, this)) return false;
            }
        }
        return true;
    }

    /** The swept-body hit test for moving attacks: the body's path from {@code from} to {@code to} meets the target. */
    protected boolean sweptHit(LivingEntity target, Vec3 from, Vec3 to) {
        AABB targetBox = target.getBoundingBox();
        double halfWidth = this.getBbWidth() * 0.5D;
        AABB contact = new AABB(targetBox.minX - halfWidth, targetBox.minY - this.getBbHeight(),
                targetBox.minZ - halfWidth, targetBox.maxX + halfWidth, targetBox.maxY,
                targetBox.maxZ + halfWidth);
        return this.hasLineOfSight(target) && (contact.contains(from) || contact.contains(to)
                || contact.clip(from, to).isPresent());
    }

    protected void applyAttack(LivingEntity target, float damage, double horizontalStrength,
                               double upward, Vec3 direction) {
        Vec3 oldMovement = target.getDeltaMovement();
        if (!target.hurt(this.damageSources().mobAttack(this), damage)) return;
        double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double scale = Mth.clamp(1.0D - resistance, 0.0D, 1.0D);
        target.setDeltaMovement(direction.x * horizontalStrength * scale,
                oldMovement.y + upward * scale, direction.z * horizontalStrength * scale);
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    protected Vec3 directionTo(@Nullable Entity target) {
        if (target == null) return Vec3.ZERO;
        Vec3 delta = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        return horizontal.lengthSqr() < 1.0E-8D ? Vec3.ZERO : horizontal.normalize();
    }

    protected Vec3 directionOrFacing(@Nullable Entity target) {
        Vec3 direction = this.directionTo(target);
        if (direction.lengthSqr() > 0.0D) return direction;
        float radians = this.getYRot() * ((float) Math.PI / 180.0F);
        return new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
    }

    protected void faceDirection(Vec3 direction) {
        if (direction.lengthSqr() == 0.0D) return;
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * 180.0D / Math.PI) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }
    //endregion

    //region harm, death and persistence
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (!accepted || this.level().isClientSide() || this.isDeadOrDying()) return accepted;
        this.reactToHurt(resolveAttacker(source));
        return true;
    }

    protected void lockTarget(LivingEntity attacker) {
        this.setTarget(attacker);
        this.lostSightTicks = 0;
    }

    @Nullable
    private static LivingEntity resolveAttacker(DamageSource source) {
        Entity sourceEntity = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        if (sourceEntity instanceof LivingEntity living) return living;
        return direct instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        return false;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        boolean wasDead = this.dead;
        super.die(source);
        if (!this.dead || wasDead) return;
        this.setTarget(null);
        this.actionTarget = null;
        this.recoveryTicks = 0;
        this.onActionCleared();
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();
        this.writeAction(this.deathAction(), this.level().getGameTime());
    }

    @Override
    protected void tickDeath() {
        HeldDeath.tick(this, this.deathRemoveTick());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        for (Cooldown cooldown : this.cooldowns()) tag.putInt(cooldown.saveKey(), this.cooldown(cooldown));
        tag.putInt(this.recoverySaveKey(), this.recoveryTicks);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setTarget(null);
        this.actionTarget = null;
        this.hitSettled = false;
        this.lostSightTicks = 0;
        this.onActionCleared();
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();
        for (Cooldown cooldown : this.cooldowns()) this.setCooldown(cooldown, tag.getInt(cooldown.saveKey()));
        this.recoveryTicks = Mth.clamp(tag.getInt(this.recoverySaveKey()), 0, ATTACK_RECOVERY_TICKS);
        this.entityData.set(DATA_PURSUING, false);
        boolean dead = this.getHealth() <= 0.0F;
        long now = this.level().getGameTime();
        this.writeAction(dead ? this.deathAction() : this.idleAction(), dead ? now - this.deathTime : now);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 1 + this.random.nextInt(3);
    }
    //endregion

    //region animation
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new ActionAnimationController<>(this, this::animationState,
                this::action, this::actionTicks, this::actionSequence));
    }

    private PlayState animationState(AnimationState<ActionEntity<A>> state) {
        A current = this.action();
        if (current != this.idleAction()) return state.setAndContinue(this.actionAnimation(current));
        String clip = !state.isMoving() ? "idle" : this.pursuing() ? "run" : "walk";
        return state.setAndContinue(RawAnimation.begin().thenLoop(this.animationPrefix() + clip));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    //endregion

    /** The flags the state machine reads from a subclass's action enum; the ordinal is the wire id. */
    public interface ActionFlags {

        boolean loops();

        boolean isAttack();

        /** A rest pose that combat engagement interrupts. */
        default boolean isAmbient() { return false; }
    }

    /** One attack's synchronized cooldown: its data slot, the length it starts at, and its save key. */
    protected record Cooldown(EntityDataAccessor<Integer> data, int maxTicks, String saveKey) {}

    private static final class CombatGoal extends Goal {

        private final ActionEntity<?> mob;

        private CombatGoal(ActionEntity<?> mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.mob.getTarget() != null || !this.mob.action().loops();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (this.mob.isIdle() && target != null) {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                this.mob.getNavigation().moveTo(target, this.mob.pursuitSpeed());
            } else {
                this.mob.getNavigation().stop();
                if (target != null && this.mob.facesTargetInAction()) {
                    this.mob.faceDirection(this.mob.directionTo(target));
                }
            }
        }
    }

    private static final class WanderGoal extends WaterAvoidingRandomStrollGoal {

        private final ActionEntity<?> mob;

        private WanderGoal(ActionEntity<?> mob) {
            super(mob, 1.0D);
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            return this.mob.isIdle() && this.mob.getTarget() == null && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.mob.isIdle() && this.mob.getTarget() == null && super.canContinueToUse();
        }

        @Override
        public void stop() {
            boolean completed = !this.mob.getNavigation().isInProgress();
            super.stop();
            if (completed) this.mob.onWanderComplete();
        }
    }
}
