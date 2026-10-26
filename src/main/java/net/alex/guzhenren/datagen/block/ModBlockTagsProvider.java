package net.alex.guzhenren.datagen.block;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.block.ModBlockTags;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Writes the block tags. The Decorative Hopper [装饰漏斗] copies the hopper's tool requirement, so it joins
 * {@code mineable/pickaxe} to break at hopper speed. The As Before terrain tag holds the ground itself -- stone,
 * soil, sand, terracotta, sandstone -- and leaves out moss, ice, snow layers and fluids, which the As Before Gu
 * [如故蛊] do not put back.
 */

public class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.DECORATIVE_HOPPER.get());
        tag(ModBlockTags.AS_BEFORE_TERRAIN)
                .addTags(BlockTags.BASE_STONE_OVERWORLD, BlockTags.BASE_STONE_NETHER, BlockTags.SAND,
                        BlockTags.TERRACOTTA, Tags.Blocks.SANDSTONE_UNCOLORED_BLOCKS, Tags.Blocks.SANDSTONE_RED_BLOCKS)
                .add(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.MYCELIUM,
                        Blocks.ROOTED_DIRT, Blocks.MUD, Blocks.GRAVEL, Blocks.CLAY, Blocks.CALCITE,
                        Blocks.DRIPSTONE_BLOCK);
        tag(ModBlockTags.AS_BEFORE_STRIPPED)
                .addTags(Tags.Blocks.ORES, Tags.Blocks.STORAGE_BLOCKS_RAW_IRON, Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER,
                        Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD);
    }
}
