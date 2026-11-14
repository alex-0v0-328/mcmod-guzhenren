package net.alex.guzhenren.entity.beast;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * 老虎：橙、白两色为共用本类、只差纹理的两个实体类型。
 * <ul>
 *   <li>近身挥击（第 8 tick 命中），3～8 格扑击：第 12 tick 施起跳冲量并当刻锁方向，{@link #onBeginAction}
 *       每次新扑击重置锁定，不复用旧向；落地命中为飞行窗口内扫掠检测。</li>
 *   <li>起跳帧当刻或其后首个 tick 锁定，漏帧不跳过；赶不上落地窗口则放弃起跳；撞墙停下即提前结束，无命中。</li>
 *   <li>{@link #isKeepingMomentum} 自起跳至落地窗口末保持横向动量，纵向弧线交重力：窗口内每 tick 重施速度，
 *       弧线才跨得过锁定距离、不被阻力耗尽；窗口后基础动作 tick 清零横向速度，不滑出。</li>
 *   <li>原版无虎音效，借豹猫一系降调，吼叫用略低沉北极熊警告音；白天只坐，无翻滚、蹭背动画，{@link #getActionAnimation}
 *       映射为待机循环。</li>
 * </ul>
 */

public final class TigerEntity extends BeastEntity {

    public static final int SWIPE_HIT_TICK = 8;
    public static final int SWIPE_END_TICKS = 16;
    public static final int POUNCE_LEAP_TICK = 12;
    public static final int POUNCE_WINDOW_END_TICKS = 18;
    public static final int POUNCE_END_TICKS = 28;
    public static final int ROAR_TICKS = 40;
    public static final int LIE_DOWN_TICKS = 24;
    public static final int GET_UP_TICKS = 20;
    public static final int DEATH_REMOVE_TICK = 32;
    public static final double POUNCE_MIN_DISTANCE = 3.0D;
    public static final double POUNCE_MAX_DISTANCE = 8.0D;
    public static final float SWIPE_DAMAGE = 8.0F;
    public static final float POUNCE_DAMAGE = 12.0F;
    public static final double MAX_HEALTH = 48.0D;
    private static final double FOLLOW_RANGE_BLOCKS = 24.0D;
    private static final double MOVEMENT_SPEED = 0.28D;
    private static final double POUNCE_UPWARD = 0.45D;
    private static final double POUNCE_MIN_SPEED = 0.6D;
    private static final double POUNCE_MAX_SPEED = 1.3D;

    private Vec3 pounceDirection = Vec3.ZERO;
    private Vec3 pounceLastPosition = Vec3.ZERO;
    private double pounceSpeed;

    public TigerEntity(EntityType<? extends TigerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE_BLOCKS);
    }

    @Override
    protected int getSwipeHitTick() { return SWIPE_HIT_TICK; }

    @Override
    protected int getSwipeActionTicks() { return SWIPE_END_TICKS; }

    @Override
    protected int getRoarActionTicks() { return ROAR_TICKS; }

    @Override
    protected int getLieDownActionTicks() { return LIE_DOWN_TICKS; }

    @Override
    protected int getUpActionTicks() { return GET_UP_TICKS; }

    @Override
    protected int getDeathRemoveTick() { return DEATH_REMOVE_TICK; }

    @Override
    protected float getSwipeDamage() { return SWIPE_DAMAGE; }

    @Override
    protected float getHeavyDamage() { return POUNCE_DAMAGE; }

    @Override
    protected double getSwipeKnockback() { return 0.6D; }

    @Override
    protected double getSwipeUpward() { return 0.0D; }

    @Override
    protected boolean isActiveHunter() { return true; }

    @Override
    protected double getPursuitSpeed() { return 1.5D; }

    @Override
    protected double getCullExtraHeight() { return 0.2D; }

    @Override
    protected double getCullHorizontalInflate() { return 0.8D; }

    @Override
    protected void chooseAttack(LivingEntity target) {
        double distance = this.distanceTo(target);
        if (this.canSwipe(target)) {
            this.tryStartAction(Action.ATTACK_SWIPE);
        } else if (distance >= POUNCE_MIN_DISTANCE && distance <= POUNCE_MAX_DISTANCE
                && this.getHeavyCooldown() == 0 && this.canStartLunge(target, POUNCE_MAX_DISTANCE)) {
            this.tryStartAction(Action.ATTACK_HEAVY);
        }
    }

    @Override
    protected void onBeginAction(Action next) {
        super.onBeginAction(next);
        if (next == Action.ATTACK_HEAVY) this.pounceDirection = Vec3.ZERO;
    }

    @Override
    protected void tickHeavyAttack(long ticks) {
        LivingEntity target = this.actionTarget;
        if (ticks < POUNCE_LEAP_TICK) {
            if (target == null || !this.canAttackTarget(target)) this.finishAction();
            return;
        }
        if (this.pounceDirection.lengthSqr() == 0.0D) {
            if (ticks >= POUNCE_WINDOW_END_TICKS || target == null || !this.canAttackTarget(target)) {
                this.finishAction();
                return;
            }
            this.pounceDirection = this.getDirectionOrFacing(target);
            this.faceDirection(this.pounceDirection);
            this.pounceSpeed = Mth.clamp(this.distanceTo(target) / 6.0D, POUNCE_MIN_SPEED, POUNCE_MAX_SPEED);
            this.setDeltaMovement(this.pounceDirection.x * this.pounceSpeed, POUNCE_UPWARD,
                    this.pounceDirection.z * this.pounceSpeed);
            this.hasImpulse = true;
            this.pounceLastPosition = this.position();
            return;
        }
        if (this.horizontalCollision) {
            this.finishAction();
            return;
        }
        Vec3 current = this.position();
        if (this.isKeepingMomentum(ticks)) {
            Vec3 velocity = this.getDeltaMovement();
            this.setDeltaMovement(this.pounceDirection.x * this.pounceSpeed, velocity.y,
                    this.pounceDirection.z * this.pounceSpeed);
        }
        if (!this.hitSettled && ticks < POUNCE_WINDOW_END_TICKS && target != null
                && this.canAttackTarget(target)
                && this.isHitBySweep(target, this.pounceLastPosition, current)) {
            this.hitSettled = true;
            this.applyAttack(target, this.getHeavyDamage(), 1.0D, 0.2D, this.pounceDirection);
        }
        this.pounceLastPosition = current;
        if (ticks >= POUNCE_END_TICKS) this.finishAction();
    }

    @Override
    protected boolean isKeepingMomentum(long ticks) {
        return ticks >= POUNCE_LEAP_TICK && ticks < POUNCE_WINDOW_END_TICKS;
    }

    @Override
    protected Action pickDaytimeAmbient(double roll) {
        return Action.SIT;
    }

    @Override
    protected String getHeavyAttackAnimation() { return "animation.attack_pounce"; }

    @Override
    protected RawAnimation getActionAnimation(Action action) {
        if (action == Action.ROLL || action == Action.BACK_SCRATCH) {
            return RawAnimation.begin().thenLoop("animation.idle");
        }
        return super.getActionAnimation(action);
    }

    @Override
    protected SoundEvent getRoarSound() { return SoundEvents.POLAR_BEAR_WARNING; }

    @Override
    protected float getRoarPitch() { return 0.9F; }

    @Override
    protected SoundEvent getStepSound() { return SoundEvents.WOLF_STEP; }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.OCELOT_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.OCELOT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.OCELOT_DEATH; }

    @Override
    public float getVoicePitch() { return 0.8F; }
}
