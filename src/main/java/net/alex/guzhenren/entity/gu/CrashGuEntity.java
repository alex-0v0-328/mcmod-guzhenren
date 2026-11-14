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
 * 野生横冲系甲虫：横冲蛊[Horizontal Crash Gu]、直撞蛊[Vertical Crash Gu]、横冲直撞蛊[Charging Crash Gu]。
 * <ul>
 *   <li>独角仙模型，飞行歇息同野生豕蛊；四变种共用本类，捕获物、尺寸、属性由注册给，贴图由渲染器给。</li>
 *   <li>动画约定同 {@link BoarGuEntity}；导出的 {@code animation.walk} 有意不用。</li>
 *   <li>飞行或降落中水平速度低于 {@link #HOVER_ENTER_SPEED} 格/tick 改循环 {@code animation.hover}，快过
 *       {@link #HOVER_EXIT_SPEED} 才换回，间隔防闪；速度取客户端每 tick 位移，无需同步。</li>
 *   <li>歇息受击未死播 {@code animation.hurt}（0.3 秒，飞行不播）；1 点生命，伤害不足 1 才不死。</li>
 *   <li>死亡先停起降，定格 {@code animation.death} 原地仰翻（空中亦然）；{@link #tickDeath} 在
 *       {@link #DEATH_REMOVE_TICK} 移除：先过渡 {@code TRANSITION_TICKS} 再播 {@link #DEATH_TICKS}，按动画长度移除会截
 *       掉末段。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
