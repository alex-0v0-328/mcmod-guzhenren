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
 * 空窍内存储蛊虫与各窍本命蛊的每秒养护结算入口（由心跳传入距上次结算经过的天数，经 {@link ApertureStorageService} 读写
 * 并转交 {@link GuUpkeep#tickInContainer} 处理）。
 * <ul>
 *   <li>绝大多数秒数传入的天数为 0，但已炼化蛊虫仍会在容器内执行自身养护逻辑，饿死时统一通过
 *       {@link TendedGuItem#starve} 发出提示。</li>
 *   <li>存储列表遍历采用逐只实时读取并在发生变更后立即写回单个槽位的策略：部分蛊虫养护会直接读写同一存储（如
 *       天元宝莲[Heavenly Essence Treasure Lotus Gu]向存储中的元老蛊[Primeval Elder Gu]注入元石），若在遍历结束后整体写
 *       回会覆盖这些即时改动；本命槽仅在状态实际变化或饿死时写回。</li>
 *   <li>⚠ 遍历存储蛊时必须先校验 {@code isRefinedTended}：未炼化蛊的饱食值初始为 0，与饿死状态相同，若漏过此检查会在首
 *       次日切时误判饿死所有野生蛊。</li>
 * </ul>
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
