package net.alex.guzhenren.entity.ai;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * 在实体处于休息姿势（大型野兽坐、卧、睡、翻滚、蹭背，或冲撞者吃草）且无攻击目标时停止导航的 AI 目标；起止由实体动作状
 * 态机判定，仅占用 {@code MOVE} 标志位以防随机游荡带走生物。
 */

public final class StandStillGoal extends Goal {

    private final Mob mob;
    private final BooleanSupplier resting;

    public StandStillGoal(Mob mob, BooleanSupplier resting) {
        this.mob = mob;
        this.resting = resting;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() { return this.resting.getAsBoolean() && this.mob.getTarget() == null; }

    @Override
    public boolean canContinueToUse() { return this.canUse(); }

    @Override
    public void tick() { this.mob.getNavigation().stop(); }
}
