package net.alex.guzhenren.item.gu.mortal.path.wood;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 九叶生机草[Nine Leaf Vitality Grass]物品：可生长并采摘生机叶蛊[Vitality Leaf Gu]的二转持续照顾型蛊[Tended Gu]，至多蓄
 * 满九片叶。
 * <ul>
 *   <li>无饱食时钟：无需喂食、不会饿死、也不自动修复损伤；已炼化状态下启用「右键一击两用」：右键轻点免费摘下一片生机叶
 *       蛊放入背包（由 {@link #checkEffect} / {@link #checkOwnTapUse} 校验是否有叶，{@link #performTapUse} 走
 *       {@link #drive} 触发 {@link #applyEffect}），长按蓄力 {@link #GROW_CHARGE_TICKS} 则消耗 {@code essencePerLeaf}
 *       点真元催熟一片叶（由 {@link #checkOwnUse} 校验未满九片且真元充足，{@link #performOwnUse} 经
 *       {@link PrimevalStoneSupply#pay} 扣费）。</li>
 *   <li>⚠ 未携带 {@code LEAF_GROWTH} 组件的物品栈默认视为满九片叶（{@link LeafGrowth#FULL}），保证从自然花朵采摘、野生
 *       捕获或创造模式复制的九叶生机草初始均为满叶。</li>
 *   <li>⚠ 叶片的自然再生（{@link LeafGrowth}）按世界时钟结算，不受持有者自身时间流速（{@link TimeFlow}）缩放；时间流速
 *       仅缩短长按催熟叶片的蓄力时长（{@link #useChargeTicks}）。</li>
 *   <li>物品模型属性 {@link #PICKED} 根据当前叶片数映射为满叶（{@code 0.0F}）、半叶（{@link LeafGrowth#PICKED_HALF}）与
 *       光杆（{@link LeafGrowth#PICKED_BARE}）三态贴图。</li>
 * </ul>
 */

public class NineLeafVitalityGrassItem extends TendedGuItem {

    public static final ResourceLocation PICKED = Guzhenren.id("picked");
    private static final int GROW_CHARGE_TICKS = 2 * Ticks.SECOND;
    private static final String FAILED_NO_LEAVES = "guzhenren.item.failed.no_leaves";
    private static final String TOOLTIP_LEAVES = "guzhenren.item.gu.leaves";
    private static final String TOOLTIP_NEXT_LEAF = "guzhenren.item.gu.next_leaf";
    private final int regrowTicks;
    private final int essencePerLeaf;

    public NineLeafVitalityGrassItem(int regrowTicks, int essencePerLeaf, GuProperties guProperties) {
        super(guProperties);
        this.regrowTicks = regrowTicks;
        this.essencePerLeaf = essencePerLeaf;
    }

    //region the leaves [叶片]
    private static LeafGrowth getGrowth(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LEAF_GROWTH, LeafGrowth.FULL);
    }

    public int getLeaves(ItemStack stack, long now) { return getGrowth(stack).settle(now, regrowTicks).leaves(); }

    public float getPickedStage(ItemStack stack, long now) { return LeafGrowth.getPickedStage(getLeaves(stack, now)); }

    private long getSecondsToNextLeaf(ItemStack stack, long now) {
        return ModDisplayText.getCountdownSeconds(getGrowth(stack).getTicksToNextLeaf(now, regrowTicks));
    }
    //endregion

    //region the two uses [两种使用]
    @Override
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) {
        long now = player.level().getGameTime();
        return getLeaves(stack, now) > 0 ? null : new Refusal(FAILED_NO_LEAVES);
    }

    @Override
    protected @Nullable Refusal checkOwnUse(Player player, ItemStack stack) {
        if (getLeaves(stack, player.level().getGameTime()) >= LeafGrowth.MAX_LEAVES) {
            return new Refusal(Refusal.CANNOT_USE);
        }
        return checkEssenceGate(player, essencePerLeaf, Refusal.ESSENCE);
    }

    @Override
    protected boolean hasTapUse(Player player, ItemStack stack) { return isRefined(stack); }

    @Override
    protected @Nullable Refusal checkOwnTapUse(Player player, ItemStack stack) {
        return checkEffect(player, stack);
    }

    @Override
    protected int useChargeTicks(Player player, ItemStack stack) {
        return TimeFlow.shortenWait(player, GROW_CHARGE_TICKS);
    }
    //endregion

    //region picking [采摘]
    @Override
    protected int performTapUse(ServerPlayer player, ItemStack stack) { return drive(player, stack); }

    @Override
    protected void applyEffect(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        if (getLeaves(stack, now) <= 0) return;

        stack.set(ModDataComponents.LEAF_GROWTH, getGrowth(stack).pick(now, regrowTicks));
        player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.VITALITY_LEAF_GU_1.get()));
    }
    //endregion

    //region growing [生长]
    @Override
    protected int performOwnUse(ServerPlayer player, ItemStack stack) {
        if (!PrimevalStoneSupply.pay(player, essencePerLeaf)) return 0;

        stack.set(ModDataComponents.LEAF_GROWTH,
                getGrowth(stack).grow(player.level().getGameTime(), regrowTicks));
        return 0;
    }
    //endregion

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!isRefined(stack)) return;

        Level level = context.level();
        long now = level != null ? level.getGameTime() : getGrowth(stack).settledAt();
        int leaves = getLeaves(stack, now);
        tooltip.add(Component.translatable(TOOLTIP_LEAVES, leaves, LeafGrowth.MAX_LEAVES)
                .withStyle(ChatFormatting.GRAY));
        if (leaves < LeafGrowth.MAX_LEAVES) {
            tooltip.add(Component.translatable(TOOLTIP_NEXT_LEAF, getSecondsToNextLeaf(stack, now))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
