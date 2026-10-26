package net.alex.guzhenren.registry.advancement;

import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.advancement.HopeGuUsedTrigger;
import net.alex.guzhenren.advancement.RankReachedTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Every custom criterion trigger [进度触发器], registered into the vanilla {@code TRIGGER_TYPES}
 * registry. The registration id mirrors the trigger name ({@code used_hope_gu}); advancement JSON
 * dispatches conditions by it.
 *
 * <p>☠ A trigger registers through {@link #TRIGGERS} only: vanilla's public
 * {@code CriteriaTriggers.register(String, ...)} parses a bare name into the {@code minecraft:} namespace.
 *
 * @author Alex
 * @version 1.0.0
 * @see HopeGuUsedTrigger
 * @see RankReachedTrigger
 * @since 1.0.0
 */

public final class ModCriteriaTriggers {

    private ModCriteriaTriggers() {}

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(BuiltInRegistries.TRIGGER_TYPES.key(), Guzhenren.MOD_ID);
    public static final Supplier<HopeGuUsedTrigger> USED_HOPE_GU = TRIGGERS.register("used_hope_gu",
            HopeGuUsedTrigger::new);
    public static final Supplier<RankReachedTrigger> REACHED_RANK = TRIGGERS.register("reached_rank",
            RankReachedTrigger::new);

    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }
}
