package net.alex.guzhenren.entity.gu;

import net.alex.guzhenren.core.Ticks;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.phys.Vec3;

/**
 * 歇息型飞行蛊共用的连续随机飞行 AI 目标。路径走完次 tick 重选；每 100 tick 换航线，每次 1/10 概率请求降落；无航线退避
 * 1 秒；显式阶段检查使降落目标掌管歇息时不重启。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class WanderCourseGoal extends WaterAvoidingRandomFlyingGoal {

    private static final double SPEED_MODIFIER = 1.0D;
    private static final int RECOURSE_TICKS = 5 * Ticks.SECOND;
    private static final int LANDING_ROLL_SIDES = 10;
    private static final int NO_COURSE_RETRY_TICKS = Ticks.SECOND;
    private final RestingFlyingGuEntity gu;
    private int courseTicks;
    private long retryTime;

    public WanderCourseGoal(RestingFlyingGuEntity gu) {
        super(gu, SPEED_MODIFIER);
        this.gu = gu;
    }

    @Override
    public boolean canUse() {
        if (gu.getPhase() != RestingFlyingGuEntity.FlightPhase.FLYING) return false;
        if (gu.level().getGameTime() < retryTime) return false;
        trigger();
        if (!super.canUse()) {
            retryTime = gu.level().getGameTime() + NO_COURSE_RETRY_TICKS;
            return false;
        }
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
