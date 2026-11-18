package net.alex.guzhenren.entity.gu;

import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 歇息型飞行蛊（{@link RestingFlyingGuEntity}）共用的逃离玩家 AI 目标。
 * <ul>
 *   <li>当 6 格内出现非创造、非旁观模式玩家时触发逃跑，拉开至 10 格外停止；{@link #canContinueToUse} 每 tick 复查，玩家
 *       切入创造/旁观模式或跨维度时立即解除。</li>
 *   <li>逃跑采样点使用 {@link AirAndWaterRandomPos}（地面随机采样工具在空中会找不到落点）；若蛊虫正处于歇息或降落阶段，
 *       启动时先调用 {@link RestingFlyingGuEntity#takeOff()} 起飞再寻路，防止卡在地面。</li>
 *   <li>未采到点、采样点反而更靠近威胁、寻路不可达或 {@link #tick} 续寻路失败时，随机退避 10～20 tick 再试。</li>
 * </ul>
 */

public class FleePlayerGoal extends Goal {

    private static final double FLEE_SPEED_MODIFIER = 3.0D;
    private static final int ESCAPE_HORIZONTAL_RANGE = 16;
    private static final int ESCAPE_VERTICAL_RANGE = 7;
    private static final float ESCAPE_CONE_ANGLE = (float) (Math.PI / 2);
    private static final int RETRY_MIN_TICKS = 10;
    private static final int RETRY_JITTER_TICKS = 11;
    private final RestingFlyingGuEntity gu;
    private @Nullable Player threat;
    private @Nullable Vec3 escapeCourse;
    private long retryTime;

    public FleePlayerGoal(RestingFlyingGuEntity gu) {
        this.gu = gu;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (gu.level().getGameTime() < retryTime) return false;
        threat = findNearestThreat(gu);
        if (threat == null) return false;
        escapeCourse = findCourseAwayFromThreat(threat);
        if (escapeCourse != null && threat.distanceToSqr(escapeCourse) >= threat.distanceToSqr(gu)
                && gu.getNavigation().createPath(escapeCourse.x, escapeCourse.y, escapeCourse.z, 0) != null) {
            return true;
        }
        backOff();
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return threat != null && threat.isAlive() && isThreat(threat) && threat.level() == gu.level()
                && gu.distanceTo(threat) < RestingFlyingGuEntity.ESCAPE_RANGE;
    }

    @Override
    public void start() {
        if (gu.getPhase() != RestingFlyingGuEntity.FlightPhase.FLYING) gu.takeOff();
        if (escapeCourse != null) {
            gu.getNavigation().moveTo(escapeCourse.x, escapeCourse.y, escapeCourse.z, FLEE_SPEED_MODIFIER);
        }
    }

    @Override
    public void stop() {
        threat = null;
        escapeCourse = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        if (threat == null || !gu.getNavigation().isDone() || gu.level().getGameTime() < retryTime) return;
        Vec3 course = findCourseAwayFromThreat(threat);
        if (course == null || !gu.getNavigation().moveTo(course.x, course.y, course.z, FLEE_SPEED_MODIFIER)) {
            backOff();
        }
    }

    private void backOff() {
        retryTime = gu.level().getGameTime() + RETRY_MIN_TICKS + gu.getRandom().nextInt(RETRY_JITTER_TICKS);
    }

    private @Nullable Vec3 findCourseAwayFromThreat(@NotNull Player threat) {
        Vec3 away = gu.position().subtract(threat.position());
        return AirAndWaterRandomPos.getPos(gu, ESCAPE_HORIZONTAL_RANGE, ESCAPE_VERTICAL_RANGE, 0,
                away.x, away.z, ESCAPE_CONE_ANGLE);
    }

    static @Nullable Player findNearestThreat(RestingFlyingGuEntity gu) {
        return gu.level().getNearestPlayer(gu.getX(), gu.getY(), gu.getZ(), RestingFlyingGuEntity.FLEE_RANGE,
                FleePlayerGoal::isThreat);
    }

    private static boolean isThreat(Entity entity) {
        return entity instanceof Player player && !player.isSpectator() && !player.isCreative();
    }
}
