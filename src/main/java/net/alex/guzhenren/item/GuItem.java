package net.alex.guzhenren.item;

import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.time.PathTimeFlowService;
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

    protected abstract String kindKey();

    public Rank rank() { return rank; }

    public GuPath path() { return path; }

    protected int tier() { return rank.ordinal() - Rank.ONE.ordinal(); }

    public record Refusal(String key, Object... args) {}

    //region charge [蓄力] -- paced by the holder's rank against this item's own, never by the stage
    public static final int USE_FAST_TICKS = 5;
    public static final int USE_SAME_TICKS = Ticks.HALF_SECOND;
    public static final int USE_SLOW_TICKS = 20;

    protected int rankGap(Player p) { return ApertureService.rank(p).ordinal() - rank.ordinal(); }

    protected int useChargeByGap(Player player) {
        int gap = rankGap(player);
        if (gap > 0) return PathTimeFlowService.shortenWait(player, USE_FAST_TICKS);
        return PathTimeFlowService.shortenWait(player, gap == 0 ? USE_SAME_TICKS : USE_SLOW_TICKS);
    }
    //endregion

    //region Vital Gu
    public static @Nullable UUID owner(ItemStack s) { return s.get(ModDataComponents.VITAL_OWNER.get()); }

    public static boolean isVital(ItemStack s) { return s.has(ModDataComponents.VITAL_OWNER.get()); }

    public static boolean isVitalOf(ItemStack s, Player p) { return p.getUUID().equals(owner(s)); }

    public static int boundAperture(ItemStack s) {
        return s.getOrDefault(ModDataComponents.VITAL_APERTURE.get(), ApertureData.PRIMARY);
    }

    public static void bind(ItemStack s, Player p, int aperture) {
        s.set(ModDataComponents.VITAL_OWNER.get(), p.getUUID());
        s.set(ModDataComponents.VITAL_APERTURE.get(), aperture);
    }
    //endregion

    //region the hooks a leaf fills
    protected boolean hasUse() { return false; }

    protected @Nullable Refusal gate(Player player, ItemStack stack) { return null; }

    protected int apply(ServerPlayer player, ItemStack stack) { return 0; }

    protected int useDurationTicks(Player player, ItemStack stack) { return 0; }

    protected boolean hasSneakUse(Player player, ItemStack stack) { return false; }

    protected @Nullable Refusal sneakGate(Player player, ItemStack stack) { return null; }

    protected int sneakApply(ServerPlayer player, ItemStack stack) { return 0; }

    public @Nullable Component chargeCaption(ItemStack stack, int remainingTicks) { return null; }

    public @Nullable Float chargeFraction(ItemStack stack, int remainingTicks) { return null; }

    public int chargeColor(ItemStack stack, int remainingTicks) { return CHARGE_COLOR_DEFAULT; }

    protected boolean feedsFromOffhand() { return false; }
    //endregion

    private InteractionResultHolder<ItemStack> refused(ServerPlayer player, Refusal refusal, ItemStack stack) {
        if (player != null) refuse(player, refusal.key(), refusal.args());
        return feedsFromOffhand()
                ? InteractionResultHolder.consume(stack)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public final @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                                 @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean sneak = isSneakUse(player, stack);
        if (!sneak && !hasUse()) return super.use(level, player, hand);

        Refusal refusal = sneak ? sneakGate(player, stack) : gate(player, stack);
        if (refusal != null) {
            return refused(player instanceof ServerPlayer server ? server : null, refusal, stack);
        }
        if (useDurationTicks(player, stack) > 0) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (player instanceof ServerPlayer server) {
            spend(server, stack, sneak ? sneakApply(server, stack) : apply(server, stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public final @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                                    @NotNull LivingEntity entity) {
        if (!hasUse()) return super.finishUsingItem(stack, level, entity);
        if (!(entity instanceof Player player)) return stack;

        boolean sneak = isSneakUse(player, stack);
        Refusal refusal = sneak ? sneakGate(player, stack) : gate(player, stack);
        if (refusal != null) {
            if (player instanceof ServerPlayer server) refuse(server, refusal.key(), refusal.args());
            return stack;
        }
        if (player instanceof ServerPlayer server) {
            spend(server, stack, sneak ? sneakApply(server, stack) : apply(server, stack));
        }
        return stack;
    }

    public static boolean crouching(Player player) { return player.isCrouching(); }

    protected boolean isSneakUse(Player player, ItemStack stack) {
        return crouching(player) && hasSneakUse(player, stack);
    }

    @Override
    public final int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        if (!hasUse()) return super.getUseDuration(stack, entity);
        return entity instanceof Player player ? useDurationTicks(player, stack) : 0;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return hasUse() ? UseAnim.NONE : super.getUseAnimation(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(ModDisplayText.guLine(rank, path, kindKey()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return isVital(stack) ? ModDisplayText.vital(super.getName(stack)) : super.getName(stack);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) { return isVital(stack) || super.isFoil(stack); }

    protected static void refuse(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), true);
    }

    protected static void inform(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }

    protected int cooldownTicks(ItemStack stack) { return COOLDOWN_TICKS; }

    protected void spend(ServerPlayer player, ItemStack stack, int count) {
        player.getCooldowns().addCooldown(this, PathTimeFlowService.shortenWait(player, cooldownTicks(stack)));
        if (count > 0 && !player.hasInfiniteMaterials()) stack.shrink(count);
    }
}
