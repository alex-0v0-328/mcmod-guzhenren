package net.alex.guzhenren.item.gu.mortal.strength;

import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The shared class behind every beast-strength Gu; the species comes from registration.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.TendedGuItem}. The gate refuses a holder who already
 * carries that species; the apply delegates to
 * {@link PathStrengthService#grant}. A species declares its
 * own family and worth on the enum, so adding one never touches this class.
 *
 * <p>⚠ If an edit here starts to look necessary, a number has been put in the wrong place.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.TendedGuItem
 * @since 1.0.0
 */

public class BeastStrengthGuItem extends TendedGuItem {

    private static final String FAILED_STRENGTH_HELD = "guzhenren.item.failed.beast_strength_held";
    private final BeastStrength beast;

    public BeastStrengthGuItem(Properties properties, BeastStrength beast, GuSpec spec) {
        super(properties, spec);
        this.beast = beast;
    }

    @Override
    protected @Nullable Refusal payoutGate(Player player, ItemStack stack) {
        return PathStrengthService.has(player, beast)
                ? new Refusal(FAILED_STRENGTH_HELD, Component.translatable(beast.getTranslationKey()))
                : null;
    }

    @Override
    protected void payout(ServerPlayer player, ItemStack stack) { PathStrengthService.grant(player, beast); }
}
