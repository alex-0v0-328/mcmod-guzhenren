package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * One reading of the strength settlement [力道结算] at one instant, gathered once by {@link StrengthService#snapshot}
 * so a settlement reads the body, the path effects and the amplifiers a single time. It holds the normal carrying
 * capacity [常态承受力], the permanent points [力量点], every running {@link StrengthAmplifier}, the All-Out Effort Gu's
 * [全力以赴] tier ({@link StrengthService#NO_ALL_OUT} unless it runs on a body holding a branch), and what {@link #of}
 * derives from them: the total points, the current capacity and the body overload [肉体过载] -- the timed beasts'
 * points past the current capacity ({@link StrengthService#getOverloadPoints}), which the body brings to bear anyway.
 *
 * <p>Immutable: the amplifier list is a copy. The release past the plain cut is not in it, since it depends on the
 * round as well; {@link StrengthService#getUsablePoints(net.minecraft.world.entity.player.Player, StrengthSnapshot)}
 * adds it.
 *
 * <p>⚠ Build it through {@link #of}, which keeps the derived components consistent with the gathered ones; the
 * canonical constructor checks nothing.
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthService
 * @since 1.0.0
 */

public record StrengthSnapshot(int normalCapacity, int permanentPoints, @NotNull List<StrengthAmplifier> amplifiers,
                               int allOutTier, int totalPoints, int capacity, int overloadPoints) {

    public StrengthSnapshot {
        amplifiers = List.copyOf(amplifiers);
    }

    public static @NotNull StrengthSnapshot of(int normalCapacity, int permanentPoints, int permanentBeastPoints,
                                               @NotNull List<StrengthAmplifier> amplifiers, int allOutTier) {
        int capacity = StrengthService.getCapacity(normalCapacity, StrengthService.getCapacityBonus(amplifiers),
                allOutTier);
        return new StrengthSnapshot(normalCapacity, permanentPoints, amplifiers, allOutTier,
                permanentPoints + StrengthService.getAmplifierPoints(amplifiers), capacity,
                StrengthService.getOverloadPoints(capacity, permanentBeastPoints,
                        StrengthService.getTimedBeastPoints(amplifiers)));
    }

    public boolean allOut() { return allOutTier != StrengthService.NO_ALL_OUT; }
}
