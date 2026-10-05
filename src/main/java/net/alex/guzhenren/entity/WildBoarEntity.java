package net.alex.guzhenren.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Server-authoritative wild boar with retaliatory charge and toss attacks, on the {@link ActionEntity} state
 * machine.
 *
 * <p>A first provocation plays the alert; later hits play the hurt pose. The charge locks its direction only
 * after its wind-up, so the boar keeps facing the target until then; in {@link #tickCharge}, the swept-body
 * test checks the actual path, not the diagonal broad-phase rectangle, and a wall, a ledge or a hit ends the
 * run. After a completed wander the boar sometimes grazes ({@link GrazeGoal}).
 *
 * <p>Just past 300 lines through its timing constants and the graze goal.
 */

public final class WildBoarEntity extends ActionEntity<WildBoarEntity.Action> {

    public static final int FOLLOW_RANGE_BLOCKS = 16;
    public static final int CHARGE_COOLDOWN_TICKS = 100;
    public static final int TOSS_COOLDOWN_TICKS = 40;
    public static final int CHARGE_WINDUP_TICKS = 6;
    public static final int CHARGE_WINDOW_END_TICKS = 16;
    public static final int CHARGE_ACTION_END_TICKS = 24;
    public static final int TOSS_HIT_TICK = 9;
    public static final int TOSS_ACTION_END_TICKS = 16;
    public static final int ALERT_ACTION_TICKS = 16;
    public static final int HURT_ACTION_TICKS = 7;
    public static final int GRAZE_ACTION_TICKS = 100;
    public static final int DEATH_REMOVE_TICK = 32;

    private static final float CHARGE_DAMAGE = 8.0F;
    private static final float TOSS_DAMAGE = 6.0F;
    private static final double CHARGE_STEP = 0.6D;
    private static final double CHARGE_KNOCKBACK = 1.0D;
    private static final double CHARGE_MIN_DISTANCE = 3.0D;
    private static final double CHARGE_MAX_DISTANCE = 8.0D;
    private static final double CHARGE_SCAN_DISTANCE = 6.0D;
    private static final double TOSS_REACH = 2.0D;
    private static final double TOSS_KNOCKBACK = 0.6D;
    private static final double TOSS_UPWARD = 0.5D;
    private static final double GRAZE_CHANCE = 0.25D;
    private static final Action[] ACTIONS = Action.values();

    private static final EntityDataAccessor<Integer> DATA_CHARGE_COOLDOWN = SynchedEntityData.defineId(
            WildBoarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOSS_COOLDOWN = SynchedEntityData.defineId(
            WildBoarEntity.class, EntityDataSerializers.INT);
    private static final Cooldown CHARGE =
            new Cooldown(DATA_CHARGE_COOLDOWN, CHARGE_COOLDOWN_TICKS, "WildBoarChargeCooldown");
    private static final Cooldown TOSS = new Cooldown(DATA_TOSS_COOLDOWN, TOSS_COOLDOWN_TICKS, "WildBoarTossCooldown");
    private static final List<Cooldown> COOLDOWNS = List.of(CHARGE, TOSS);

    private Vec3 chargeDirection = Vec3.ZERO;

    public WildBoarEntity(EntityType<? extends WildBoarEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE_BLOCKS);
    }

    //region action set
    @Override
    protected Action[] actionValues() { return ACTIONS; }

    @Override
    protected Action idleAction() { return Action.IDLE; }

    @Override
    protected Action alertAction() { return Action.ALERT; }

    @Override
    protected Action deathAction() { return Action.DEATH; }

    @Override
    protected List<Cooldown> cooldowns() { return COOLDOWNS; }

    @Override
    protected Cooldown cooldownOf(Action attack) { return attack == Action.ATTACK_CHARGE ? CHARGE : TOSS; }

    @Override
    protected String recoverySaveKey() { return "WildBoarRecoveryTicks"; }

    @Override
    protected int deathRemoveTick() { return DEATH_REMOVE_TICK; }

    @Override
    protected double pursuitSpeed() { return 1.4D; }

    @Override
    protected double targetRange() { return FOLLOW_RANGE_BLOCKS; }

    public int chargeCooldown() {
        return this.cooldown(CHARGE);
    }

    public int tossCooldown() {
        return this.cooldown(TOSS);
    }
    //endregion

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(4, new GrazeGoal(this));
    }

    @Override
    protected void onBeginAction(Action next) {
        if (next == Action.ATTACK_CHARGE) this.chargeDirection = Vec3.ZERO;
    }

    @Override
    protected void onActionCleared() {
        this.chargeDirection = Vec3.ZERO;
    }

    @Override
    protected boolean facesTargetInAction() {
        return this.action() != Action.ATTACK_CHARGE || this.actionTicks() < CHARGE_WINDUP_TICKS;
    }

    @Override
    protected void onWanderComplete() {
        if (this.isIdle() && this.getTarget() == null && this.random.nextDouble() < GRAZE_CHANCE
                && !this.isDeadOrDying()) {
            this.beginAction(Action.GRAZE, false);
        }
    }

    //region attacks
    @Override
    protected void chooseAttack(LivingEntity target) {
        double distance = this.distanceTo(target);
        if (distance <= TOSS_REACH && this.tossCooldown() == 0 && this.hasLineOfSight(target)) {
            this.beginAction(Action.ATTACK_TOSS, true);
        } else if (distance >= CHARGE_MIN_DISTANCE && distance <= CHARGE_MAX_DISTANCE && this.chargeCooldown() == 0
                && this.canStartLunge(target, CHARGE_SCAN_DISTANCE)) {
            this.beginAction(Action.ATTACK_CHARGE, true);
        }
    }

    @Override
    protected void tickAction(Action current, long ticks) {
        switch (current) {
            case ALERT -> {
                if (ticks >= ALERT_ACTION_TICKS) this.finishAction();
            }
            case ATTACK_CHARGE -> {
                if (ticks >= CHARGE_ACTION_END_TICKS) {
                    this.finishAction();
                } else if (ticks >= CHARGE_WINDUP_TICKS && ticks < CHARGE_WINDOW_END_TICKS) {
                    this.tickCharge();
                }
            }
            case ATTACK_TOSS -> {
                if (ticks >= TOSS_HIT_TICK && !this.hitSettled) {
                    this.hitSettled = true;
                    this.performToss();
                }
                if (ticks >= TOSS_ACTION_END_TICKS) this.finishAction();
            }
            case HURT -> {
                if (ticks >= HURT_ACTION_TICKS) this.finishAction();
            }
            case GRAZE -> {
                if (ticks >= GRAZE_ACTION_TICKS || this.getTarget() != null) this.finishAction();
            }
            case DEATH, IDLE -> {}
        }
    }

    private void tickCharge() {
        if (this.hitSettled) return;
        LivingEntity target = this.actionTarget;
        if (target == null || !this.canAttackTarget(target)) {
            this.finishAction();
            return;
        }
        if (this.chargeDirection.lengthSqr() == 0.0D) {
            this.chargeDirection = this.directionTo(target);
            this.faceDirection(this.chargeDirection);
        }
        Vec3 movement = this.chargeDirection.scale(CHARGE_STEP);
        if (!this.hasGroundAhead(this.position().add(movement))) {
            this.hitSettled = true;
            return;
        }
        Vec3 start = this.position();
        this.move(MoverType.SELF, movement);
        Vec3 end = this.position();
        if (this.sweptHit(target, start, end)) {
            this.hitSettled = true;
            this.applyAttack(target, CHARGE_DAMAGE, CHARGE_KNOCKBACK, 0.0D, this.chargeDirection);
        }
        if (this.horizontalCollision || end.distanceToSqr(start) < movement.lengthSqr() * 0.99D) {
            this.hitSettled = true;
        }
    }

    private void performToss() {
        LivingEntity target = this.actionTarget;
        if (target == null || !this.canAttackTarget(target) || this.distanceTo(target) > TOSS_REACH
                || !this.hasLineOfSight(target)) return;
        this.applyAttack(target, TOSS_DAMAGE, TOSS_KNOCKBACK, TOSS_UPWARD, this.directionOrFacing(target));
    }

    @Override
    protected void reactToHurt(@Nullable LivingEntity attacker) {
        boolean newlyProvoked = this.getTarget() == null;
        if (attacker != null && this.canAttackTarget(attacker)) {
            this.lockTarget(attacker);
            if (!this.action().isAttack() && this.action() != Action.DEATH) {
                this.switchAction(newlyProvoked ? Action.ALERT : Action.HURT);
            }
        } else if (!this.action().isAttack() && this.action() != Action.ALERT) {
            this.switchAction(Action.HURT);
        }
    }
    //endregion

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.PIG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIG_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState block) {
        this.playSound(SoundEvents.PIG_STEP, 0.15F, 1.0F);
    }

    @Override
    protected String animationPrefix() { return ""; }

    @Override
    protected RawAnimation actionAnimation(Action action) {
        RawAnimation clip = RawAnimation.begin();
        if (action == Action.DEATH) return clip.thenPlayAndHold(action.animation());
        return action.loops() ? clip.thenLoop(action.animation()) : clip.thenPlay(action.animation());
    }

    /** The synchronized poses with their animation names. */
    public enum Action implements ActionEntity.ActionFlags {

        IDLE("idle", true, false),
        GRAZE("graze", true, false),
        ALERT("alert", false, false),
        ATTACK_CHARGE("attack_charge", false, true),
        ATTACK_TOSS("attack_toss", false, true),
        HURT("hurt", false, false),
        DEATH("death", false, false);

        private final String animation;
        private final boolean loop;
        private final boolean attack;

        Action(String animation, boolean loop, boolean attack) {
            this.animation = animation;
            this.loop = loop;
            this.attack = attack;
        }

        public String animation() { return this.animation; }

        @Override
        public boolean loops() { return this.loop; }

        @Override
        public boolean isAttack() { return this.attack; }
    }

    private static final class GrazeGoal extends Goal {

        private final WildBoarEntity boar;

        private GrazeGoal(WildBoarEntity boar) {
            this.boar = boar;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.boar.action() == Action.GRAZE && this.boar.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() && this.boar.actionTicks() < GRAZE_ACTION_TICKS;
        }

        @Override
        public void tick() {
            this.boar.getNavigation().stop();
        }

        @Override
        public void stop() {
            if (this.boar.action() == Action.GRAZE) this.boar.finishAction();
        }
    }
}
