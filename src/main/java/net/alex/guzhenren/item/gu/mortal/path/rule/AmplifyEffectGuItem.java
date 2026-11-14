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
 * 功倍蛊[Amplify Effect Gu]：律道，二至五转，不单独催动。
 * <ul>
 *   <li>另一只蛊的效果乘自身转数（×2 至 ×5），该次真元[Essence]与饱食[Hunger]自付；副手时随主手蛊结算，主手时随扇形
 *       选择器催动快捷栏蛊（{@link #driveFromHotbar}）。</li>
 *   <li>{@link #canAmplify} 是菜单与两条服务端路径共读的唯一资格判定；{@link #bill} 返回催动中或副手功倍蛊的倍率并就地
 *       计费，否则 1。</li>
 *   <li>声明 {@code canBeAmplified} 且结算时调 {@link #bill} 的行为按倍率乘时长：{@code TimedBuffBehavior}、
 *       {@code AllOutEffortBehavior}、限时 {@code SkinBehavior}（玉皮蛊[Jade Skin Gu]、白玉蛊[White Jade Gu]）、
 *       {@code ShieldBehavior}（天蓬蛊[Heavenly Mugwort Gu]，不乘护盾值）、{@code VitalityLeafBehavior}、
 *       {@code WatchBehavior}。</li>
 *   <li>持续照顾型蛊[Tended Gu]：主手炼化，每次计费催动与每日消耗前后从背包补食；炼化后客户端把右键换成扇形选择器，
 *       自身催动只能来自伪造包，一律拒（{@code useThreshold} 为 0，故提示「需要长按选择增幅目标」而非真元不足）；副手
 *       不响应点击，被拒主手蛊保留自己的动作栏原因。</li>
 *   <li>⚠ 副手时遇不可放大的蛊、自身野生、或被催动蛊付完后真元与随身元石[Primeval Stone]付不起，则闲置不计费，被催动
 *       蛊照常生效不放大；菜单催动先合计两份消耗（{@code PrimevalStoneSupply.canAfford}），不够即拒。</li>
 *   <li>饱食条空时同样被强行催动[Forced Use]：放大效果照常生效，随后它力竭而死离手。</li>
 *   <li>唯一可变静态字段 {@code driving}：调用级服务端线程上下文，try/finally 包住一次催动，不越调用、不跨玩家。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @since 1.0.0
 */

public final class AmplifyEffectGuItem extends TendedGuItem {

    private static final String FAILED_NEEDS_HOLD = "guzhenren.item.failed.amplify_needs_hold";
    private static final String TOOLTIP_AMPLIFIES = "guzhenren.item.gu.amplifies";
    private static @Nullable Driving driving;

    private record Driving(ServerPlayer player, ItemStack amplifier) {}

    /** 一次计费催动的结果：被催动蛊的效果倍率，以及付费的功倍蛊[Amplify Effect Gu]（可为空）。 */
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
