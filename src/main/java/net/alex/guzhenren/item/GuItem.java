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
 * <p>Extends {@link Item} to add the four right-click templates a leaf fills: plain use, sneak use,
 * charge pacing, and Vital Gu [本命蛊] binding. The charge duration is paced by the gap between the
 * holder's rank and the item's own via {@code useChargeByGap}, never by the stage.
 *
 * <p>A Vital Gu names its aperture by kind ({@code ApertureData.PRIMARY} or {@code SECOND}), never by
 * position, so the binding survives Hope Gu inserting a first aperture ahead of a lone second one wherever
 * the Gu sits: {@code bind} stores the kind of the holder's index, {@code boundIndex} maps it back. An
 * aperture has one Vital Gu: {@code bind} also stamps a fresh token, and only the stack whose token the
 * store holds for that kind is the current one ({@code isCurrentVital}); a replaced one is harmless.
 *
 * <p>⚠ The four hooks ({@code gate}, {@code apply}, {@code useDurationTicks}, {@code hasSneakUse}) are
 * re-read in both {@code use} and {@code finishUsingItem}, so leaving or entering a crouch mid-charge
 * switches which branch lands. A leaf wanting its own pacing overrides the hook, not the stage.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.MortalGuItem
 * @since 1.0.0
 */

public abstract class GuItem extends Item {

