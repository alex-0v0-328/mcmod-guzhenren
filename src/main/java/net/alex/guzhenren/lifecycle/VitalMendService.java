package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.item.gu.tending.GuHealth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 本命蛊[Vital Gu]借主人回血恢复伤势：实际回上的生命（满血溢出不算）累计到首只受伤的当前本命蛊（本命槽按空窍序，再
 * 随身背包），满一次最大生命升一档，主人付 {@link #MEND_COST_FRACTION} 最大生命（至少留 1 点）。
 * <ul>
 *   <li>⚠ 由 {@code LivingHealEvent} 最低优先级转发，读最终回血量；扣血先于回血落地。</li>
 * </ul>
 */

public final class VitalMendService {

    public static final float MEND_COST_FRACTION = 0.2F;

    private VitalMendService() {}

    public static void onHealed(@NotNull ServerPlayer player, float amount) {
        float full = player.getMaxHealth();
        float healed = Math.min(amount, full - player.getHealth());
        if (healed <= 0.0F || mendStored(player, healed, full)) return;

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isWoundedVital(stack, player)) {
                mend(player, stack, healed, full);
                return;
            }
        }
    }

    private static boolean mendStored(ServerPlayer player, float healed, float full) {
        for (int aperture = 0; aperture < ApertureData.MAX_APERTURES; aperture++) {
            ItemStack vital = ApertureStorageService.getVital(player, aperture);
            if (!isWoundedVital(vital, player)) continue;

            ItemStack stack = vital.copy();
            mend(player, stack, healed, full);
            ApertureStorageService.setVital(player, aperture, stack);
            return true;
        }
        return false;
    }

    private static void mend(ServerPlayer player, ItemStack stack, float healed, float full) {
        if (!GuHealth.mend(player, stack, healed, full)) return;

        player.setHealth(Math.max(1.0F, player.getHealth() - full * MEND_COST_FRACTION));
    }

    private static boolean isWoundedVital(ItemStack stack, ServerPlayer player) {
        return GuHealth.get(stack) != null && VitalGuState.isCurrentVital(stack, player);
    }
}
