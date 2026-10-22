package net.alex.guzhenren.item;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.TossedItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The inventory-toss half of "a Vital Gu [本命蛊] cannot be thrown away"; the Q-key drop is answered
 * on the item itself via {@code onDroppedByPlayer}.
 *
 * <p>Only {@link net.alex.guzhenren.item.GuItem#isVital} stacks are refused. This handler must
 * hand the stack back into the inventory: vanilla's {@link net.minecraftforge.event.entity.item.ItemTossEvent}
 * removes the entity before posting, so canceling alone deletes the item instead of saving it. It hands it
 * back through {@link TossedItems#stow}, which never drops; a Vital Gu that does not fit hovers where it was
 * dropped and never despawns, so a full bag can neither crash the server nor lose it.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.GuItem
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ItemTossEvents {

    private ItemTossEvents() {}

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemEntity tossed = event.getEntity();
        if (!GuItem.isVital(tossed.getItem())) return;

        if (TossedItems.stow(event.getPlayer(), tossed)) {
            event.setCanceled(true);
        } else {
            tossed.setUnlimitedLifetime();
        }
    }
}
