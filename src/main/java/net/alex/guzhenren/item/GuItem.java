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
 * 蛊虫与蛊材的抽象基类：只含二者共有的身份、提示与右键。
 * <ul>
 *   <li>身份含转数、道[Path]、种类行；叶类填的模板：催动、轻点、蓄力、原地催动，皆右键轻点或按住，不加左键、挥动、潜行
 *       模板；蓄力时长取叶类 {@code useDurationTicks}；本命蛊标记与转数差节奏只属凡蛊 {@code MortalGuItem}。</li>
 *   <li>⚠ {@code hasTapUse} 叶类一击两用：{@link #TAP_TICKS} 内松开在 {@code releaseUsing} 生效轻点
 *       {@code checkTapUse}/{@code performTapUse}，蓄满则催动，中途松开皆无；蓄力不短于 {@link #HOLD_MIN_TICKS} 故不重
 *       叠；两者皆拒才拒按下，用轻点理由；仅已炼化的九叶生机草[Nine Leaf Vitality Grass] 回答它，野生仍按住炼化。</li>
 *   <li>⚠ 钩子 {@code checkUse}、{@code performUse}、{@code useDurationTicks}、{@code hasTapUse} 在 {@code use}、
 *       {@code releaseUsing}、{@code finishUsingItem} 中重读，不缓存；自定节奏覆写钩子而非阶段。</li>
 *   <li>主手催动失败或放行（含冷却）时原版把点击交给副手：仅作副手配合的叶类 {@code hasOffhandUse} 答 false，静默放行，
 *       不顶替主手蛊的拒绝；{@code keepsRefusedClick} 的叶类（除天元宝莲[Heavenly Essence Treasure Lotus Gu] 外的持续照
 *       顾型蛊[Tended Gu]）吞下拒绝，副手物品不被使用或放置。</li>
 *   <li>{@link #tryUseInPlace} 不经手、不蓄力原地催动（轮盘选取、元老蛊[Primeval Elder Gu] 轻点）：原版冷却静默挡下，
 *       {@code checkInPlaceUse} 在动作栏拒绝，{@code performInPlaceUse} 生效，返回是否生效。</li>
 *   <li>拒绝只为境界不符、无法使用、效果未结束、自动生效、蛊虫饥饿、目标错误、背包已满、真元[Essence] 不足、尚未开
 *       窍[Awakening] 时，处处用 {@link Refusal} 同名共用键（Alex 2026-11-14）；指引操作或给数值才用蛊自己的键。</li>
 * </ul>
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
