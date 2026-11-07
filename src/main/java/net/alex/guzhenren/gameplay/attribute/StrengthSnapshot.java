package net.alex.guzhenren.gameplay.attribute;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.StrengthAmplifier;
import org.jetbrains.annotations.NotNull;

/**
 * One reading of the strength settlement [力道结算] at one instant, gathered once by {@link StrengthService#snapshot}
 * so a settlement reads the body, the path effects and the amplifiers a single time. It holds the normal carrying
 * capacity [常态承受力], the permanent points [力量点], every running {@link StrengthAmplifier}, whether the All-Out
 * Effort Gu [全力以赴] runs, and what {@link #of} derives from them: the total points, the current capacity and the
 * timed beast points summed by {@link BeastStrengthFamily}, which the round keeper's release reads.
 *
 * <p>Immutable: the amplifier list and the family map are copies. The release past the capacity is not in it, since
 * it depends on the round as well; {@link StrengthService#getUsablePoints(net.minecraft.world.entity.player.Player,
 * StrengthSnapshot)} adds it.
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
                               boolean allOut, int totalPoints, int capacity,
                               @NotNull Map<BeastStrengthFamily, Integer> beastAmplifierPoints) {

    public StrengthSnapshot {
        amplifiers = List.copyOf(amplifiers);
        beastAmplifierPoints = Map.copyOf(beastAmplifierPoints);
    }

    public static @NotNull StrengthSnapshot of(int normalCapacity, int permanentPoints,
                                               @NotNull List<StrengthAmplifier> amplifiers, boolean allOut) {
        Map<BeastStrengthFamily, Integer> beastPoints = new EnumMap<>(BeastStrengthFamily.class);
        for (StrengthAmplifier amplifier : amplifiers) {
            if (amplifier.isBeast()) beastPoints.merge(amplifier.family(), amplifier.points(), Integer::sum);
        }
        return new StrengthSnapshot(normalCapacity, permanentPoints, amplifiers, allOut,
                permanentPoints + StrengthService.getAmplifierPoints(amplifiers),
                StrengthService.getCapacity(normalCapacity, StrengthService.getCapacityBonus(amplifiers), allOut),
                beastPoints);
    }
}
