package net.alex.guzhenren.item.gu.mortal.rule;

import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Amplify Effect Gu [功倍蛊]: Rule Path [律道], ranks II to V, never driven on its own. It multiplies another Gu's
 * effect by its own rank number, ×2 to ×5, and pays its own essence and hunger for that one use, two ways:
 * held in the offhand while a main-hand Gu pays out, or held in the main hand while the radial menu [扇形选择器]
 * drives a hotbar Gu ({@link #driveFromHotbar}).
 *
 * <p>Extends {@link TendedGuItem}: refined in the main hand like any tended Gu, and fed from the bag around each
 * billed use and on the day rollover. Once refined, the client turns its right click into the radial menu, so its
 * own use never reaches the server but by a forged packet, and is refused; from the offhand it does not answer the
 * click at all, so a refused main-hand Gu keeps its own reason on the action bar.
 *
 * <p>A Gu answers it through a module that declares {@code canBeAmplified} and asks {@link #amplify} from its own
 * payout, multiplying its effect's length: {@code TimedBuffModule} (Self Reliance included),
 * {@code AllOutEffortModule}, the timed {@code SkinModule} (Jade Skin, White Jade), {@code VitalityLeafModule} and
 * {@code WatchModule}; {@link #amplify} answers the factor, the driving or offhand amplifier's, billed there, or 1.
 * {@link #canAmplify} is the one eligibility test (the amplifier refined, the target amplifiable, refined if tended,
 * of its rank or lower), read by the client's menu and by both server paths.
 *
 * <p>⚠ In the offhand it stays idle and unbilled for a Gu it cannot amplify, while wild, and when the aperture
 * cannot pay its essence after the driven Gu paid its own; the driven Gu then lands unamplified. The menu's drive
 * checks the two costs together first and refuses instead. On an empty bar it is forced like any tended Gu: the
 * amplified effect lands, then it dies of exhaustion and leaves its hand.
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @since 1.0.0
 */

public final class AmplifyEffectGuItem extends TendedGuItem {

    private static final String FAILED_OFFHAND = "guzhenren.item.failed.amplify_offhand";
    private static final String TOOLTIP_AMPLIFIES = "guzhenren.item.gu.amplifies";
    private static @Nullable Driving driving;

    private record Driving(ServerPlayer player, ItemStack amplifier) {}

    /** What one billed use came to: the factor on the driven Gu's effect and the Gu that paid it, if any. */
    public record Amplification(int factor, @Nullable AmplifyEffectGuItem gu) {

        public static final Amplification NONE = new Amplification(1, null);

        public boolean isAmplified() { return gu != null; }
    }

    public AmplifyEffectGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
    }

    public int getMultiplier() { return getRank().ordinal(); }

    public boolean canAmplify(ItemStack amplifier, ItemStack target) {
        return isRefined(amplifier) && target.getItem() instanceof MortalGuItem driven && driven.canBeAmplified()
                && driven.getRank().ordinal() <= getRank().ordinal()
                && (!(driven instanceof TendedGuItem tended) || tended.isRefined(target));
    }

    public static int amplify(ServerPlayer player, MortalGuItem driven) { return bill(player, driven).factor(); }

    public static @NotNull Amplification bill(ServerPlayer player, MortalGuItem driven) {
        ItemStack stack = driving != null && driving.player() == player ? driving.amplifier() : player.getOffhandItem();
        if (!(stack.getItem() instanceof AmplifyEffectGuItem amplifier) || !driven.canBeAmplified()) {
            return Amplification.NONE;
        }
        return amplifier.tryBill(player, stack, driven) ? new Amplification(amplifier.getMultiplier(), amplifier)
                : Amplification.NONE;
    }

    public static void driveFromHotbar(ServerPlayer player, int slot) {
        if (!Inventory.isHotbarSlot(slot) || slot == player.getInventory().selected) return;

        ItemStack stack = player.getMainHandItem();
        ItemStack target = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof AmplifyEffectGuItem amplifier) || !amplifier.canAmplify(stack, target)) return;

        MortalGuItem driven = (MortalGuItem) target.getItem();
        if (ApertureEssenceService.getSpendable(player) < amplifier.getUseEssence() + driven.getUseEssence()) {
            ActionBar.refuse(player, FAILED_ESSENCE);
            return;
        }
        driving = new Driving(player, stack);
        try {
            driven.tryUseInPlace(player, target);
        } finally {
            driving = null;
        }
    }

    private boolean tryBill(ServerPlayer player, ItemStack stack, MortalGuItem driven) {
        if (!isRefined(stack) || driven.getRank().ordinal() > getRank().ordinal()) return false;
        if (!ApertureEssenceService.consume(player, spec.getEssencePerRound())) return false;

        eat(player, stack);
        if (hunger.isSpendForced(stack) && !player.getAbilities().instabuild) {
            exhaust(player, stack);
            stack.shrink(1);
            return true;
        }
        eat(player, stack);
        if (isHungry(player, stack)) hunger.warn(player, stack, 1L);
        return true;
    }

    @Override
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) { return new Refusal(FAILED_OFFHAND); }

    @Override
    protected void applyEffect(ServerPlayer player, ItemStack stack) {}

    @Override
    protected long useThreshold(ItemStack stack) { return 0L; }

    @Override
    protected boolean hasOffhandUse() { return false; }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(TOOLTIP_AMPLIFIES, getMultiplier()).withStyle(ChatFormatting.GRAY));
    }
}
