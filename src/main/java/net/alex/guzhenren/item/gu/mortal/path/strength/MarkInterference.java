package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Gu-side gate of Mark Interference [道痕互斥]: refuses a strength Gu whose branch interferes with one the body
 * already holds. The beast and human strength behaviors ask it first, and so does the timed buff behavior for a
 * beast buff; the write funnel in {@link PathStrengthService} enforces the same rule, this only words it.
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthPathBranch#interferesWith
 * @since 1.0.0
 */

public final class MarkInterference {

    private MarkInterference() {}

    private static final String FAILED_MARK_INTERFERENCE = "guzhenren.item.failed.mark_interference";

    public static @Nullable Refusal check(@NotNull Player player, @NotNull StrengthPathBranch branch) {
        return PathStrengthService.getInterference(player, branch) != null
                ? new Refusal(FAILED_MARK_INTERFERENCE) : null;
    }
}
