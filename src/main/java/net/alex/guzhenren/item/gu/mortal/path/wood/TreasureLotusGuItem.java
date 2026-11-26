package net.alex.guzhenren.item.gu.mortal.path.wood;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 天元宝莲[Heavenly Essence Treasure Lotus Gu]物品：被动产出元石[Primeval Stone]并回复真元的持续照顾型蛊[Tended Gu]。
 * <ul>
 *   <li>无饱食时钟：无需喂食、也不会饿死；野生状态下仍可长按炼化，炼化后右键催动固定返回
 *       {@code FAIL}（{@link #keepsRefusedClick} 为 {@code false}，不挥手且放行副手）。</li>
 *   <li>在每秒一次的 {@link #payOwnUpkeep} 被动心跳中，为持有者回复 5% 最大真元并产出 {@code stonesPerSecond} 块元石：
 *       依次尝试存入随身及空窍存储中的元老蛊[Primeval Elder Gu]、主背包、快捷栏与副手，仍放不下时向地面掉落；受伤照常产
 *       出，不修伤。</li>
 * </ul>
 */

public class TreasureLotusGuItem extends TendedGuItem {

    private static final int ESSENCE_REGEN_PERCENT = 5;
    private final int stonesPerSecond;

    public TreasureLotusGuItem(int stonesPerSecond, GuProperties guProperties) {
        super(guProperties);
        this.stonesPerSecond = stonesPerSecond;
    }

    @Override
    protected boolean keepsRefusedClick() { return false; }

    @Override
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) {
        return new Refusal(Refusal.AUTO_EFFECT);
    }

    @Override
    protected void applyEffect(ServerPlayer player, ItemStack stack) {}

    //region the passive heartbeat [被动心跳]
    @Override
    protected void payOwnUpkeep(ServerPlayer player, ItemStack stack) {
        ApertureEssenceService.add(player, ApertureEssenceService.getMaxEssence(player) * ESSENCE_REGEN_PERCENT / 100);
        giveStones(player, stonesPerSecond);
    }

    private void giveStones(ServerPlayer player, int amount) {
        int left = fillElders(player, amount);
        if (left > 0) dropToInventory(player, left);
    }

    private int fillElders(ServerPlayer player, int amount) {
        int left = amount;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            left -= storeInElder(inventory.getItem(slot), left);
        }
        for (int aperture = 0; aperture < ApertureData.MAX_APERTURES && left > 0; aperture++) {
            left = fillStoredElders(player, aperture, left);
            if (left <= 0) break;

            ItemStack vital = ApertureStorageService.getVital(player, aperture);
            int moved = storeInElder(vital, left);
            if (moved > 0) {
                ApertureStorageService.setVital(player, aperture, vital);
                left -= moved;
            }
        }
        return left;
    }

    private int fillStoredElders(ServerPlayer player, int aperture, int amount) {
        if (ApertureStorageService.getView(player, aperture).stream()
                .noneMatch(stored -> stored.getItem() instanceof PrimevalElderGuItem)) return amount;
        int left = amount;
        List<ItemStack> storedItems = new ArrayList<>(ApertureStorageService.getItems(player, aperture));
        boolean storedChanged = false;
        for (ItemStack stored : storedItems) {
            if (left <= 0) break;
            int moved = storeInElder(stored, left);
            storedChanged |= moved > 0;
            left -= moved;
        }
        if (storedChanged) ApertureStorageService.set(player, aperture, storedItems);
        return left;
    }

    private int storeInElder(ItemStack stack, int amount) {
        return stack.getItem() instanceof PrimevalElderGuItem elder ? elder.storeStones(stack, amount) : 0;
    }

    private void dropToInventory(ServerPlayer player, int amount) {
        ItemStack produced = new ItemStack(ModItems.PRIMEVAL_STONE.get());
        Inventory inventory = player.getInventory();
        int left = placeInto(inventory, 9, 36, produced, amount);
        left = placeInto(inventory, 0, 9, produced, left);
        ItemStack offhand = player.getOffhandItem();
        if (left > 0 && ItemStack.isSameItemSameComponents(offhand, produced)
                && offhand.getCount() < offhand.getMaxStackSize()) {
            int moved = Math.min(left, offhand.getMaxStackSize() - offhand.getCount());
            offhand.grow(moved);
            left -= moved;
        } else if (left > 0 && offhand.isEmpty()) {
            int moved = Math.min(left, produced.getMaxStackSize());
            player.setItemInHand(InteractionHand.OFF_HAND, produced.copyWithCount(moved));
            left -= moved;
        }
        while (left > 0) {
            int moved = Math.min(left, produced.getMaxStackSize());
            player.drop(produced.copyWithCount(moved), false);
            left -= moved;
        }
    }

    private int placeInto(Inventory inventory, int from, int to, ItemStack produced, int amount) {
        int left = amount;
        for (int slot = from; slot < to && left > 0; slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (ItemStack.isSameItemSameComponents(existing, produced)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(left, existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                left -= moved;
            }
        }
        for (int slot = from; slot < to && left > 0; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                int moved = Math.min(left, produced.getMaxStackSize());
                inventory.setItem(slot, produced.copyWithCount(moved));
                left -= moved;
            }
        }
        return left;
    }
    //endregion
}
