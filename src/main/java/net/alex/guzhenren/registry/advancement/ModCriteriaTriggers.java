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
 * 自定义原版进度触发器注册表（包含 {@link HopeGuUsedTrigger} 与 {@link RankReachedTrigger}），向
 * {@code BuiltInRegistries.TRIGGER_TYPES} 注册模组条件触发器。
 * <ul>
 *   <li>⚠ 必须统一经由 {@link #TRIGGERS} 注册：直接调用原版 {@code CriteriaTriggers.register(String, ...)} 会将裸路径名
 *       归入 {@code minecraft:} 命名空间，而进度 JSON 依赖本模组命名空间下的注册 ID 分派条件。</li>
 * </ul>
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
