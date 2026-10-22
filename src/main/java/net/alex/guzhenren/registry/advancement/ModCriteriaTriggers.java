package net.alex.guzhenren.registry.advancement;

import java.util.function.Supplier;
import net.alex.guzhenren.advancement.HopeGuUsedTrigger;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Every custom criterion trigger [进度触发器], registered with the vanilla {@code CriteriaTriggers}. The
 * registration id mirrors the trigger name ({@code used_hope_gu}); advancement JSON dispatches conditions by it.
 *
 * <p>⚠ Loader glue: Forge 1.20.1 has no trigger registry, only vanilla's plain map, which is not safe to write
 * from the parallel mod construction; {@link #register} defers the write to the common setup on the main thread.
 *
 * @author Alex
 * @version 1.0.0
 * @see HopeGuUsedTrigger
 * @since 1.0.0
 */

public final class ModCriteriaTriggers {

    private ModCriteriaTriggers() {}

    private static final HopeGuUsedTrigger HOPE_GU_USED = new HopeGuUsedTrigger();
    public static final Supplier<HopeGuUsedTrigger> USED_HOPE_GU = () -> HOPE_GU_USED;

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> CriteriaTriggers.register(HOPE_GU_USED)));
    }
}
