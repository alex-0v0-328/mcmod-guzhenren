package net.alex.guzhenren.registry.loot;

import com.mojang.serialization.MapCodec;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.RemoveItemLootModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 全局战利品修改器编解码器注册表，仅注册 {@link RemoveItemLootModifier} 的 {@code remove_item} 序列化器（具体数据包修改
 * 器实例由 {@code ModLootModifierProvider} 生成）。
 */

public final class ModLootModifiers {

    private ModLootModifiers() {}

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Guzhenren.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<RemoveItemLootModifier>>
            REMOVE_ITEM = LOOT_MODIFIERS.register("remove_item", () -> RemoveItemLootModifier.CODEC);

    public static void register(IEventBus modEventBus) {
        LOOT_MODIFIERS.register(modEventBus);
    }
}
