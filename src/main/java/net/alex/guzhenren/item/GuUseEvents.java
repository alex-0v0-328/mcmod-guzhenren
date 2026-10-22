package net.alex.guzhenren.item;

import net.alex.guzhenren.Guzhenren;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sets the charge duration of a Gu item when its use starts, on both sides.
 *
 * <p>⚠ Loader glue: Forge 1.20.1 asks an item for its use duration without the user, while a Gu's charge is
 * paced by its holder's rank ({@link GuItem#chargeDuration}). The start event is where the user is known, and
 * the client runs it too, so both sides count down the same ticks.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuItem
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class GuUseEvents {

    private GuUseEvents() {}

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getItem().getItem() instanceof GuItem gu) {
            event.setDuration(gu.chargeDuration(event.getItem(), event.getEntity()));
        }
    }
}
