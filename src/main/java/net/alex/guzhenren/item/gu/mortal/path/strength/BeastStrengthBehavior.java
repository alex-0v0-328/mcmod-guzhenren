package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The beast-strength behavior [兽力]: grants one species' strength for good; the species comes from registration.
 *
 * <p>The gate refuses a body that holds Human Jun Strength ({@link MarkInterference}), then a holder whose family
 * already carries that species or an equal or stronger one it would replace ({@link PathStrengthData#getCovering},
 * named by the family's reading), then a body whose normal carrying capacity [常态承受力] has no room for it beside
 * the beasts that stay ({@link StrengthService#canAdmit}: amplifiers and the All-Out lift admit nothing); the effect
 * delegates to {@link PathStrengthService#grant}, which swaps a replaced beast out. A species declares its own
 * family and worth on the enum, so adding one never touches this behavior.
 *
 * <p>⚠ If an edit here starts to look necessary, a number has been put in the wrong place.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.tending.TendedGuItem
 * @since 1.0.0
 */

public record BeastStrengthBehavior(BeastStrength beast) implements GuBehavior {

    private static final String FAILED_STRENGTH_HELD = "guzhenren.item.failed.beast_strength_held";
    private static final String FAILED_CAPACITY = "guzhenren.item.failed.beast_strength_capacity";

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        Refusal interference = MarkInterference.check(player, StrengthPathBranch.BEAST_STRENGTH_PHANTOM);
        if (interference != null) return interference;
        PathStrengthData held = PathStrengthService.get(player);
        BeastStrength covering = held.getCovering(beast);
        if (covering != null) {
            BeastStrengthFamily family = covering.getFamily();
            return new Refusal(FAILED_STRENGTH_HELD, ModDisplayText.formatBeastStrength(family,
                    held.getBeastReadings().getOrDefault(family, 1), held.getInnate(family)));
        }

        return StrengthService.canAdmit(player, beast) ? null : new Refusal(FAILED_CAPACITY);
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathStrengthService.grant(player, beast);
    }
}
