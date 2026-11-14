package net.alex.guzhenren.item.gu.stack;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.TossedItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

/**
 * 「本命蛊不可丢」的物品栏一半；Q 键丢弃归物品的 {@code onDroppedByPlayer}。
 * <ul>
 *   <li>只拒 {@link VitalGuState#isVital} 的物品，经 {@link TossedItems#stow} 归还，从不掉落。</li>
 *   <li>⚠ 须还回：{@link net.neoforged.neoforge.event.entity.item.ItemTossEvent} 发布前已移实体，仅取消会删物品。</li>
 *   <li>放不下的本命蛊悬停原处、永不消失：背包满既不崩服也不丢蛊。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.GuItem
 * @since 1.0.0
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
