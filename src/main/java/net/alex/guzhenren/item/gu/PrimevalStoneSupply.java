package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 元石[Primeval Stone] 唯一的供给线：携带元石的一切自动抽取都经此。
 * <ul>
 *   <li>⚠ 蛊只经 {@link #pay} 付真元[Essence]：先补给线，再付代价，先扣池，余由元石补；灌注[Channel] 或炼化步长按池定，
 *       故先补给（{@link #topUpFor} 其下限）再付。经此的还有补给线（低于 {@link #REFILL_BELOW_PERCENT}% 补，到
 *       {@link #REFILL_UP_TO_PERCENT}% 停）、温养[Nourish] 钱袋、炼化菜单供给槽；{@link #takeFrom} 是元石离开物品堆的唯
 *       一途径，散装或元老蛊[Primeval Elder Gu] 储库皆然。</li>
 *   <li>⚠ 来源顺序固定：任意背包格（含副手）散装元石 → 携带元老蛊存量 → 空窍存储中的元老蛊（不取本命蛊槽），按空窍与槽
 *       位；存储中的在副本上抽取，经 {@code ApertureStorageService.setSlot} 写回，因视图交出的是存储物品堆本身。</li>
 *   <li>⚠ 客户端无空窍存储，其 {@link #canAfford} 只计携带来源，由服务端裁决，按住的催动同步回结果；元石只对有空窍可燃
 *       者计数（{@link #canAfford}）：真元门槛代替 {@code isAwakened}，否则凡人能用元石付蛊。</li>
 *   <li>⚠ 创造模式不抽元石（{@link #draw} 答 0）：补给不倒，池不足部分免除，如创造不吃东西；仅亲手移石（供给槽、温
 *       养）；池定步长者改读 {@link #getPoolAfterTopUp}（可及元石补给后的池），故炼化、灌注节奏同生存持此石时。</li>
 *   <li>☠ 补给线第二份副本会漂移，池会静默截掉超上限倒入的量；{@link #trySpend}：超池的一笔直接燃烧元石，不经上限：池先
 *       付，元石补余，找零回池。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PrimevalStoneItem
 * @see PrimevalElderGuItem
 * @since 1.0.0
 */

public final class PrimevalStoneSupply {

    private PrimevalStoneSupply() {}

    public static final int REFILL_BELOW_PERCENT = 50;
    public static final int REFILL_UP_TO_PERCENT = 80;

    public static long getEssencePerStone() {
        return ModItems.PRIMEVAL_STONE.get() instanceof PrimevalStoneItem stone ? stone.getEssence() : 0L;
    }

    public static int takeFrom(ItemStack source, int wanted) {
        if (wanted <= 0) return 0;
        if (source.getItem() instanceof PrimevalStoneItem) {
            int taken = Math.min(wanted, source.getCount());
            source.shrink(taken);
            return taken;
        }
        return source.getItem() instanceof PrimevalElderGuItem vault ? vault.drawStones(source, wanted) : 0;
    }

    //region the stone top-up [元石补给]
    public static boolean isTopUpNeeded(Player player) {
        long max = ApertureEssenceService.getMaxEssence(player);
        return max > 0L && ApertureEssenceService.getCurrentEssence(player) * 100L < max * REFILL_BELOW_PERCENT;
    }

    public static long topUpDeficit(Player player) {
        return ApertureEssenceService.getMaxEssence(player) * REFILL_UP_TO_PERCENT / 100L
                - ApertureEssenceService.getCurrentEssence(player);
    }

    public static long getPoolAfterTopUp(Player player) {
        long spendable = ApertureEssenceService.getSpendable(player);
        if (!isTopUpNeeded(player)) return spendable;
        return spendable + Math.clamp(topUpDeficit(player), 0L, getWorthOnHand(player));
    }

    public static void topUp(ServerPlayer player) {
        if (isTopUpNeeded(player)) pourInto(player, topUpDeficit(player));
    }

    /** 先走补给线；仍不足 {@code need} 且池容得下时，补到 {@code need}。 */
    public static void topUpFor(ServerPlayer player, long need) {
        topUp(player);
        long missing = need - ApertureEssenceService.getSpendable(player);
        if (missing > 0L && ApertureEssenceService.getMaxEssence(player) >= need) pourInto(player, missing);
    }

    /** 蛊支付真元的入口：仅池与元石合计仍付不起时返回 false，且分文不付。 */
    public static boolean pay(ServerPlayer player, long cost) {
        if (cost <= 0L) return true;
        topUp(player);
        return trySpend(player, cost);
    }
    //endregion

    //region sourcing stones [取石]
    private static void pourInto(ServerPlayer player, long wanted) {
        long each = getEssencePerStone();
        if (wanted <= 0L || each <= 0L) return;
        int taken = draw(player, (int) Math.min(Integer.MAX_VALUE, (wanted + each - 1) / each));
        if (taken > 0) ApertureEssenceService.add(player, taken * each);
    }

    private static int draw(ServerPlayer player, int wanted) {
        if (player.getAbilities().instabuild) return 0;
        int taken = drawFrom(player, wanted, true);
        taken += drawFrom(player, wanted - taken, false);
        return taken + drawFromApertures(player, wanted - taken);
    }

    private static int drawFrom(ServerPlayer player, int wanted, boolean loose) {
        int taken = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && taken < wanted; i++) {
            ItemStack stack = inventory.getItem(i);
            if (loose ? stack.getItem() instanceof PrimevalStoneItem : stack.getItem() instanceof PrimevalElderGuItem) {
                taken += takeFrom(stack, wanted - taken);
            }
        }
        return taken;
    }

    private static int drawFromApertures(ServerPlayer player, int wanted) {
        int taken = 0;
        for (int aperture = 0; aperture < ApertureData.MAX_APERTURES && taken < wanted; aperture++) {
            for (int index = 0; index < ApertureStorageService.count(player, aperture) && taken < wanted; index++) {
                ItemStack stored = ApertureStorageService.getView(player, aperture).get(index);
                if (!(stored.getItem() instanceof PrimevalElderGuItem)) continue;
                ItemStack copy = stored.copy();
                int drawn = takeFrom(copy, wanted - taken);
                if (drawn <= 0) continue;
                ApertureStorageService.setSlot(player, aperture, index, copy);
                taken += drawn;
            }
        }
        return taken;
    }
    //endregion

    //region a lump past the pool [超池支付]
    private static long getWorthOnHand(Player player) {
        long stones = 0L;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof PrimevalStoneItem) stones += stack.getCount();
            if (stack.getItem() instanceof PrimevalElderGuItem) stones += PrimevalElderGuItem.getStored(stack);
        }
        for (int aperture = 0; aperture < ApertureData.MAX_APERTURES; aperture++) {
            for (ItemStack stack : ApertureStorageService.getView(player, aperture)) {
                if (stack.getItem() instanceof PrimevalElderGuItem) stones += PrimevalElderGuItem.getStored(stack);
            }
        }
        return stones * getEssencePerStone();
    }

    public static boolean canAfford(Player player, long cost) {
        long spendable = ApertureEssenceService.getSpendable(player);
        if (spendable >= cost) return true;
        return ApertureEssenceService.getMaxEssence(player) > 0L && spendable + getWorthOnHand(player) >= cost;
    }

    public static boolean trySpend(ServerPlayer player, long cost) {
        if (cost <= 0L) return true;
        long each = getEssencePerStone();
        if (each <= 0L || !canAfford(player, cost)) return false;

        long fromPool = Math.min(ApertureEssenceService.getSpendable(player), cost);
        ApertureEssenceService.consume(player, fromPool);

        long owed = cost - fromPool;
        if (owed <= 0L) return true;
        long drawn = draw(player, (int) Math.min(Integer.MAX_VALUE, (owed + each - 1) / each)) * each;
        if (drawn > owed) ApertureEssenceService.add(player, drawn - owed);
        return true;
    }
    //endregion
}
