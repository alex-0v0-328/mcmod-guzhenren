package net.alex.guzhenren.item.gu.mortal.strength;

import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The beast-strength module [兽力]: grants one species' strength for good; the species comes from registration.
 *
 * <p>The gate refuses a holder who already carries that species; the effect delegates to
 * {@link PathStrengthService#grant}. A species declares its own family and worth on the enum, so adding one never
 * touches this module. {@code CarvingEffects} finds the Gu that grants a species by this module.
 *
 * <p>⚠ If an edit here starts to look necessary, a number has been put in the wrong place.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.TendedGuItem
 * @since 1.0.0
 */

public record BeastStrengthModule(BeastStrength beast) implements GuModule {

    private static final String FAILED_STRENGTH_HELD = "guzhenren.item.failed.beast_strength_held";

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        return PathStrengthService.has(player, beast)
                ? new Refusal(FAILED_STRENGTH_HELD, Component.translatable(beast.getTranslationKey()))
                : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathStrengthService.grant(player, beast);
    }
}
