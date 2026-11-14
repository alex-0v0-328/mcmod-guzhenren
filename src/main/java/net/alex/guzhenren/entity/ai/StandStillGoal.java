package net.alex.guzhenren.entity.ai;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * 休息姿势（大型野兽坐、卧、睡、翻滚、蹭背，野猪吃草）且无目标时停住导航的 AI 目标；起止由实体动作状态机定，只占
 * {@code MOVE}，闲逛带不走生物。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
