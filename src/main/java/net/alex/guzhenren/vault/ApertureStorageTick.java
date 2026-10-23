package net.alex.guzhenren.vault;

import net.alex.guzhenren.core.ItemStacks;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.item.gu.GuUpkeep;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The per-second settle of Gu held inside apertures, both the stored ones and each Vital Gu [本命蛊]. Called from
 * every heartbeat with the day count elapsed since the last one -- zero on most seconds, when a refined Gu
 * still pays its own upkeep; forwards to {@link GuUpkeep#tickInContainer} per refined Gu and reports starvation
 * through {@link TendedGuItem#starve}. Each stored Gu is read from the live store
 * ({@code ApertureStorageService.getView}) and its slot written back on its own right after its tick
 * ({@code ApertureStorageService.setSlot}): a Gu's upkeep may write the store itself (the Treasure Lotus mints
 * stones into a stored Primeval Elder Gu, which may itself have just paid its upkeep), and a walk that wrote one
 * snapshot back at the end would erase that.
 *
 * <p>⚠ Every reader asks {@code isRefinedTended} first: an unrefined Gu's hunger is zero, and zero is also
 * what starvation looks like -- drop the test and the first rollover eats every wild Gu. Like the store's
 * list, the Vital slot is written back ({@code setVital}) only when the walk changed it.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureStorageService
 * @see GuUpkeep
 * @since 1.0.0
 */

public final class ApertureStorageTick {

    private ApertureStorageTick() {}

    public static void tickStored(@NotNull ServerPlayer player, long days) {
        for (int aperture = 0; aperture < ApertureData.MAX_APERTURES; aperture++) {
            tickStore(player, aperture, days);
            tickVital(player, aperture, days);
        }
    }

    private static void tickStore(ServerPlayer player, int aperture, long days) {
        for (int i = 0; i < ApertureStorageService.count(player, aperture); i++) {
            ItemStack original = ApertureStorageService.getView(player, aperture).get(i);
            if (!isRefinedTended(original)) continue;

            ItemStack stack = original.copy();
            boolean starved = GuUpkeep.tickInContainer(player, stack, days);
            if (starved) TendedGuItem.starve(player, stack);
            if (!starved && !hasChanged(original, stack)) continue;

            ApertureStorageService.setSlot(player, aperture, i, starved ? ItemStack.EMPTY : stack);
        }
    }

    private static void tickVital(ServerPlayer player, int aperture, long days) {
        ItemStack stack = ApertureStorageService.getVital(player, aperture);
        if (!(stack.getItem() instanceof TendedGuItem)) return;

        ItemStack before = stack.copy();
        if (GuUpkeep.tickInContainer(player, stack, days)) {
            ApertureStorageService.setVital(player, aperture, ItemStack.EMPTY);
            TendedGuItem.starve(player, stack);
            return;
        }
        if (hasChanged(before, stack)) ApertureStorageService.setVital(player, aperture, stack);
    }

    private static boolean isRefinedTended(ItemStack stack) {
        return stack.getItem() instanceof TendedGuItem gu && gu.isRefined(stack);
    }

    private static boolean hasChanged(ItemStack before, ItemStack after) {
        return before.getCount() != after.getCount()
                || !ItemStacks.sameItemSameData(before, after);
    }
}
