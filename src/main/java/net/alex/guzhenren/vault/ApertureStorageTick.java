package net.alex.guzhenren.vault;

import java.util.List;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.item.gu.tending.GuUpkeep;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 空窍[Aperture]内蛊的每秒结算，含存储蛊与各本命蛊。
 *
 * <ul>
 *   <li>心跳调用，传距上次的天数，多数秒为 0，已炼化蛊仍自付养护；逐只转 {@link GuUpkeep#tickInContainer}，
 *       饿死经 {@link TendedGuItem#starve} 报告。</li>
 *   <li>每只存储蛊从实时存储读、tick 后即单独写回：养护可能自写存储（天元宝莲[Heavenly Essence Treasure Lotus Gu]
 *       往存储的元老蛊[Primeval Elder Gu]铸元石[Primeval Stone]，该元老蛊或刚付完养护），遍历末整体写回会抹掉之。</li>
 *   <li>⚠ 先问 {@code isRefinedTended}：未炼化蛊饱食[Hunger]为 0，与饿死无异；去掉它，首次日切即饿死所有野生蛊。</li>
 *   <li>本命槽同理，仅遍历改动它时写回。</li>
 * </ul>
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
        for (int i = 0; ; i++) {
            List<ItemStack> stored = ApertureStorageService.getView(player, aperture);
            if (i >= stored.size()) return;

            ItemStack original = stored.get(i);
            if (!isRefinedTended(original)) continue;

            ItemStack stack = original.copy();
            boolean starved = GuUpkeep.tickInContainer(player, stack, days);
            if (starved) TendedGuItem.starve(player, stack);
            if (!starved && ItemStack.matches(original, stack)) continue;

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
        if (!ItemStack.matches(before, stack)) ApertureStorageService.setVital(player, aperture, stack);
    }

    private static boolean isRefinedTended(ItemStack stack) {
        return stack.getItem() instanceof TendedGuItem gu && gu.isRefined(stack);
    }
}
