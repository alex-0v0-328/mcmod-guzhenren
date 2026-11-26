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
 * 随身元石[Primeval Stone]（{@link PrimevalStoneItem}）与元老蛊[Primeval Elder Gu]存量的唯一自动抽取与真元结算通道。
 * <ul>
 *   <li>⚠ 凡蛊的一切真元支出均须通过 {@link #pay} 执行：先触发自动补给线（当真元池低于上限
 *       {@link #REFILL_BELOW_PERCENT}% 时自动抽取元石补至 {@link #REFILL_UP_TO_PERCENT}%），再调用 {@link #trySpend} 优
 *       先扣除真元池余额、差额由随身元石补足（仅当池与元石合计仍不足时返回 {@code false} 且分文不扣）；由于灌注
 *       [Channel]与炼化的每步定额取决于当前真元池余额，调用方须先调 {@link #topUpFor}（在常规补给线基础上，若池容量允许
 *       但仍低于最低起付线 {@code need}，则继续抽石补至 {@code need}）再计算步长；此外日常心跳补给、温养[Nourish] 钱袋
 *       与炼蛊台供给槽均走本类，{@link #takeFrom} 是从散装元石栈或元老蛊中扣减元石的唯一底层入口。</li>
 *   <li>⚠ 抽取元石的检索顺序严格固定：「背包与副手中的散装元石 → 背包与副手中的元老蛊存量 → 各空窍存储中的元老蛊存量
 *       （跳过本命槽）」；空窍存储中的元老蛊必须在物品栈副本上扣减后再通过 {@code ApertureStorageService.setSlot} 写
 *       回，以触发存储同步。</li>
 *   <li>⚠ 客户端无法访问服务端空窍存储，因此客户端调用 {@link #canAfford} 时仅统计随身背包与副手来源（最终结果由服务端
 *       权威裁决并同步）；同时 {@link #canAfford} 仅在玩家最大真元大于 0（即拥有可用空窍）时才计入随身元石，借此以真元
 *       门槛自然拦截未开窍凡人使用需消耗真元的蛊虫。</li>
 *   <li>⚠ 创造模式下自动抽取元石恒返回 0（{@link #draw}），真元池不足的部分直接免除（唯有手动向供给槽或温养钱袋放入元
 *       石时才扣减物品）；依赖真元池余额决定步长的炼化与灌注逻辑改读 {@link #getPoolAfterTopUp}（模拟计入随身元石补给后
 *       的真元池），使创造模式持有元石时的步长节奏与生存模式保持一致。</li>
 *   <li>⚠ 杀招[Killing Move]与功倍[Amplify Effect]驱动不受真元门槛限制于创造模式：其校验与后援扣费改走
 *       {@link #canAffordOrCreative} 与 {@link #payOrCreative}——创造模式付不起时不拒绝、不扣池（{@link #pay} 付不起本就分
 *       文不扣），付得起则照常扣；其余真元支出（灌注、炼化、普通催动）仍用 {@link #canAfford} 与 {@link #pay}。</li>
 *   <li>⚠ 切勿自行复制补给线逻辑或在超池支付时先把元石全部倒入真元池（真元池会静默截断超出上限的部分）：
 *       {@link #trySpend} 对超出当前真元池余额的单笔开销会先扣空真元池，再直接燃烧刚好够补齐差额的整数块元石，并将单块
 *       元石折算后多出的零头退回真元池。</li>
 * </ul>
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

    public static void topUpFor(ServerPlayer player, long need) {
        topUp(player);
        long missing = need - ApertureEssenceService.getSpendable(player);
        if (missing > 0L && ApertureEssenceService.getMaxEssence(player) >= need) pourInto(player, missing);
    }

    public static boolean pay(ServerPlayer player, long cost) {
        if (cost <= 0L) return true;
        topUp(player);
        return trySpend(player, cost);
    }
    //endregion

    //region creative [创造模式免门槛]
    public static boolean canAffordOrCreative(Player player, long cost) {
        return player.getAbilities().instabuild || canAfford(player, cost);
    }

    public static boolean payOrCreative(ServerPlayer player, long cost) {
        return pay(player, cost) || player.getAbilities().instabuild;
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
