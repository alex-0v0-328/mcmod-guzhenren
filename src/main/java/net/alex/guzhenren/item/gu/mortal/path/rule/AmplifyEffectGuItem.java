package net.alex.guzhenren.item.gu.mortal.path.rule;

import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
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
 * 功倍蛊[Amplify Effect Gu]物品：二至五转持续照顾型蛊[Tended Gu]，不可单独催动生效，专用于倍增另一只凡蛊的效果。
 * <ul>
 *   <li>将被催动凡蛊的效果乘以功倍蛊自身转数（2～5 倍），并由功倍蛊自行支付一次真元与饱食[Hunger]消耗；握于副手时随主手
 *       蛊催动自动结算，握于主手时通过客户端扇形轮盘选择快捷栏中的目标蛊虫进行原地驱动
 *       （{@link #driveFromHotbar}）。</li>
 *   <li>{@link #canAmplify} 是客户端轮盘与服务端两条结算路径共用的唯一资格判定；{@link #bill} 在目标蛊生效时对当前驱动
 *       中或副手握持的功倍蛊当场扣费并返回 {@link Amplification}（内含效果倍率与付费的功倍蛊实例；未增幅时返回
 *       {@link Amplification#NONE}，倍率为 1、蛊实例为 {@code null}）。</li>
 *   <li>声明了 {@code canBeAmplified} 且在结算时调用 {@link #bill} 的行为会按倍率延长持续时长，包括
 *       {@code TimedBuffBehavior}、{@code AllOutEffortBehavior}、限时 {@code SkinBehavior}（玉皮蛊[Jade Skin Gu]、
 *       白玉蛊[White Jade Gu]）、{@code ShieldBehavior}（天蓬蛊[Heavenly Mugwort Gu]，仅延长时长而不倍增护盾
 *       值）、{@code VitalityLeafBehavior} 与 {@code WatchBehavior}。</li>
 *   <li>作为持续照顾型蛊，仅支持在主手长按炼化，并在每次增幅扣费及每日维护前后自动从背包补食；炼化后客户端将主手右键拦
 *       截为扇形轮盘，若服务端收到直接催动包则一律以「需长按选择增幅目标」拒绝（{@link #useThreshold} 设为 0 以免被真元
 *       门槛抢先拦截）；副手令 {@link #hasOffhandUse} 返回 {@code false}，保证主手蛊被拒时保留其自身的拒绝提示。</li>
 *   <li>⚠ 握于副手时，若主手蛊不支持增幅、功倍蛊自身尚未炼化、或主手蛊扣费后剩余真元与随身元石[Primeval Stone]不足以支
 *       付功倍蛊消耗，功倍蛊保持静默不扣费，主手蛊照常以原倍率生效；而通过主手轮盘驱动（{@link #driveFromHotbar}）时会
 *       先用 {@code PrimevalStoneSupply.canAfford} 校验两只蛊的真元总和，不足则直接拒绝。</li>
 *   <li>饱食条为空时同样触发强行催动[Forced Use]：本次增幅效果照常生效，随后功倍蛊力竭死亡并从手中消失。</li>
 *   <li>唯一可变静态字段 {@code driving} 为服务端单次 {@link #driveFromHotbar} 调用栈内的线程上下文，由
 *       {@code try/finally} 严格包裹，不跨调用或跨玩家留存。</li>
 * </ul>
 */

public final class AmplifyEffectGuItem extends TendedGuItem {

    private static final String FAILED_NEEDS_HOLD = "guzhenren.item.failed.amplify_needs_hold";
    private static final String TOOLTIP_AMPLIFIES = "guzhenren.item.gu.amplifies";
    private static @Nullable Driving driving;

    private record Driving(ServerPlayer player, ItemStack amplifier) {}

    public record Amplification(int factor, @Nullable AmplifyEffectGuItem gu) {

        public static final Amplification NONE = new Amplification(1, null);
    }

    public AmplifyEffectGuItem(GuProperties guProperties) {
        super(guProperties);
    }

    private int getMultiplier() { return getRank().ordinal(); }

    public boolean canAmplify(ItemStack amplifier, ItemStack target) {
        return isRefined(amplifier) && target.getItem() instanceof MortalGuItem driven && driven.canBeAmplified()
                && driven.getRank().ordinal() <= getRank().ordinal()
                && (!(driven instanceof TendedGuItem tended) || tended.isRefined(target));
    }

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
        if (!PrimevalStoneSupply.canAfford(player, amplifier.getUseEssence() + driven.getUseEssence())) {
            ActionBar.refuse(player, Refusal.ESSENCE);
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
        return billAsPartner(player, stack);
    }

    @Override
    protected Refusal checkEffect(Player player, ItemStack stack) { return new Refusal(FAILED_NEEDS_HOLD); }

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
