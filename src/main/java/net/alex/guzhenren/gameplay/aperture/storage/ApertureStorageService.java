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
 * 空窍[Aperture]所存蛊（含本命蛊）的唯一运行时写入方。
 * <ul>
 *   <li>本命蛊落槽（{@code setVital}）或炼蛊传绑定（{@link #adoptVital}）即成唯一现任，令牌顶替该种类旧令牌；落槽经
 *       {@link ApertureService#setPrimaryPath} 设主修（空窍同步，存储不同步）。</li>
 *   <li>只凭 {@link StoredGu} 识蛊，空窍不依赖物品包，绑本命蛊时经它读道[Path]；唯一可变静态字段 {@code storedGu}
 *       是接缝，入口类于 mod 构造时经 {@link #setStoredGu} 设一次，此后不换。</li>
 *   <li>⚠ 写入绝不经 {@code ApertureService.store}：那里挂着 {@code ApertureChangedEvent} 及最大生命刷新。</li>
 *   <li>⚠ {@code setPrimaryPath} 值不变即不做事；仍须调用，否则重绑丢主修。</li>
 *   <li>{@link #shiftForFirstAperture} 即 {@link ApertureData#insertFirst} 的存储侧：希望蛊[Hope Gu]插第一空窍发
 *       {@code FirstApertureInsertedEvent}，{@link ApertureStorageEvents} 据此调用。</li>
 *   <li>{@link #load(Player, int)} 合计所存蛊与本命蛊，对照 {@code MAX_LOAD}；拒写规则让过满（转数跌落）仍可清空，
 *       {@code isOverLoad} 超上限才算写前负载。</li>
 *   <li>{@link #setSlot} 写回一只所存蛊，供每秒结算。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureService
 * @see net.alex.guzhenren.vault.ApertureStorageTick
 * @since 1.0.0
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

    /** 写入 {@code next}，除非 {@code aperture} 负载因此同时超上限与当前负载。 */
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
