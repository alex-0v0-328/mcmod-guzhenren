package net.alex.guzhenren.entity.beast;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.path.strength.BeastPhantomsDrawnEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Stands the phantoms [兽力虚影] of an attack round: {@link BeastPhantomsDrawnEvent} is answered with
 * {@link BeastPhantomEntity#show} in the same tick, since the event dispatches synchronously. The round lives
 * below the entity and posts instead of calling up.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastPhantomEntity
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BeastPhantomShowEvents {

    private BeastPhantomShowEvents() {}

    @SubscribeEvent
    public static void onPhantomsDrawn(BeastPhantomsDrawnEvent event) {
        BeastPhantomEntity.show(event.getPlayer(), event.getFamilies());
    }
}
