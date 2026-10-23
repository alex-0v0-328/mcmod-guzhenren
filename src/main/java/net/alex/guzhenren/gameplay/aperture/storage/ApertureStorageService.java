package net.alex.guzhenren.gameplay.aperture.storage;

import java.util.List;
import net.alex.guzhenren.core.ItemStacks;
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
 * The only runtime writer of what an Aperture [空窍] holds, including the Vital Gu [本命蛊] bound to each.
 * {@code setVital} also rewrites the aperture's primary path via {@link ApertureService#setPrimaryPath}
 * -- binding a Gu IS what sets primary path [主修] (the store is not synced; the aperture is).
 *
 * <p>Knows the Gu it holds only through the {@link StoredGu} the entry point sets ({@link #setStoredGu}), so the
 * aperture does not depend on the item package; binding a Vital Gu reads that Gu's declared path from it.
 * ⚠ Writes NEVER go through {@code ApertureService.store}:
 * the {@code ApertureChangedEvent} and its max-health refresh hang off that. ⚠
 * {@code setPrimaryPath} no-ops when unchanged; keep the call, or a rebind loses the path. A Vital Gu
 * becomes its aperture's one current Vital Gu when it lands in the slot ({@code setVital}) or a refinement
 * passes the binding on ({@link #adoptVital}): its token replaces the kind's token in the store.
 *
 * <p>A write is refused only when it ends above both {@code MAX_LOAD} and the load it started from, so an
 * over-full store (a rank that fell) can still be emptied; {@code exceedsLoad} counts the starting load only
 * when the result is over the cap. {@link #setSlot} writes one stored Gu back, the per-second settle's write.
 *
 * <p>{@link #shiftForFirstAperture} is the storage-side twin of
 * {@link ApertureData#insertFirst}: when Hope Gu opens the first aperture ahead of a lone second one, the
 * aperture posts a {@code FirstApertureInsertedEvent} and {@link ApertureStorageEvents} slides every stored list
 * and the Vital Gu slot up one position.
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
            if (ItemStacks.sameItemSameData(current, incoming)) existingCount = current.getCount();
            else currentLoad -= getCost(holder, current);
        }
        int freeLoad = Math.max(0, limit - currentLoad);
        return Math.min(incoming.getMaxStackSize(), existingCount + freeLoad / getCostPerItem(holder, incomingRank));
    }

    public static void set(@NotNull ServerPlayer player, int aperture, @NotNull List<ItemStack> items) {
        ApertureStorage current = get(player);
        ApertureStorage next = current.with(aperture, items);
        if (isOverLoad(player, current, next, aperture)) return;

        PlayerData.get(player).setApertureStorage(next);
    }

    public static void setSlot(@NotNull ServerPlayer player, int aperture, int index, @NotNull ItemStack stack) {
        ApertureStorage current = get(player);
        ApertureStorage next = current.withSlot(aperture, index, stack);
        if (isOverLoad(player, current, next, aperture)) return;

        PlayerData.get(player).setApertureStorage(next);
    }

    public static boolean setVital(@NotNull ServerPlayer player, int aperture, @NotNull ItemStack stack) {
        ApertureStorage current = get(player);
        ApertureStorage next = current.withVital(aperture, stack);
        if (isOverLoad(player, current, next, aperture)) return false;

        PlayerData.get(player).setApertureStorage(next);
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
        ApertureStorage current = get(player);
        ApertureStorage next = current.withPage(aperture, from, page);
        if (isOverLoad(player, current, next, aperture)) return false;

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
        PlayerData.get(player).setApertureStorage(get(player).shiftRight());
    }

    private static int load(Rank holder, List<ItemStack> stacks) {
        int total = 0;
        for (ItemStack stack : stacks) total += getCost(holder, stack);
        return total;
    }

    private static boolean isOverLoad(Player player, ApertureStorage current, ApertureStorage next, int aperture) {
        int after = load(player, next, aperture);
        return after > MAX_LOAD && after > load(player, current, aperture);
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
