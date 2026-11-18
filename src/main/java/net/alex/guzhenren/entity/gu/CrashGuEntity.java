package net.alex.guzhenren.entity.gu;

import java.util.function.Supplier;
import net.alex.guzhenren.entity.HeldDeath;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * 野生横冲系甲虫实体：涵盖横冲蛊[Horizontal Crash Gu]、直撞蛊[Vertical Crash Gu]与横冲直撞蛊[Charging Crash Gu]。
 * <ul>
 *   <li>采用独角仙模型，飞行与降落歇息机制同 {@link BoarGuEntity}（导出的 {@code animation.walk} 有意不用）；各变种共用
 *       本类，捕获所得物品、尺寸与属性在注册时注入，贴图由渲染器按实体类型区分。</li>
 *   <li>处于飞行或降落阶段时，若客户端每 tick 水平位移低于 {@link #HOVER_ENTER_SPEED} 则切为循环
 *       {@code animation.hover}，超过 {@link #HOVER_EXIT_SPEED} 才切回飞行动画，用双阈值迟滞防止临界闪烁，且直接读取客
 *       户端位移、无需额外网络同步。</li>
 *   <li>歇息阶段受击且未死时触发 0.3 秒 {@code animation.hurt}（飞行中不播放；实体仅 1 点生命，伤害低于 1 时才会触
 *       发）。</li>
 *   <li>⚠ 死亡时先停止起降触发动画并定格在 {@code animation.death} 原地仰翻，{@link #tickDeath} 必须等到
 *       {@link #DEATH_REMOVE_TICK}（即 {@code TRANSITION_TICKS} 过渡加上 {@link #DEATH_TICKS} 动画时长）再移除实体，若
 *       仅按动画时长移除会截断末段仰翻。</li>
 * </ul>
 */

public class CrashGuEntity extends RestingFlyingGuEntity {

    public static final double HOVER_ENTER_SPEED = 0.02D;
    public static final double HOVER_EXIT_SPEED = 0.04D;
    public static final int DEATH_TICKS = 24;
    public static final int DEATH_REMOVE_TICK = DEATH_TICKS + TRANSITION_TICKS;
    private static final RawAnimation HOVER_ANIM =
            RawAnimation.begin().thenLoop("animation.hover");
    private static final RawAnimation HURT_ANIM =
            RawAnimation.begin().thenPlay("animation.hurt");
    private static final RawAnimation DEATH_ANIM =
            RawAnimation.begin().thenPlayAndHold("animation.death");
    private boolean hovering;

    public CrashGuEntity(EntityType<? extends CrashGuEntity> type, Level level,
                                    Supplier<Item> caughtGu) {
        super(type, level, caughtGu);
    }

    public static boolean isHovering(boolean hovering, double horizontalSpeed) {
        return hovering ? horizontalSpeed < HOVER_EXIT_SPEED : horizontalSpeed < HOVER_ENTER_SPEED;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            hovering = isHovering(hovering, Math.hypot(getX() - xo, getZ() - zo));
        }
    }

    //region harm [受伤]
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && isAlive() && getPhase() == FlightPhase.RESTING) {
            triggerAnim(MAIN, "hurt");
        }
        return hurt;
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        super.die(damageSource);
        if (!level().isClientSide()) stopTriggeredAnim(MAIN, null);
    }

    @Override
    protected void tickDeath() {
        HeldDeath.tick(this, DEATH_REMOVE_TICK);
    }
    //endregion

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(createMainController().triggerableAnim("hurt", HURT_ANIM));
    }

    @Override
    protected RawAnimation getSteadyAnimation() {
        if (isDeadOrDying()) return DEATH_ANIM;
        return hovering && getPhase() != FlightPhase.RESTING ? HOVER_ANIM : super.getSteadyAnimation();
    }
}
