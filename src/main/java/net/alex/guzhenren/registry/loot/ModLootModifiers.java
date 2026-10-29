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
 * The global loot modifier codecs.
 *
 * <p>DeferredRegister holder: owns {@code remove_item}, the codec behind {@link RemoveItemLootModifier}, wired to the
 * mod event bus by {@code register}. The modifiers themselves are data, written by {@code ModLootModifierProvider}.
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
