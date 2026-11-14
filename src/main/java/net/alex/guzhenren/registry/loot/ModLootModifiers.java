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
 * 全局战利品修改器的编解码器，仅 {@link RemoveItemLootModifier} 的 {@code remove_item}；修改器是数据，由
 * {@code ModLootModifierProvider} 生成。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
