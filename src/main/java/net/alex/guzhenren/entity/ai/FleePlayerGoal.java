package net.alex.guzhenren.entity.ai;

import java.util.EnumSet;
import net.alex.guzhenren.entity.gu.RestingFlyingGuEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The escape reaction shared by flying Gu with a landing and rest lifecycle.
 *
 * <p>A non-creative, non-spectator player within six blocks is a threat. The goal holds until that
 * player is farther than ten blocks; {@link #canContinueToUse} re-checks every tick, since a player
 * who switches to creative or spectator, or leaves the dimension, is no longer the threat the goal
 * started from. The escape point uses {@link AirAndWaterRandomPos} because the normal ground-only
 * random position helper returns no point for a flying mob in open air. A resting or landing Gu
 * takes off before the escape navigation starts, so a threat cannot leave the entity stuck in a
 * ground phase. A search that finds no course, a course toward the threat or no path, and a
 * {@link #tick} repath that fails, back off for 10 to 20 ticks before the next attempt.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
