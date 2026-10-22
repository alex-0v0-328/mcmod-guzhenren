package net.alex.guzhenren.gameplay.clock;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * A place where time itself runs at its own rate: {@link TimeFlow#getWorldRate} multiplies the rate of every area
 * added through {@link TimeFlow#addArea} at the player's position into the dimension's. Nothing adds one yet.
 *
 * <p>⚠ Outside its place an area answers 1. ⚠ An area answers the same on client and server, or syncs what its
 * answer reads: item charge times run through {@link TimeFlow#shortenWait} on both sides.
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeFlow
 * @since 1.0.0
 */

@FunctionalInterface
public interface TimeFlowArea {

    double getTimeRate(@NotNull Level level, @NotNull Vec3 position);
}
