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
 * 自定义进度触发器，注册进原版 {@code TRIGGER_TYPES}。
 *
 * <ul>
 *   <li>☠ 只经 {@link #TRIGGERS} 注册：{@code CriteriaTriggers.register(String, ...)} 把裸名归入 {@code minecraft:}；
 *       注册 ID 即触发器名（{@code used_hope_gu}），进度 JSON 按它分派条件。</li>
 * </ul>
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
