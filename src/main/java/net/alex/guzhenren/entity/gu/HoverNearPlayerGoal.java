package net.alex.guzhenren.entity.gu;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 驱动 {@link FlyingGuEntity} 飞向目标玩家并在其眼部附近悬停的 AI 目标。
 * <ul>
 *   <li>⚠ 直接写入速度向量而不走寻路导航：光点直线接近玩家，使用寻路缓存与卡住检测反而会干扰悬停。</li>
 *   <li>启动时通过 {@link FlyingGuEntity#seekTarget} 选取目标，逼近后以基于 {@code tickCount} 的正弦速度分量产生上下轻
 *       飘效果。</li>
 * </ul>
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
