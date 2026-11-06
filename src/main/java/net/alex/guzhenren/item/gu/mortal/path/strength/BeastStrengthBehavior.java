package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.attribute.StrengthService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The beast-strength behavior [兽力]: grants one species' strength for good; the species comes from registration.
 *
 * <p>The gate refuses a body that holds Human Jun Strength ({@link MarkInterference}), then a holder who already
 * carries that species, then a body whose normal carrying capacity [常态承受力] has no room for it beside the
 * beasts it holds ({@link StrengthService#canAdmit}: amplifiers and the All-Out lift admit nothing); the effect
 * delegates to {@link PathStrengthService#grant}. A species declares its own family and worth on the enum, so
 * adding one never touches this behavior. {@code CarvingEffects} finds the Gu that grants a species by this
 * behavior.
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
        if (PathStrengthService.has(player, beast)) {
            return new Refusal(FAILED_STRENGTH_HELD, Component.translatable(beast.getTranslationKey()));
        }

        return StrengthService.canAdmit(player, beast)
                ? null
                : new Refusal(FAILED_CAPACITY, Component.translatable(beast.getTranslationKey()));
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathStrengthService.grant(player, beast);
    }
}
