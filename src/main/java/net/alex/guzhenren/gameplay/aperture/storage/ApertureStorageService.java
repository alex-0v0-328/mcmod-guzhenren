package net.alex.guzhenren.gameplay.aperture.storage;

import java.util.List;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 空窍存蛊与本命蛊的唯一运行时写入服务。
 * <ul>
 *   <li>本命蛊放入槽位（{@link #setVital}）或炼蛊升转继承绑定（{@link #adoptVital}）时，将其令牌记为该空窍种类的唯一现
 *       任令牌；放入本命槽时同步调用 {@link ApertureService#setPrimaryPath} 设定该窍主修流派（空窍附件同步客户端，而存
 *       储附件不同步）。</li>
 *   <li>通过 {@link StoredGu} 桥接接口读取蛊虫属性，使 {@code gameplay} 包不依赖 {@code item} 包；唯一可变静态字段
 *       {@code storedGu} 在模组构造期由入口类经 {@link #setStoredGu} 注入一次。</li>
 *   <li>⚠ 存蛊写入直接更新 {@link PlayerData#setApertureStorage}，绝不经过会发布 {@code ApertureChangedEvent} 及刷新最
 *       大生命值的 {@link ApertureService}；但 {@link #setVital} 仍须调 {@link ApertureService#setPrimaryPath}（流派未
 *       变时内部直接返回），否则更换同流派或新流派本命蛊会漏设主修。</li>
 *   <li>{@link #shiftForFirstAperture} 响应 {@code FirstApertureInsertedEvent}（由 {@link ApertureStorageEvents} 转
 *       发），与 {@link ApertureData#insertFirst} 同步后移各空窍存储下标。</li>
 *   <li>{@link #load(Player, int)} 汇总该窍普通存蛊与本命蛊的负载并与 {@link #MAX_LOAD} 比较；{@code commit} 仅在写入后
 *       负载同时超过 {@link #MAX_LOAD} 且高于写入前负载时才拒绝写入，确保因修为跌落而超载的空窍仍能取出物品。</li>
 * </ul>
 */

public final class ApertureStorageService {

    private static StoredGu storedGu;

    public static void setStoredGu(@NotNull StoredGu gu) { storedGu = gu; }

    private ApertureStorageService() {}

    public static final int MAX_LOAD = 256;

    public static @NotNull ApertureStorage get(@NotNull Player player) {
        return PlayerData.get(player).getApertureStorage();
    }

    public static @NotNull List<ItemStack> getItems(@NotNull Player player, int aperture) {
        return get(player).get(aperture);
    }

    public static @NotNull List<ItemStack> getView(@NotNull Player player, int aperture) {
        return get(player).getView(aperture);
    }

    public static @NotNull List<ItemStack> getPage(@NotNull Player player, int aperture, int from, int size) {
        return get(player).getPage(aperture, from, size);
    }

    public static boolean isSamePage(@NotNull Player player, int aperture, int from, @NotNull List<ItemStack> page) {
        return get(player).isSamePage(aperture, from, page);
    }

    public static int count(@NotNull Player player, int aperture) { return get(player).count(aperture); }

    public static @NotNull ItemStack getVital(@NotNull Player player, int aperture) {
        return get(player).getVital(aperture);
    }

    public static int load(@NotNull Player player, int aperture) { return load(player, get(player), aperture); }

    public static boolean canStore(@NotNull ItemStack stack) { return storedGu.getRank(stack) != null; }

    public static int getMaxStackSize(@NotNull Player player, int aperture, int currentLoad,
            @NotNull ItemStack current, @NotNull ItemStack incoming) {
        Rank incomingRank = storedGu.getRank(incoming);
        if (incomingRank == null) return 0;

        Rank holder = ApertureService.getAperture(player, aperture).rank();
        int limit = Math.max(MAX_LOAD, currentLoad);
        int existingCount = 0;
        if (!current.isEmpty()) {
            if (ItemStack.isSameItemSameComponents(current, incoming)) existingCount = current.getCount();
            else currentLoad -= getCost(holder, current);
        }
        int freeLoad = Math.max(0, limit - currentLoad);
        return Math.min(incoming.getMaxStackSize(), existingCount + freeLoad / getCostPerItem(holder, incomingRank));
    }

    public static void set(@NotNull ServerPlayer player, int aperture, @NotNull List<ItemStack> items) {
        commit(player, aperture, get(player).with(aperture, items));
    }

    public static void setSlot(@NotNull ServerPlayer player, int aperture, int index, @NotNull ItemStack stack) {
        commit(player, aperture, get(player).withSlot(aperture, index, stack));
    }

    public static boolean setVital(@NotNull ServerPlayer player, int aperture, @NotNull ItemStack stack) {
        if (!commit(player, aperture, get(player).withVital(aperture, stack))) return false;

        adoptVital(player, aperture, stack);
        GuPath path = storedGu.getPath(stack);
        if (path != null) ApertureService.setPrimaryPath(player, aperture, path);
        return true;
    }

    public static long getVitalToken(@NotNull Player player, int kind) { return get(player).getToken(kind); }

    public static void adoptVital(@NotNull ServerPlayer player, int aperture, @NotNull ItemStack stack) {
        long token = storedGu.getToken(stack);
        int kind = ApertureService.get(player).getKindAt(aperture);
        if (token != 0L && storedGu.isVitalOf(stack, player) && token != getVitalToken(player, kind)) {
            PlayerData.get(player).setApertureStorage(get(player).withToken(kind, token));
        }
    }

    public static boolean setPage(@NotNull ServerPlayer player, int aperture, int from, @NotNull List<ItemStack> page) {
        return commit(player, aperture, get(player).withPage(aperture, from, page));
    }

    private static boolean commit(ServerPlayer player, int aperture, ApertureStorage next) {
        int after = load(player, next, aperture);
        if (after > MAX_LOAD && after > load(player, get(player), aperture)) return false;

        PlayerData.get(player).setApertureStorage(next);
        return true;
    }

    private static int load(Player player, ApertureStorage storage, int aperture) {
        Rank holder = ApertureService.getAperture(player, aperture).rank();
        int total = 0;
        if (aperture >= 0 && aperture < storage.byAperture().size()) {
            total += load(holder, storage.byAperture().get(aperture));
        }
        if (aperture >= 0 && aperture < storage.vital().size()) {
            total += getCost(holder, storage.vital().get(aperture));
        }
        return total;
    }

    public static void shiftForFirstAperture(@NotNull ServerPlayer player) {
        PlayerData.get(player).setApertureStorage(get(player).shiftRight(storedGu));
    }

    private static int load(Rank holder, List<ItemStack> stacks) {
        int total = 0;
        for (ItemStack stack : stacks) total += getCost(holder, stack);
        return total;
    }

    private static int getCost(Rank holder, ItemStack stack) {
        Rank rank = stack.isEmpty() ? null : storedGu.getRank(stack);
        if (rank == null) return 0;

        return getCostPerItem(holder, rank) * stack.getCount();
    }

    private static int getCostPerItem(Rank holder, Rank gu) {
        int gap = gu.ordinal() - holder.ordinal();
        return gap < 0 ? 1 : gap == 0 ? 2 : 2 << gap;
    }
}
