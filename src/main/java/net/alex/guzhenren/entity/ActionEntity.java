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
 * 野猪、大型野兽（熊、虎）与龟共用服务端权威动作状态机。
 * <ul>
 *   <li>姿势即同步动作 {@code A}，带服务端游戏时间起点与单调序号，迟到的追踪者从首帧重放一次性动作；
 *       {@link #getActionTicks} 计起点后服务端 tick，序号区分同类重复动作。</li>
 *   <li>伤害落在动画命中帧而非接触时，{@link #strikeTarget} 只打仍在触及距离内且可见者；攻击各有 {@link Cooldown}，
 *       随攻击起算，结束再隔 {@link #ATTACK_RECOVERY_TICKS}；冷却与恢复仅服务端计数，只存档、不同步。</li>
 *   <li>攻击中、恢复期、冷却中不起新攻击（{@link #tryStartAction}）；目标当场锁定，新攻击者改不了进行中攻击；
 *       {@link #hitSettled}：命中已结算（中或空）。</li>
 *   <li>目标因距离、视线、规则丢失即结束警戒与未结算攻击（{@link #validateTarget}），已结算命中与击杀照播恢复。</li>
 *   <li>视线走生物感知（每目标每 tick 缓存），仅扫掠检测 {@link #isHitBySweep} 重投射线，因冲撞先移身体再检测；
 *       扑击被拒隔 {@link #LUNGE_RECHECK_TICKS} tick 再扫；冷却表只存进行中冷却，空闲生物跳过
 *       {@link #tickCooldowns}。</li>
 *   <li>子类给动作集（{@link ActionFlags} 枚举，序号即网络 id，含待机、保持目标的警戒、死亡）、冷却与存档键、选招、
 *       动作时序、受击反应、动作动画（每动作建一次、每帧复用）；{@link #getRunAnimationName} 命名追击循环，无 run
 *       片段可改映射。</li>
 *   <li>环境动作（{@link ActionFlags#isAmbient}）即休息姿势，交战即断；{@link #isNightTime}：会睡者共用夜间窗口；
 *       {@code registerGoals}、{@code defineSynchedData} 于构造器内、子类字段赋值前运行，
 *       所触钩子（{@link #registerSpeciesGoals}、{@link #getIdleAction}、{@link #getCooldowns}）只能返回常量。</li>
 *   <li>{@link CombatGoal} 空闲时追击，否则不动；按原版近战节奏（{@code MeleeAttackGoal}）重寻路：目标移动时每 4～10
 *       tick，超 16 格 +5、超 32 格 +10，失败 +15，动作打断行走后立即重算；{@link WanderGoal} 空闲无目标时游荡，完后调
 *       {@link #onWanderComplete}；读档从空闲（或死亡）起。</li>
 *   <li>有意超 300 行：状态机、AI 目标、持久化与战斗几何共享私有状态，宜一体读。</li>
 * </ul>
 */

public abstract class ActionEntity<A extends Enum<A> & ActionEntity.ActionFlags> extends PathfinderMob
        implements GeoEntity {

    public static final int ATTACK_RECOVERY_TICKS = 10;
    public static final int SIGHT_LOSS_TICKS = 5 * Ticks.SECOND;
    private static final int LUNGE_RECHECK_TICKS = 5;
    private static final int NIGHT_START_TICK = 13000;
    private static final int NIGHT_END_TICK = 23000;

    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_ACTION_START = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_ACTION_SEQUENCE = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PURSUING = SynchedEntityData.defineId(
            ActionEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable BaseLoops baseLoops;
    private final RawAnimation[] actionAnimations = new RawAnimation[this.getActionValues().length];
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

    //region hooks [钩子]
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

    protected String getRunAnimationName() {
        return this.getAnimationPrefix() + "run";
    }

    protected boolean isNightTime() {
        long dayTime = this.level().getDayTime() % Ticks.DAY;
        return dayTime >= NIGHT_START_TICK && dayTime <= NIGHT_END_TICK;
    }

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

    //region action state [动作状态]
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

    private boolean isPursuing() {
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

    //region server tick [服务端心跳]
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

    //region combat geometry [战斗几何]
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

    //region harm, death and persistence [受伤、死亡与持久化]
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
        this.clearEngagement();
        this.recoveryTicks = 0;
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
        this.clearEngagement();
        for (Cooldown cooldown : this.getCooldowns()) this.setCooldown(cooldown, tag.getInt(cooldown.saveKey()));
        this.recoveryTicks = Mth.clamp(tag.getInt(this.getRecoverySaveKey()), 0, ATTACK_RECOVERY_TICKS);
        this.entityData.set(DATA_PURSUING, false);
        boolean dead = this.getHealth() <= 0.0F;
        long now = this.level().getGameTime();
        this.writeAction(dead ? this.getDeathAction() : this.getIdleAction(), dead ? now - this.deathTime : now);
    }

    private void clearEngagement() {
        this.setTarget(null);
        this.actionTarget = null;
        this.hitSettled = false;
        this.lostSightTicks = 0;
        this.onActionCleared();
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();
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

    //region animation [动画]
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new ActionAnimationController<>(this, this::handleAnimationState,
                this::getAction, this::getActionTicks, this::getActionSequence));
    }

    private PlayState handleAnimationState(AnimationState<ActionEntity<A>> state) {
        A current = this.getAction();
        if (current != this.getIdleAction()) return state.setAndContinue(this.actionAnimation(current));
        if (this.baseLoops == null) {
            this.baseLoops = BaseLoops.of(this.getAnimationPrefix(), this.getRunAnimationName());
        }
        return state.setAndContinue(!state.isMoving() ? this.baseLoops.idle()
                : this.isPursuing() ? this.baseLoops.run() : this.baseLoops.walk());
    }

    private RawAnimation actionAnimation(A action) {
        RawAnimation animation = this.actionAnimations[action.ordinal()];
        if (animation == null) {
            animation = this.getActionAnimation(action);
            this.actionAnimations[action.ordinal()] = animation;
        }
        return animation;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    //endregion

    public interface ActionFlags {

        boolean isLooping();

        boolean isAttack();

        default boolean isAmbient() { return false; }
    }

    public record Cooldown(int maxTicks, String saveKey) {}

    private record BaseLoops(RawAnimation idle, RawAnimation walk, RawAnimation run) {

        static BaseLoops of(String prefix, String run) {
            return new BaseLoops(RawAnimation.begin().thenLoop(prefix + "idle"),
                    RawAnimation.begin().thenLoop(prefix + "walk"), RawAnimation.begin().thenLoop(run));
        }
    }

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
