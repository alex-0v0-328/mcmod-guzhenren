package net.alex.guzhenren.registry.block;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * The block tags this mod declares.
 *
 * <p>Tag-key holder (not a DeferredRegister). {@link #AS_BEFORE_TERRAIN} and {@link #AS_BEFORE_STRIPPED} decide what
 * the Mountain and River As Before Gu [山如故 / 江如故] put back: a decoration's change survives only where it swaps
 * one terrain block for another (a cliff's rock bands, a canyon's sandstone), and a stripped block (an ore, a raw
 * ore block) comes back as the stone around it. A modpack widens either tag through a datapack.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModBlockTags {

    private ModBlockTags() {}

    public static final TagKey<Block> AS_BEFORE_TERRAIN = key("as_before_terrain");
    public static final TagKey<Block> AS_BEFORE_STRIPPED = key("as_before_stripped");
    public static final TagKey<Block> STORAGE_BLOCKS_STEEL = commonKey("storage_blocks/steel");

    private static TagKey<Block> key(String name) {
        return TagKey.create(Registries.BLOCK, Guzhenren.id(name));
    }

    private static TagKey<Block> commonKey(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", name));
    }
}
