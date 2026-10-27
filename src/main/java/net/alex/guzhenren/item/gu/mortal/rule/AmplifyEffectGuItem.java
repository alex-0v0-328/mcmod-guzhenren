package net.alex.guzhenren.item.gu.mortal.rule;

import java.util.List;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Amplify Effect Gu [功倍蛊]: Rule Path [律道], ranks II to V, never driven on its own. Held in the offhand while a
 * Gu in the main hand pays out, it multiplies that Gu's effect by its own rank number, ×2 to ×5, and pays its own
 * essence and hunger for that one use.
 *
 * <p>Extends {@link TendedGuItem}: refined and fed in the main hand like any tended Gu; its own use is always
 * refused, and from the offhand it does not answer the click at all, so a refused main-hand Gu keeps its own reason
 * on the action bar.
 *
 * <p>Only some Gu answer it, each asking {@link #amplify} from its own payout and multiplying its effect's length:
 * {@code BuffGuItem} (Self Reliance included), {@code AllOutEffortGuItem}, the timed {@code SkinGuItem} (Jade Skin,
 * White Jade), {@code VitalityLeafGuItem} and {@code WatchGuItem}. A Gu that never asks is never amplified, and the
 * amplifier is never billed for it.
 *
 * <p>⚠ It stays idle and unbilled for a Gu above its own rank, while wild, and when the aperture cannot pay its
 * essence after the driven Gu paid its own; the driven Gu then lands unamplified. On an empty bar it is forced like
 * any tended Gu: the amplified effect lands, then it dies of exhaustion and leaves the offhand.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @since 1.0.0
 */

public final class AmplifyEffectGuItem extends TendedGuItem {

    private static final String FAILED_OFFHAND = "guzhenren.item.failed.amplify_offhand";
    private static final String TOOLTIP_AMPLIFIES = "guzhenren.item.gu.amplifies";

    public AmplifyEffectGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    public int getMultiplier() { return getRank().ordinal(); }

    /** The factor {@code driven}'s effect lands with: the offhand amplifier's, billed here, or 1. */
    public static int amplify(ServerPlayer player, MortalGuItem driven) {
        ItemStack stack = player.getOffhandItem();
        if (!(stack.getItem() instanceof AmplifyEffectGuItem amplifier)) return 1;
        return amplifier.tryBill(player, stack, driven) ? amplifier.getMultiplier() : 1;
    }

    private boolean tryBill(ServerPlayer player, ItemStack stack, MortalGuItem driven) {
        if (!isRefined(stack) || driven.getRank().ordinal() > getRank().ordinal()) return false;
        if (!ApertureEssenceService.consume(player, spec.getEssencePerRound())) return false;

        if (clock.isSpendForced(stack) && !player.getAbilities().instabuild) {
            exhaust(player, stack);
            stack.shrink(1);
            return true;
        }
        if (isHungry(player, stack)) clock.warn(player, stack, 1L);
        return true;
    }

    @Override
    protected @Nullable Refusal checkPayoutGate(Player player, ItemStack stack) { return new Refusal(FAILED_OFFHAND); }

    @Override
    protected void payOut(ServerPlayer player, ItemStack stack) {}

    @Override
    protected long useThreshold(ItemStack stack) { return 0L; }

    @Override
    protected boolean hasOffhandUse() { return false; }

    @Override
    protected void appendTooltip(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                 @NotNull TooltipFlag flag) {
        super.appendTooltip(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(TOOLTIP_AMPLIFIES, getMultiplier()).withStyle(ChatFormatting.GRAY));
    }
}
