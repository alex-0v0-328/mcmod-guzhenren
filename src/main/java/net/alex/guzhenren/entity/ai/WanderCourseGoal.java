package net.alex.guzhenren.entity.ai;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.entity.RestingFlyingGuEntity;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.phys.Vec3;

/**
 * The continuous random flight goal shared by the resting flying Gu entities.
 *
 * <p>A finished path is selected again on the next tick. Every 100 ticks the current course is
 * replaced, and each replacement has a one-in-ten chance to request a landing. The explicit phase
 * check keeps the goal from restarting while the landing goal owns a ground rest.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class WanderCourseGoal extends WaterAvoidingRandomFlyingGoal {

    private static final double SPEED_MODIFIER = 1.0D;
    private static final int RECOURSE_TICKS = 5 * Ticks.SECOND;
    private static final int LANDING_ROLL_SIDES = 10;
    private final RestingFlyingGuEntity gu;
    private int courseTicks;

    public WanderCourseGoal(RestingFlyingGuEntity gu) {
        super(gu, SPEED_MODIFIER);
        this.gu = gu;
    }

    @Override
    public boolean canUse() {
        if (gu.phase() != RestingFlyingGuEntity.FlightPhase.FLYING) return false;
        trigger();
        if (!super.canUse()) return false;
        rollLanding();
        return true;
    }

    @Override
    public void start() {
        super.start();
        courseTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        courseTicks++;
        if (courseTicks < RECOURSE_TICKS) return;
        courseTicks = 0;
        Vec3 course = getPosition();
        if (course == null) return;
        gu.getNavigation().moveTo(course.x, course.y, course.z, speedModifier);
        rollLanding();
    }

    private void rollLanding() {
        if (gu.getRandom().nextInt(LANDING_ROLL_SIDES) == 0) gu.requestLanding();
    }
}
