package net.alex.guzhenren.item.gu.mortal.wood;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
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
 * Nine Leaf Vitality Grass [九叶生机草]: a wood path grass that grows Vitality Leaf Gu [生机叶蛊], nine at most.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.TendedGuItem} but declares no clock -- never eats, never starves,
 * and has no way to mend its health. Refined right click picks one leaf into the bag for free; sneak + right click
 * pays {@code essencePerLeaf} essence [真元] over a {@link #GROW_CHARGE_TICKS} charge to grow one. Leaves also
 * regrow by themselves, one per {@code regrowTicks}; {@link LeafGrowth} settles the count lazily from its stamp.
 *
 * <p>⚠ A stack without the component holds nine: the grass picked from its flower, a wild one and a creative copy
 * are all full. {@link #checkPayoutGate} answers both clicks, because {@code TendedGuItem.checkSneakGate} also
 * ends in it.
 *
 * <p>⚠ The regrowth runs on the grass's own clock and is never scaled by Time Path [宙道] self-time; only the
 * holder's grow charge is shortened, like every other charge.
 *
 * <p>{@link #PICKED} is the item model property: {@code 0} full (seven leaves and up), {@link
 * LeafGrowth#PICKED_HALF} half, {@link LeafGrowth#PICKED_BARE} empty.
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
    private static final String FAILED_LEAVES_FULL = "guzhenren.item.failed.leaves_full";
    private static final String TOOLTIP_LEAVES = "guzhenren.item.gu.leaves";
    private static final String TOOLTIP_NEXT_LEAF = "guzhenren.item.gu.next_leaf";
    private final int regrowTicks;
    private final int essencePerLeaf;

    public NineLeafVitalityGrassItem(Properties properties, int regrowTicks, int essencePerLeaf, GuSpec spec) {
        super(properties, spec);
        this.regrowTicks = regrowTicks;
        this.essencePerLeaf = essencePerLeaf;
    }

    //region the leaves -- settled from the stamp on read, on either side
    public static LeafGrowth getGrowth(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LEAF_GROWTH, LeafGrowth.FULL);
    }

    public int getLeaves(ItemStack stack, long now) { return getGrowth(stack).settle(now, regrowTicks).leaves(); }

    public float getPickedStage(ItemStack stack, long now) { return LeafGrowth.getPickedStage(getLeaves(stack, now)); }

    private int getSecondsToNextLeaf(ItemStack stack, long now) {
        return (getGrowth(stack).getTicksToNextLeaf(now, regrowTicks) + Ticks.SECOND - 1) / Ticks.SECOND;
    }
    //endregion

    //region both clicks -- a pick needs a leaf, a grow needs room and the essence
    @Override
    protected @Nullable Refusal checkPayoutGate(Player player, ItemStack stack) {
        long now = player.level().getGameTime();
        int leaves = getLeaves(stack, now);
        if (isSneakUse(player, stack)) {
            if (leaves >= LeafGrowth.MAX_LEAVES) return new Refusal(FAILED_LEAVES_FULL);
            return checkEssenceGate(player, essencePerLeaf, FAILED_ESSENCE);
        }
        return leaves > 0 ? null
                : new Refusal(FAILED_NO_LEAVES, Component.literal(String.valueOf(getSecondsToNextLeaf(stack, now))));
    }

    @Override
    protected boolean hasSneakUse(Player player, ItemStack stack) { return isRefined(stack); }

    @Override
    protected int useChargeTicks(Player player, ItemStack stack) {
        return isSneakUse(player, stack)
                ? TimeFlow.shortenWait(player, GROW_CHARGE_TICKS)
                : super.useChargeTicks(player, stack);
    }
    //endregion

    //region picking -- the plain right click, free
    @Override
    protected void payOut(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        if (getLeaves(stack, now) <= 0) return;

        stack.set(ModDataComponents.LEAF_GROWTH, getGrowth(stack).pick(now, regrowTicks));
        player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.VITALITY_LEAF_GU.get()));
    }
    //endregion

    //region growing -- sneak + right click, paid from the holder's own essence
    @Override
    protected int sneakApply(ServerPlayer player, ItemStack stack) {
        if (!ApertureEssenceService.consume(player, essencePerLeaf)) return 0;

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
