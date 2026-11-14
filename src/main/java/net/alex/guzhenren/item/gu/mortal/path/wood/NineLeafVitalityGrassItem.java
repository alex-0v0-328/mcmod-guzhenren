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
 * 九叶生机草[Nine Leaf Vitality Grass]：长生机叶蛊[Vitality Leaf Gu]的木道草，至多九片。
 * <ul>
 *   <li>无时钟：不进食、不饿死、不修复生命；炼化后点按免费摘一片入包，{@link #checkEffect} 管摘叶、点按与效果，
 *       {@link #checkOwnUse} 管长叶。</li>
 *   <li>长按蓄力 {@link #GROW_CHARGE_TICKS}、付 {@code essencePerLeaf} 真元[Essence]长一片；同其他催动经
 *       {@code PrimevalStoneSupply.pay}，不足由所带元石[Primeval Stone]补。</li>
 *   <li>⚠ 无组件的物品算九片：花上摘的、野生的、创造复制的都满。</li>
 *   <li>⚠ 自行再生（{@link LeafGrowth}）按草自身时钟，不受宙道时间缩放；后者只缩短长叶蓄力。</li>
 *   <li>{@link #PICKED} 模型属性：满 0，半 {@link LeafGrowth#PICKED_HALF}，空 {@link LeafGrowth#PICKED_BARE}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see LeafGrowth
 * @since 1.0.0
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
