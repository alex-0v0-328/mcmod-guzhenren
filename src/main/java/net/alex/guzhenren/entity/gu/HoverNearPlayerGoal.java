package net.alex.guzhenren.entity.gu;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 飞行蛊在想要的玩家旁悬停的 AI 目标。
 * <ul>
 *   <li>☠ 直接驱动移动向量、从不寻路：光点直飞，路径缓存与卡住检测只会碍事。</li>
 *   <li>目标每 tick 经 {@link FlyingGuEntity#seekTarget} 重读；{@code tickCount} 正弦浮动给悬停加飘动。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.gu.FlyingGuEntity
 * @since 1.0.0
 */

public class HoverNearPlayerGoal extends Goal {

    private static final double APPROACH_SPEED = 0.25;
    private static final double EASING = 0.25;
    private static final double BOB_AMPLITUDE = 0.15;
    private static final double BOB_FREQUENCY = 0.15;
    private static final double HOVER_DRAG = 0.8;
    private static final double HOVER_RANGE_SQR = FlyingGuEntity.HOVER_RANGE * FlyingGuEntity.HOVER_RANGE;
    private static final double DETECT_RANGE_SQR = FlyingGuEntity.DETECT_RANGE * FlyingGuEntity.DETECT_RANGE;
    private final FlyingGuEntity gu;
    private @Nullable Player target;

    public HoverNearPlayerGoal(FlyingGuEntity gu) {
        this.gu = gu;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        target = gu.seekTarget();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && gu.isWanting(target)
                && gu.distanceToSqr(target) <= DETECT_RANGE_SQR;
    }

    @Override
    public void start() { gu.getNavigation().stop(); }

    @Override
    public void stop() { target = null; }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        if (target == null) return;
        gu.getLookControl().setLookAt(target);

        Vec3 toEye = target.getEyePosition().subtract(gu.position());
        if (toEye.lengthSqr() > HOVER_RANGE_SQR) {
            approach(toEye);
            return;
        }
        bob();
    }

    private void approach(Vec3 toEye) {
        Vec3 wanted = toEye.normalize().scale(APPROACH_SPEED);
        gu.setDeltaMovement(gu.getDeltaMovement().add(wanted.subtract(gu.getDeltaMovement()).scale(EASING)));
    }

    private void bob() {
        Vec3 movement = gu.getDeltaMovement();
        double lift = Math.sin(gu.tickCount * BOB_FREQUENCY) * BOB_AMPLITUDE;
        gu.setDeltaMovement(movement.x * HOVER_DRAG, lift, movement.z * HOVER_DRAG);
    }
}
