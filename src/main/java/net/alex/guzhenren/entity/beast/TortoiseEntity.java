package net.alex.guzhenren.entity.beast;

import java.util.List;
import net.alex.guzhenren.entity.ActionEntity;
import net.alex.guzhenren.entity.ai.StandStillGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * 被动龟：攻击集为空的 {@link ActionEntity}；陆龟、石龟（{@link TortoiseSpecies}）为共用本类的两个实体类型。
 * <ul>
 *   <li>从不锁定目标：警戒即待机，{@link #chooseAttack} 不执行，{@link #getCooldownOf} 不可达；受击只播一次受击姿势，
 *       进行中不重启。</li>
 *   <li>游荡完后夜里按 {@link #NIGHT_SLEEP_CHANCE} 睡至天亮，白天按 {@link #EAT_CHANCE} 进食
 *       {@link #EAT_ACTION_TICKS}，均由 {@link StandStillGoal} 定住；无 run 片段，用不到的追击循环映射为 walk。</li>
 *   <li>⚠ GeckoLib 以客户端同步速度 (|vx| + |vz|) / 2 ≥ 0.015 判定行走；游荡实测陆龟约 0.009～0.012、石龟约
 *       0.006～0.008（沿轴到斜 45°），默认阈值下停在 idle、腿不动，故渲染器改用 {@link #WALK_ANIM_THRESHOLD}，停步后约
 *       1 tick 回 idle。</li>
 * </ul>
 */

public final class TortoiseEntity extends ActionEntity<TortoiseEntity.Action> {

    public static final int EAT_ACTION_TICKS = 40;
    public static final int HURT_ACTION_TICKS = 10;
    public static final int DEATH_REMOVE_TICK = 32;
    public static final double NIGHT_SLEEP_CHANCE = 0.5D;
    public static final double EAT_CHANCE = 0.25D;
    public static final float WALK_ANIM_THRESHOLD = 0.004F;
    private static final String ANIMATION_PREFIX = "animation.";
    private static final Action[] ACTIONS = Action.values();

    public TortoiseEntity(EntityType<? extends TortoiseEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes(TortoiseSpecies species) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, species.getMaxHealth())
                .add(Attributes.ARMOR, species.getArmor())
                .add(Attributes.KNOCKBACK_RESISTANCE, species.getKnockbackResistance())
                .add(Attributes.MOVEMENT_SPEED, species.getMovementSpeed());
    }

    //region action set [动作集]
    @Override
    protected Action[] getActionValues() { return ACTIONS; }

    @Override
    protected Action getIdleAction() { return Action.IDLE; }

    @Override
    protected Action getAlertAction() { return Action.IDLE; }

    @Override
    protected Action getDeathAction() { return Action.DEATH; }

    @Override
    protected List<Cooldown> getCooldowns() { return List.of(); }

    @Override
    protected Cooldown getCooldownOf(Action attack) {
        throw new IllegalArgumentException("the tortoise has no attack: " + attack);
    }

    @Override
    protected String getRecoverySaveKey() { return "TortoiseRecoveryTicks"; }

    @Override
    protected int getDeathRemoveTick() { return DEATH_REMOVE_TICK; }

    @Override
    protected double getPursuitSpeed() { return 1.0D; }

    @Override
    protected boolean isFacingTargetInAction() { return false; }
    //endregion

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(4, new StandStillGoal(this, () -> this.getAction().isAmbient()));
    }

    @Override
    protected void chooseAttack(LivingEntity target) {}

    @Override
    protected void tickAction(Action current, long ticks) {
        switch (current) {
            case EAT -> {
                if (ticks >= EAT_ACTION_TICKS) this.finishAction();
            }
            case SLEEP -> {
                if (!this.isNightTime()) this.finishAction();
            }
            case HURT -> {
                if (ticks >= HURT_ACTION_TICKS) this.finishAction();
            }
            case DEATH, IDLE -> {}
        }
    }

    @Override
    protected void reactToHurt(@Nullable LivingEntity attacker) {
        if (this.getAction() != Action.HURT) this.switchAction(Action.HURT);
    }

    @Override
    protected void onWanderComplete() {
        if (!this.isIdle() || this.isDeadOrDying()) return;
        if (this.isNightTime()) {
            if (this.random.nextDouble() < NIGHT_SLEEP_CHANCE) this.tryBeginAction(Action.SLEEP, false);
        } else if (this.random.nextDouble() < EAT_CHANCE) {
            this.tryBeginAction(Action.EAT, false);
        }
    }

    //region sounds [音效]
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TURTLE_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState block) {
        this.playSound(SoundEvents.TURTLE_SHAMBLE, 0.15F, 1.0F);
    }
    //endregion

    //region animation [动画]
    @Override
    protected String getAnimationPrefix() { return ANIMATION_PREFIX; }

    @Override
    protected String getRunAnimationName() { return ANIMATION_PREFIX + "walk"; }

    @Override
    protected RawAnimation getActionAnimation(Action action) {
        RawAnimation clip = RawAnimation.begin();
        String name = ANIMATION_PREFIX + action.getAnimation();
        if (action == Action.DEATH) return clip.thenPlayAndHold(name);
        return action.isLooping() ? clip.thenLoop(name) : clip.thenPlay(name);
    }
    //endregion

    public enum Action implements ActionEntity.ActionFlags {

        IDLE("idle", true, false),
        EAT("eat", false, true),
        SLEEP("sleep", true, true),
        HURT("hurt", false, false),
        DEATH("death", false, false);

        private final String animation;
        private final boolean loop;
        private final boolean ambient;

        Action(String animation, boolean loop, boolean ambient) {
            this.animation = animation;
            this.loop = loop;
            this.ambient = ambient;
        }

        public String getAnimation() { return this.animation; }

        @Override
        public boolean isLooping() { return this.loop; }

        @Override
        public boolean isAttack() { return false; }

        @Override
        public boolean isAmbient() { return this.ambient; }
    }
}
