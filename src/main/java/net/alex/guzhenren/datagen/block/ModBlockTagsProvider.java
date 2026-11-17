package net.alex.guzhenren.datagen.block;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.block.ModBlockTags;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 方块标签。
 *
 * <ul>
 *   <li>如故蛊[As Before Gu]地形标签只收地面本身（石、土、沙、陶瓦、砂岩），不收它不复原的苔藓、冰、雪层、流体。</li>
 *   <li>知心草作物入保田标签，否则它脚下的耕地干了会退成泥土；入原版作物标签，蜜蜂授粉、剑挖加速和其他模组借此认它是
 *       作物，黑土耕地也按此标签补随机刻。</li>
 *   <li>知心草作物入农夫乐事的沃土排除标签 {@link #UNAFFECTED_BY_RICH_SOIL}：沃土与沃土耕地不给它施骨粉；没装农夫乐事
 *       时这个标签无人读。</li>
 *   <li>黑土耕地入原版耕地所在的三个标签（锹挖、大型垂滴叶可放、村民耕地）。</li>
 * </ul>
 */

public class ModBlockTagsProvider extends BlockTagsProvider {

    // The loader's convention tag other mods' steel blocks join; only datagen names it, so the key lives here.
    private static final TagKey<Block> STORAGE_BLOCKS_STEEL =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/steel"));
    private static final TagKey<Block> UNAFFECTED_BY_RICH_SOIL = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "unaffected_by_rich_soil"));

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.STEEL_BLOCK.get());
        tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.STEEL_BLOCK.get());
        tag(BlockTags.BEACON_BASE_BLOCKS).add(ModBlocks.STEEL_BLOCK.get());
        tag(STORAGE_BLOCKS_STEEL).add(ModBlocks.STEEL_BLOCK.get());
        tag(Tags.Blocks.STORAGE_BLOCKS).addTag(STORAGE_BLOCKS_STEEL);
        tag(BlockTags.CROPS).add(ModBlocks.INTIMATE_GRASS_CROP.get());
        tag(BlockTags.MAINTAINS_FARMLAND).add(ModBlocks.INTIMATE_GRASS_CROP.get());
        tag(UNAFFECTED_BY_RICH_SOIL).add(ModBlocks.INTIMATE_GRASS_CROP.get());
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.BLACK_SOIL_FARMLAND.get());
        tag(BlockTags.BIG_DRIPLEAF_PLACEABLE).add(ModBlocks.BLACK_SOIL_FARMLAND.get());
        tag(Tags.Blocks.VILLAGER_FARMLANDS).add(ModBlocks.BLACK_SOIL_FARMLAND.get());
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
