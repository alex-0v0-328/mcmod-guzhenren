package net.alex.guzhenren.item;

import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.registry.item.ModDataComponents;
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
 * The abstract base of everything a Gu hand holds, covering both Gu [蛊虫] and Gu material [蛊材].
 *
 * <p>Extends {@link Item} to add the four right-click templates a leaf fills: the use, the tap use, charge
 * pacing, and Vital Gu [本命蛊] binding. Every gesture is a right click, tapped or held: there is no left-click,
 * swing or sneak template, and none gets added. The charge duration is paced by the gap between the
 * holder's rank and the item's own via {@code useChargeByGap}, never by the stage.
 *
 * <p>⚠ A leaf answering {@code hasTapUse} has two uses on the one click: let go within {@link #TAP_TICKS} lands
 * the tap use ({@code checkTapUse}, {@code performTapUse}) in {@code releaseUsing}, a full charge lands the use, and a
 * release in between lands neither. Its charge is never shorter than {@link #HOLD_MIN_TICKS}, so the two cannot
 * overlap; the press is refused only when both uses refuse, with the tap's reason. Only the Primeval Elder Gu and
 * the Nine Leaf Vitality Grass answer it, and only once refined, so a wild one is still refined by the hold.
 *
 * <p>Vanilla offers a click to the offhand whenever the main hand's use fails or passes, cooldown included. A leaf
 * that works only as a partner held there answers {@code hasOffhandUse} false, so it passes silently instead of
 * replacing the main-hand Gu's refusal on the action bar. A leaf answering {@code keepsRefusedClick} (every tended
 * Gu but the Treasure Lotus) consumes its refusal instead, so the offhand item is not used or placed on it.
 *
 * <p>{@link #tryUseInPlace} uses a stack where it lies, with no hand and no charge: a pick from a radial menu. The
 * vanilla cooldown stops it silently, as it stops a click; {@code checkInPlaceUse} refuses on the action bar, and
 * {@code performInPlaceUse} lands it. It answers whether it landed.
 *
 * <p>A Vital Gu names its aperture by kind ({@code ApertureData.PRIMARY} or {@code SECOND}), never by
 * position, so the binding survives Hope Gu inserting a first aperture ahead of a lone second one wherever
 * the Gu sits: {@code bind} stores the kind of the holder's index, {@code getBoundIndex} maps it back. An
 * aperture has one Vital Gu: {@code bind} also stamps a fresh token, and only the stack whose token the
 * store holds for that kind is the current one ({@code isCurrentVital}); a replaced one is harmless.
 *
 * <p>⚠ The hooks ({@code checkUse}, {@code performUse}, {@code useDurationTicks}, {@code hasTapUse}) are re-read in
 * {@code use}, {@code releaseUsing} and {@code finishUsingItem}, never cached from the press. A leaf wanting its
 * own pacing overrides the hook, not the stage.
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

    protected int getTier() { return rank.ordinal() - Rank.ONE.ordinal(); }

    public record Refusal(String key, Object... args) {}

    //region charge [蓄力] -- paced by the holder's rank against this item's own, never by the stage
    public static final int USE_FAST_TICKS = Ticks.SECOND / 4;
    public static final int USE_SAME_TICKS = Ticks.HALF_SECOND;
    public static final int USE_SLOW_TICKS = Ticks.SECOND;

    protected int getRankGap(Player player) { return ApertureService.getRank(player).ordinal() - rank.ordinal(); }

    protected int useChargeByGap(Player player) {
        int gap = getRankGap(player);
        if (gap > 0) return TimeFlow.shortenWait(player, USE_FAST_TICKS);
        return TimeFlow.shortenWait(player, gap == 0 ? USE_SAME_TICKS : USE_SLOW_TICKS);
    }
    //endregion

    //region Vital Gu
    public static @Nullable UUID getOwner(ItemStack stack) {
        return stack.get(ModDataComponents.VITAL_OWNER);
    }

    public static boolean isVital(ItemStack stack) {
        return stack.has(ModDataComponents.VITAL_OWNER);
    }

    public static boolean isVitalOf(ItemStack stack, Player player) { return player.getUUID().equals(getOwner(stack)); }

    public static boolean isForeignVital(ItemStack stack, Player player) {
        return isVital(stack) && !isVitalOf(stack, player);
    }

    public static int getBoundAperture(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.VITAL_APERTURE, ApertureData.PRIMARY);
    }

    public static int getBoundIndex(ItemStack stack, Player owner) {
        return ApertureService.get(owner).indexOf(getBoundAperture(stack));
    }

    public static long getToken(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.VITAL_TOKEN, 0L);
    }

    public static boolean isCurrentVital(ItemStack stack, ServerPlayer owner) {
        long token = getToken(stack);
        return token != 0L && isVitalOf(stack, owner)
                && token == ApertureStorageService.getVitalToken(owner, getBoundAperture(stack));
    }

    public static void bind(ItemStack stack, ServerPlayer player, int index) {
        long token = player.getRandom().nextLong();
        stack.set(ModDataComponents.VITAL_OWNER, player.getUUID());
        stack.set(ModDataComponents.VITAL_APERTURE, ApertureService.get(player).getKindAt(index));
        stack.set(ModDataComponents.VITAL_TOKEN, token == 0L ? 1L : token);
    }

    public static void unbind(ItemStack stack) {
        stack.remove(ModDataComponents.VITAL_OWNER);
        stack.remove(ModDataComponents.VITAL_APERTURE);
        stack.remove(ModDataComponents.VITAL_TOKEN);
    }
    //endregion

    //region the hooks a leaf fills
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

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return isVital(stack) ? ModDisplayText.formatVital(super.getName(stack)) : super.getName(stack);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) { return isVital(stack) || super.isFoil(stack); }

    protected int getCooldownTicks(ItemStack stack) { return COOLDOWN_TICKS; }

    protected void spend(ServerPlayer player, ItemStack stack, int count) {
        player.getCooldowns().addCooldown(this, TimeFlow.shortenWait(player, getCooldownTicks(stack)));
        if (count > 0 && !player.getAbilities().instabuild) stack.shrink(count);
    }
}
