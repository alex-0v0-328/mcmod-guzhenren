package net.alex.guzhenren.entity;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
 * late trackers replay one-shot actions from their first frame: {@link #getAction} is the current one,
 * {@link #getActionTicks} the elapsed server game ticks since its start, and {@link #getActionSequence} tells
 * repeated actions of one type apart. Damage lands on animation hit frames, never on contact:
 * {@link #strikeTarget} settles the hit and lands it only on a target still in reach and in sight. Each attack
 * has its own {@link Cooldown}, which starts with the attack; a finished attack adds
 * {@link #ATTACK_RECOVERY_TICKS} before the next one. Cooldowns and recovery are server-only counts: no client
 * reads them, so they are saved but never synchronized.
 *
 * <p>The subclass supplies the action set ({@link #getActionValues}, with {@link #getIdleAction}, the target-keeping
 * {@link #getAlertAction} and {@link #getDeathAction}), its cooldowns and save keys, the attack choice
 * ({@link #chooseAttack}), every non-idle action's timing ({@link #tickAction}), the reaction to a hit
 * ({@link #reactToHurt}) and each non-idle action's animation ({@link #getActionAnimation}). {@code registerGoals}
 * and {@code defineSynchedData} run inside the entity constructor, before subclass fields are assigned, so the
 * hooks they reach ({@link #registerSpeciesGoals}, {@link #getIdleAction}, {@link #getCooldowns}) return constants.
 *
 * <p>{@link #tryStartAction} starts a server action: attacks refuse to start while another attack is in
 * progress, during recovery or under their cooldown, and the target is captured at once so a new attacker
 * cannot redirect an attack already in progress. {@link #hitSettled} marks the running attack's hit as
 * resolved, landed or missed. In {@link #validateTarget}, a target lost to range, sight or the rules ends a
 * running alert and an unsettled attack; a settled hit still plays its recovery pose, even when it killed
 * the target. Sight checks go through the mob's sensing, which caches one answer per target per tick; only
 * {@link #isHitBySweep} casts a fresh ray, because a charge moves the body before it tests. After a refusal
 * {@link #canStartLunge} sweeps the lunge path again only after {@link #LUNGE_RECHECK_TICKS} ticks, and the
 * cooldown map holds only running cooldowns, so an idle mob skips {@link #tickCooldowns} entirely.
 *
 * <p>{@link CombatGoal} pursues the target while idle and holds still otherwise. It re-paths on vanilla's melee
 * rhythm ({@code MeleeAttackGoal}): every 4 to 10 ticks while the target keeps moving, 5 or 10 more beyond 16 or 32
 * blocks, 15 more after a failed path, and at once when an action has stopped the walk. {@link WanderGoal} strolls
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
    private static final int LUNGE_RECHECK_TICKS = 5;

    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_ACTION_START = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_ACTION_SEQUENCE = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PURSUING = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Object2IntMap<Cooldown> cooldownTicks = new Object2IntOpenHashMap<>();
    @Nullable
    protected LivingEntity actionTarget;
    protected boolean hitSettled;
    private int lostSightTicks;
    private int recoveryTicks;
    private long nextLungeCheckTime;

    protected ActionEntity(EntityType<? extends ActionEntity<A>> type, Level level) {
        super(type, level);
    }

    //region hooks
    protected abstract A[] getActionValues();

    protected abstract A getIdleAction();

    protected abstract A getAlertAction();

    protected abstract A getDeathAction();

    protected abstract List<Cooldown> getCooldowns();

    protected abstract Cooldown getCooldownOf(A attack);

    protected abstract String getRecoverySaveKey();

    protected abstract int getDeathRemoveTick();

    protected abstract double getPursuitSpeed();

    protected abstract void chooseAttack(LivingEntity target);

    protected abstract void tickAction(A current, long ticks);

    protected abstract void reactToHurt(@Nullable LivingEntity attacker);

    protected abstract boolean isFacingTargetInAction();

    protected abstract void onWanderComplete();

    protected abstract RawAnimation getActionAnimation(A action);

    protected abstract String getAnimationPrefix();

    protected void registerSpeciesGoals() {}

    protected void onBeginAction(A next) {}

    protected void onActionCleared() {}

    protected boolean isKeepingMomentum(long ticks) {
        return false;
    }

    protected double getTargetRange() {
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
        builder.define(DATA_ACTION, toId(this.getIdleAction()));
        builder.define(DATA_ACTION_START, 0L);
        builder.define(DATA_ACTION_SEQUENCE, 0);
        builder.define(DATA_PURSUING, false);
    }

    //region action state
    public A getAction() {
        A[] actions = this.getActionValues();
        byte id = this.entityData.get(DATA_ACTION);
        return id >= 0 && id < actions.length ? actions[id] : this.getIdleAction();
    }

    public long getActionTicks() {
        if (this.isIdle()) return 0L;
        long elapsed = this.level().getGameTime() - this.entityData.get(DATA_ACTION_START);
        return Math.max(0L, elapsed);
    }

    public int getActionSequence() {
        return this.entityData.get(DATA_ACTION_SEQUENCE);
    }

    public boolean isPursuing() {
        return this.entityData.get(DATA_PURSUING);
    }

    protected boolean isIdle() {
        return this.getAction() == this.getIdleAction();
    }

    protected int getCooldown(Cooldown cooldown) {
        return this.cooldownTicks.getInt(cooldown);
    }

    public boolean tryStartAction(A next) {
        if (this.level().isClientSide() || next == null || this.isDeadOrDying() || this.getAction().isAttack()) {
            return false;
        }
        if (next.isAttack() && (this.recoveryTicks > 0 || !this.canAttackTarget(this.getTarget())
                || this.getCooldown(this.getCooldownOf(next)) > 0)) return false;
        return this.tryBeginAction(next, true);
    }

    protected boolean tryBeginAction(A next, boolean applyCooldown) {
        if (next == this.getIdleAction() && this.isIdle()) return false;
        if (next.isAttack()) {
            this.actionTarget = this.getTarget();
            this.hitSettled = false;
            if (applyCooldown) this.setCooldown(this.getCooldownOf(next), this.getCooldownOf(next).maxTicks());
        } else if (next != this.getAlertAction()) {
            this.actionTarget = null;
        }
        this.onBeginAction(next);
        this.writeAction(next, this.level().getGameTime());
        this.getNavigation().stop();
        return true;
    }

    protected void finishAction() {
        A current = this.getAction();
        if (current == this.getIdleAction() || current == this.getDeathAction()) return;
        this.recoveryTicks = current.isAttack() ? ATTACK_RECOVERY_TICKS : 0;
        this.actionTarget = null;
        this.hitSettled = false;
        this.onActionCleared();
        this.writeAction(this.getIdleAction(), this.level().getGameTime());
    }

    protected void switchAction(A next) {
        this.actionTarget = null;
        this.onActionCleared();
        this.writeAction(next, this.level().getGameTime());
        this.getNavigation().stop();
    }

    private void writeAction(A next, long start) {
        this.entityData.set(DATA_ACTION, toId(next));
        this.entityData.set(DATA_ACTION_START, start);
        this.entityData.set(DATA_ACTION_SEQUENCE, this.getActionSequence() + 1);
    }

    private static byte toId(Enum<?> action) {
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
        A current = this.getAction();
        if (target != null && current.isAmbient()) {
            this.finishAction();
            current = this.getAction();
        }
        if (current != this.getIdleAction()) {
            this.tickCurrentAction(current);
        } else if (this.recoveryTicks == 0 && target != null && this.canAttackTarget(target)) {
            this.chooseAttack(target);
        }
    }

    private void tickCurrentAction(A current) {
        long ticks = this.getActionTicks();
        if (!this.isKeepingMomentum(ticks)) {
            this.getNavigation().stop();
            this.setXxa(0.0F);
            this.setYya(0.0F);
            this.setSpeed(0.0F);
            this.setZza(0.0F);
            Vec3 velocity = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, velocity.y, 0.0D);
        }
        this.tickAction(current, ticks);
    }

    private void tickCooldowns() {
        if (!this.cooldownTicks.isEmpty()) {
            for (Cooldown cooldown : this.getCooldowns()) {
                int ticks = this.getCooldown(cooldown);
                if (ticks > 1) this.cooldownTicks.put(cooldown, ticks - 1);
                else if (ticks == 1) this.cooldownTicks.removeInt(cooldown);
            }
        }
        if (this.recoveryTicks > 0) this.recoveryTicks--;
    }

    private void setCooldown(Cooldown cooldown, int ticks) {
        int clamped = Mth.clamp(ticks, 0, cooldown.maxTicks());
        if (clamped > 0) this.cooldownTicks.put(cooldown, clamped);
        else this.cooldownTicks.removeInt(cooldown);
    }

    private void validateTarget() {
        LivingEntity target = this.getTarget();
        if (target != null && this.canAttackTarget(target)) {
            if (this.getSensing().hasLineOfSight(target)) {
                this.lostSightTicks = 0;
                return;
            }
            if (++this.lostSightTicks < SIGHT_LOSS_TICKS) return;
        }
        if (target != null) this.setTarget(null);
        this.lostSightTicks = 0;
        if (this.getAction() == this.getAlertAction() || this.getAction().isAttack() && !this.hitSettled) {
            this.finishAction();
        }
    }
    //endregion

    //region combat geometry
    protected boolean canAttackTarget(@Nullable LivingEntity target) {
        if (target == null || target == this || target.isRemoved() || !target.isAlive() || !target.isAttackable()) {
            return false;
        }
        double range = this.getTargetRange();
        if (target.level() != this.level() || this.isAlliedTo(target)
                || this.distanceToSqr(target) > range * range) return false;
        return !(target instanceof Player player
                && (player.isSpectator() || player.isCreative()
                || this.level().getDifficulty() == Difficulty.PEACEFUL));
    }

    protected boolean canStartLunge(LivingEntity target, double maxDistance) {
        long now = this.level().getGameTime();
        if (now < this.nextLungeCheckTime) return false;
        if (this.isLungePathClear(target, maxDistance)) return true;
        this.nextLungeCheckTime = now + LUNGE_RECHECK_TICKS;
        return false;
    }

    private boolean isLungePathClear(LivingEntity target, double maxDistance) {
        if (!this.canAttackTarget(target) || !this.onGround()
                || !this.getSensing().hasLineOfSight(target)) return false;
        Vec3 direction = this.getDirectionTo(target);
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
        return this.canStandOn(position, -radius, -radius) && this.canStandOn(position, -radius, radius)
                && this.canStandOn(position, radius, -radius) && this.canStandOn(position, radius, radius);
    }

    private boolean canStandOn(Vec3 position, double offsetX, double offsetZ) {
        BlockPos below = BlockPos.containing(position.x + offsetX, position.y - 0.15D, position.z + offsetZ);
        return this.level().loadedAndEntityCanStandOn(below, this);
    }

    /** The swept-body hit test for moving attacks: the body's path from {@code from} to {@code to} meets the target. */
    protected boolean isHitBySweep(LivingEntity target, Vec3 from, Vec3 to) {
        AABB targetBox = target.getBoundingBox();
        double halfWidth = this.getBbWidth() * 0.5D;
        AABB contact = new AABB(targetBox.minX - halfWidth, targetBox.minY - this.getBbHeight(),
                targetBox.minZ - halfWidth, targetBox.maxX + halfWidth, targetBox.maxY,
                targetBox.maxZ + halfWidth);
        return this.hasLineOfSight(target) && (contact.contains(from) || contact.contains(to)
                || contact.clip(from, to).isPresent());
    }

    protected void strikeTarget(double reach, float damage, double horizontalStrength, double upward) {
        this.hitSettled = true;
        LivingEntity target = this.actionTarget;
        if (target == null || !this.canAttackTarget(target) || this.distanceTo(target) > reach
                || !this.getSensing().hasLineOfSight(target)) return;
        this.applyAttack(target, damage, horizontalStrength, upward, this.getDirectionOrFacing(target));
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

    protected Vec3 getDirectionTo(@Nullable Entity target) {
        if (target == null) return Vec3.ZERO;
        Vec3 delta = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        return horizontal.lengthSqr() < 1.0E-8D ? Vec3.ZERO : horizontal.normalize();
    }

    protected Vec3 getDirectionOrFacing(@Nullable Entity target) {
        Vec3 direction = this.getDirectionTo(target);
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
        this.writeAction(this.getDeathAction(), this.level().getGameTime());
    }

    @Override
    protected void tickDeath() {
        HeldDeath.tick(this, this.getDeathRemoveTick());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        for (Cooldown cooldown : this.getCooldowns()) tag.putInt(cooldown.saveKey(), this.getCooldown(cooldown));
        tag.putInt(this.getRecoverySaveKey(), this.recoveryTicks);
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
        for (Cooldown cooldown : this.getCooldowns()) this.setCooldown(cooldown, tag.getInt(cooldown.saveKey()));
        this.recoveryTicks = Mth.clamp(tag.getInt(this.getRecoverySaveKey()), 0, ATTACK_RECOVERY_TICKS);
        this.entityData.set(DATA_PURSUING, false);
        boolean dead = this.getHealth() <= 0.0F;
        long now = this.level().getGameTime();
        this.writeAction(dead ? this.getDeathAction() : this.getIdleAction(), dead ? now - this.deathTime : now);
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
        controllers.add(new ActionAnimationController<>(this, this::handleAnimationState,
                this::getAction, this::getActionTicks, this::getActionSequence));
    }

    private PlayState handleAnimationState(AnimationState<ActionEntity<A>> state) {
        A current = this.getAction();
        if (current != this.getIdleAction()) return state.setAndContinue(this.getActionAnimation(current));
        String clip = !state.isMoving() ? "idle" : this.isPursuing() ? "run" : "walk";
        return state.setAndContinue(RawAnimation.begin().thenLoop(this.getAnimationPrefix() + clip));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    //endregion

    /** The flags the state machine reads from a subclass's action enum; the ordinal is the wire id. */
    public interface ActionFlags {

        boolean isLooping();

        boolean isAttack();

        /** A rest pose that combat engagement interrupts. */
        default boolean isAmbient() { return false; }
    }

    /** One attack's cooldown: the length it starts at and its save key. */
    protected record Cooldown(int maxTicks, String saveKey) {}

    private static final class CombatGoal extends Goal {

        private final ActionEntity<?> mob;
        private @Nullable Vec3 pathedTo;
        private int repathTicks;

        private CombatGoal(ActionEntity<?> mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.mob.getTarget() != null || !this.mob.getAction().isLooping();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.pathedTo = null;
            this.repathTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (this.mob.isIdle() && target != null) {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                this.chase(target);
            } else {
                this.mob.getNavigation().stop();
                this.start();
                if (target != null && this.mob.isFacingTargetInAction()) {
                    this.mob.faceDirection(this.mob.getDirectionTo(target));
                }
            }
        }

        private void chase(LivingEntity target) {
            this.repathTicks = Math.max(this.repathTicks - 1, 0);
            if (this.repathTicks > 0) return;
            if (this.pathedTo != null && target.distanceToSqr(this.pathedTo) < 1.0D
                    && this.mob.getRandom().nextFloat() >= 0.05F) return;

            this.pathedTo = target.position();
            double distance = this.mob.distanceToSqr(target);
            this.repathTicks = 4 + this.mob.getRandom().nextInt(7)
                    + (distance > 1024.0D ? 10 : distance > 256.0D ? 5 : 0);
            if (!this.mob.getNavigation().moveTo(target, this.mob.getPursuitSpeed())) this.repathTicks += 15;
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