    public static final int COOLDOWN_TICKS = 2;
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
        return ModDataComponents.get(stack, ModDataComponents.VITAL_OWNER);
    }

    public static boolean isVital(ItemStack stack) {
        return ModDataComponents.has(stack, ModDataComponents.VITAL_OWNER);
    }

    public static boolean isVitalOf(ItemStack stack, Player player) { return player.getUUID().equals(getOwner(stack)); }

    public static boolean isForeignVital(ItemStack stack, Player player) {
        return isVital(stack) && !isVitalOf(stack, player);
    }

    public static int getBoundAperture(ItemStack stack) {
        return ModDataComponents.getOrDefault(stack, ModDataComponents.VITAL_APERTURE, ApertureData.PRIMARY);
    }

    public static int getBoundIndex(ItemStack stack, Player owner) {
        return ApertureService.get(owner).indexOf(getBoundAperture(stack));
    }

    public static long getToken(ItemStack stack) {
        return ModDataComponents.getOrDefault(stack, ModDataComponents.VITAL_TOKEN, 0L);
    }

    public static boolean isCurrentVital(ItemStack stack, ServerPlayer owner) {
        long token = getToken(stack);
        return token != 0L && isVitalOf(stack, owner)
                && token == ApertureStorageService.getVitalToken(owner, getBoundAperture(stack));
    }

    public static void bind(ItemStack stack, ServerPlayer player, int index) {
        long token = player.getRandom().nextLong();
        ModDataComponents.set(stack, ModDataComponents.VITAL_OWNER, player.getUUID());
        ModDataComponents.set(stack, ModDataComponents.VITAL_APERTURE, ApertureService.get(player).getKindAt(index));
        ModDataComponents.set(stack, ModDataComponents.VITAL_TOKEN, token == 0L ? 1L : token);
    }

    public static void unbind(ItemStack stack) {
        ModDataComponents.remove(stack, ModDataComponents.VITAL_OWNER);
        ModDataComponents.remove(stack, ModDataComponents.VITAL_APERTURE);
        ModDataComponents.remove(stack, ModDataComponents.VITAL_TOKEN);
    }
    //endregion

    //region the hooks a leaf fills
    protected boolean hasUse() { return false; }

    protected @Nullable Refusal checkGate(Player player, ItemStack stack) { return null; }

    protected int apply(ServerPlayer player, ItemStack stack) { return 0; }

    protected int useDurationTicks(Player player, ItemStack stack) { return 0; }

    protected boolean hasSneakUse(Player player, ItemStack stack) { return false; }

    protected @Nullable Refusal checkSneakGate(Player player, ItemStack stack) { return null; }

    protected int sneakApply(ServerPlayer player, ItemStack stack) { return 0; }

    public @Nullable Component chargeCaption(ItemStack stack, int remainingTicks) { return null; }

    public @Nullable Float chargeFraction(ItemStack stack, int remainingTicks) { return null; }

    public int chargeColor(ItemStack stack, int remainingTicks) { return CHARGE_COLOR_DEFAULT; }

    protected boolean canFeedFromOffhand() { return false; }
    //endregion

    private @Nullable Refusal getRefusal(Player player, ItemStack stack, boolean sneak) {
        return sneak ? checkSneakGate(player, stack) : checkGate(player, stack);
    }

    private void refuse(Player player, Refusal refusal) {
        if (player instanceof ServerPlayer server) ActionBar.refuse(server, refusal.key(), refusal.args());
    }

    private void runUse(Player player, ItemStack stack, boolean sneak) {
        if (player instanceof ServerPlayer server) {
            spend(server, stack, sneak ? sneakApply(server, stack) : apply(server, stack));
        }
    }

    @Override
    public final @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                                 @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean sneak = isSneakUse(player, stack);
        if (!sneak && !hasUse()) return super.use(level, player, hand);

        Refusal refusal = getRefusal(player, stack, sneak);
        if (refusal != null) {
            refuse(player, refusal);
            return canFeedFromOffhand() ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
        }
        if (useDurationTicks(player, stack) > 0) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        runUse(player, stack, sneak);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public final @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                                    @NotNull LivingEntity entity) {
        if (!hasUse()) return super.finishUsingItem(stack, level, entity);
        if (!(entity instanceof Player player)) return stack;

        boolean sneak = isSneakUse(player, stack);
        Refusal refusal = getRefusal(player, stack, sneak);
        if (refusal != null) {
            refuse(player, refusal);
        } else {
            runUse(player, stack, sneak);
        }
        return stack;
    }

    public static boolean isCrouching(Player player) { return player.isCrouching(); }

    protected boolean isSneakUse(Player player, ItemStack stack) {
        return isCrouching(player) && hasSneakUse(player, stack);
    }

    /**
     * How long {@code entity} charges this item, in ticks.
     *
     * <p>⚠ A version seam: the loader asks through {@link #getUseDuration}, whose arguments differ between the
     * NeoForge 1.21.1 mainline and the Forge 1.20.1 port (which has no entity there and sets the real duration when
     * the use starts); anything that needs the real total, such as the charge bar, asks here.
     */
    public final int chargeDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        if (!hasUse()) return super.getUseDuration(stack, entity);
        return entity instanceof Player player ? useDurationTicks(player, stack) : 0;
    }

    @Override
    public final int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return chargeDuration(stack, entity);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return hasUse() ? UseAnim.NONE : super.getUseAnimation(stack);
    }

    @Override
    public final void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                      @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        appendTooltip(stack, context.level(), tooltip, flag);
    }

    /**
     * The tooltip lines of this item, each class adding its own after {@code super}.
     *
     * <p>⚠ A version seam: the NeoForge 1.21.1 mainline and Forge 1.20.1 hand {@code appendHoverText} different
     * context arguments, so the base class takes them, passes on the level the tooltip is drawn for ({@code null}
     * when there is none), and subclasses fill this.
     */
    protected void appendTooltip(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                 @NotNull TooltipFlag flag) {
        tooltip.add(ModDisplayText.guLine(rank, path, getKindKey()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return isVital(stack) ? ModDisplayText.vital(super.getName(stack)) : super.getName(stack);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) { return isVital(stack) || super.isFoil(stack); }

    protected int getCooldownTicks(ItemStack stack) { return COOLDOWN_TICKS; }

    protected void spend(ServerPlayer player, ItemStack stack, int count) {
        player.getCooldowns().addCooldown(this, TimeFlow.shortenWait(player, getCooldownTicks(stack)));
        if (count > 0 && !player.getAbilities().instabuild) stack.shrink(count);
    }
}
