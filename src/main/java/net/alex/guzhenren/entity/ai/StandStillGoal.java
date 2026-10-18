package net.alex.guzhenren.entity.ai;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Holds a mob's navigation still while it plays a rest pose and has no target: the big beasts' sit, lie, sleep,
 * roll and scratch, the wild boar's graze. The pose starts and ends in the entity's own action machine; this goal
 * only claims {@code MOVE}, so a stroll cannot walk the mob out of it.
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
