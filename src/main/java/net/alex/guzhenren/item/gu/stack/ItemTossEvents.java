package net.alex.guzhenren.item.gu.stack;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.TossedItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

/**
 * 阻止本命蛊从容器界面被丢弃的事件订阅类（快捷栏按 Q 键丢弃由 {@code MortalGuItem#onDroppedByPlayer} 拦截）。
 * <ul>
 *   <li>仅拦截 {@link VitalGuState#isVital} 为真的物品实体，通过 {@link TossedItems#stow} 将其塞回玩家背包或光标，确保
 *       本命蛊不会因界面外抛而遗失。</li>
 *   <li>⚠ {@link ItemTossEvent} 触发时物品已从原槽位移除，必须在成功塞回玩家身上后才能调用 {@code setCanceled(true)}，
 *       若仅取消事件而不归还物品会导致本命蛊直接蒸发；当玩家背包与光标全满无法塞回时，将掉落物设为永不自然消失
 *       （{@link ItemEntity#setUnlimitedLifetime}），兼顾防崩服与防丢蛊。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ItemTossEvents {

    private ItemTossEvents() {}

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemEntity tossed = event.getEntity();
        if (!VitalGuState.isVital(tossed.getItem())) return;

        if (TossedItems.stow(event.getPlayer(), tossed)) {
            event.setCanceled(true);
        } else {
            tossed.setUnlimitedLifetime();
        }
    }
}
