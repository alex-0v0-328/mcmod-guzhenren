package net.alex.guzhenren.item;

import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The abstract base of everything a Gu hand holds, covering both Gu [蛊虫] and Gu material [蛊材]: only what the
 * two share, the identity (rank, path and the kind line of the tooltip), the tooltip and the right click.
 *
 * <p>Extends {@link Item} with the right-click templates a leaf fills: the use, the tap use, the charge and the
 * in-place use. Every gesture is a right click, tapped or held: there is no left-click, swing or sneak template,
 * and none gets added. How long a charge runs is the leaf's {@code useDurationTicks}; the Vital Gu [本命蛊] mark
 * and the rank-gap pacing are a mortal Gu's ({@code MortalGuItem}), never a material's.
 *
 * <p>⚠ A leaf answering {@code hasTapUse} has two uses on the one click: let go within {@link #TAP_TICKS} lands
 * the tap use ({@code checkTapUse}, {@code performTapUse}) in {@code releaseUsing}, a full charge lands the use, and a
 * release in between lands neither. Its charge is never shorter than {@link #HOLD_MIN_TICKS}, so the two cannot
 * overlap; the press is refused only when both uses refuse, with the tap's reason. Only the Nine Leaf Vitality
 * Grass answers it, and only once refined, so a wild one is still refined by the hold.
 *
 * <p>Vanilla offers a click to the offhand whenever the main hand's use fails or passes, cooldown included. A leaf
 * that works only as a partner held there answers {@code hasOffhandUse} false, so it passes silently instead of
 * replacing the main-hand Gu's refusal on the action bar. A leaf answering {@code keepsRefusedClick} (every tended
 * Gu but the Treasure Lotus) consumes its refusal instead, so the offhand item is not used or placed on it.
 *
 * <p>{@link #tryUseInPlace} uses a stack where it lies, with no hand and no charge: a pick from a radial menu, or a
 * Primeval Elder Gu's tap. The vanilla cooldown stops it silently, as it stops a click; {@code checkInPlaceUse}
 * refuses on the action bar, and {@code performInPlaceUse} lands it. It answers whether it landed.
 *
 * <p>⚠ The hooks ({@code checkUse}, {@code performUse}, {@code useDurationTicks}, {@code hasTapUse}) are re-read in
 * {@code use}, {@code releaseUsing} and {@code finishUsingItem}, never cached from the press. A leaf wanting its
 * own pacing overrides the hook, not the stage.
 *
 * <p>A refusal whose only news is "wrong realm", "not usable here", "an effect still runs", "it works on its
 * own", "a Gu is hungry", "wrong target", "the bag is full", "not enough essence" or "not
 * yet awakened" uses the shared {@link Refusal} key of that name, wherever it is said (Alex 2026-11-14); a Gu keeps a key of its own only where the line tells the player
 * what to do or names a value.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.MortalGuItem
 * @since 1.0.0
 */

public abstract class GuItem extends Item {

    public static final int COOLDOWN_TICKS = 2;
    public static final int TAP_TICKS = Ticks.SECOND / 4;
    public static final int HOLD_MIN_TICKS = Ticks.HALF_SECOND;
    public static final int CHARGE_COLOR_DEFAULT = 0xFF4FC3F7;
    private final Rank rank;
    private final GuPath path;

    protected GuItem(Properties properties, Rank rank, GuPath path) {
        super(properties);
        this.rank = rank;
        this.path = path;
    }

    protected abstract String getKindKey();

    public Rank getRank() { return rank; }

    public GuPath getPath() { return path; }

    public int getTier() { return rank.ordinal() - Rank.ONE.ordinal(); }

    public record Refusal(String key, Object... args) {
        public static final String RANK_MISMATCH = "guzhenren.item.failed.rank_mismatch";
        public static final String CANNOT_USE = "guzhenren.item.failed.cannot_use";
        public static final String EFFECT_ACTIVE = "guzhenren.item.failed.effect_active";
        public static final String AUTO_EFFECT = "guzhenren.item.failed.auto_effect";
        public static final String GU_HUNGRY = "guzhenren.item.failed.gu_hungry";
        public static final String WRONG_TARGET = "guzhenren.item.failed.wrong_target";
        public static final String BAG_FULL = "guzhenren.item.failed.bag_full";
        public static final String ESSENCE = "guzhenren.item.failed.essence";
        public static final String UNAWAKENED = "guzhenren.item.failed.unawakened";
    }

    //region the hooks a leaf fills [叶类钩子]
    protected boolean hasUse() { return false; }

    protected @Nullable Refusal checkUse(Player player, ItemStack stack) { return null; }

    protected int performUse(ServerPlayer player, ItemStack stack) { return 0; }

    protected int useDurationTicks(Player player, ItemStack stack) { return 0; }

    protected boolean hasTapUse(Player player, ItemStack stack) { return false; }

    protected @Nullable Refusal checkTapUse(Player player, ItemStack stack) { return null; }

    protected int performTapUse(ServerPlayer player, ItemStack stack) { return 0; }

    public @Nullable Component chargeCaption(ItemStack stack, int remainingTicks) { return null; }

    public @Nullable Float chargeFraction(ItemStack stack, int remainingTicks) { return null; }

    public int chargeColor(ItemStack stack, int remainingTicks) { return CHARGE_COLOR_DEFAULT; }

    protected boolean hasOffhandUse() { return true; }

    protected boolean keepsRefusedClick() { return false; }

    protected @Nullable Refusal checkInPlaceUse(Player player, ItemStack stack) { return checkUse(player, stack); }

    protected int performInPlaceUse(ServerPlayer player, ItemStack stack) { return performUse(player, stack); }
    //endregion

    public final boolean tryUseInPlace(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) return false;

        Refusal refusal = checkInPlaceUse(player, stack);
        if (refusal != null) {
            refuse(player, refusal);
            return false;
        }
        spend(player, stack, performInPlaceUse(player, stack));
        return true;
    }

    private @Nullable Refusal getPressRefusal(Player player, ItemStack stack) {
        Refusal refusal = checkUse(player, stack);
        return refusal != null && hasTapUse(player, stack) ? checkTapUse(player, stack) : refusal;
    }

    private int getHoldTicks(Player player, ItemStack stack) {
        int ticks = useDurationTicks(player, stack);
        return hasTapUse(player, stack) ? Math.max(ticks, HOLD_MIN_TICKS) : ticks;
    }

    private void refuse(Player player, Refusal refusal) {
        if (player instanceof ServerPlayer server) ActionBar.refuse(server, refusal.key(), refusal.args());
    }

    private void runUse(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer server) spend(server, stack, performUse(server, stack));
    }

    @Override
    public final @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                                 @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.OFF_HAND && !hasOffhandUse()) return InteractionResultHolder.pass(stack);
        if (!hasUse()) return super.use(level, player, hand);

        Refusal refusal = getPressRefusal(player, stack);
        if (refusal != null) {
            refuse(player, refusal);
            return keepsRefusedClick() ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
        }
        if (getHoldTicks(player, stack) > 0) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        runUse(player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public final @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                                    @NotNull LivingEntity entity) {
        if (!hasUse()) return super.finishUsingItem(stack, level, entity);
        if (!(entity instanceof Player player)) return stack;

        Refusal refusal = checkUse(player, stack);
        if (refusal != null) {
            refuse(player, refusal);
        } else {
            runUse(player, stack);
        }
        return stack;
    }

    @Override
    public final void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity,
                                   int remainingTicks) {
        if (!hasUse() || !(entity instanceof Player player) || !hasTapUse(player, stack)) return;
        if (getHoldTicks(player, stack) - remainingTicks > TAP_TICKS) return;

        Refusal refusal = checkTapUse(player, stack);
        if (refusal != null) {
            refuse(player, refusal);
        } else if (player instanceof ServerPlayer server) {
            spend(server, stack, performTapUse(server, stack));
        }
    }

    @Override
    public final int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        if (!hasUse()) return super.getUseDuration(stack, entity);
        return entity instanceof Player player ? getHoldTicks(player, stack) : 0;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return hasUse() ? UseAnim.NONE : super.getUseAnimation(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(ModDisplayText.formatGuLine(rank, path, getKindKey()).withStyle(ChatFormatting.GRAY));
    }

    protected int getCooldownTicks(ItemStack stack) { return COOLDOWN_TICKS; }

    protected void spend(ServerPlayer player, ItemStack stack, int count) {
        player.getCooldowns().addCooldown(this, TimeFlow.shortenWait(player, getCooldownTicks(stack)));
        if (count > 0 && !player.getAbilities().instabuild) stack.shrink(count);
    }
}
