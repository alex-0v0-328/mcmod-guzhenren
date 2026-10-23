package net.alex.guzhenren.registry.loot;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.loot.AddTableLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The global loot modifier types this mod registers.
 *
 * <p>⚠ Port-only: the NeoForge 1.21.1 mainline registers none and uses NeoForge's built-in {@code add_table}.
 *
 * @author Alex
 * @version 1.0.0
 * @see AddTableLootModifier
 * @since 1.0.0
 */

public final class ModLootModifiers {

    private ModLootModifiers() {}

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Guzhenren.MOD_ID);
    public static final RegistryObject<Codec<AddTableLootModifier>> ADD_TABLE =
            SERIALIZERS.register("add_table", () -> AddTableLootModifier.CODEC);

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
