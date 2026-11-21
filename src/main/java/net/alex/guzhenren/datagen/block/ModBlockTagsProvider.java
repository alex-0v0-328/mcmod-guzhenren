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
 * 本模组方块标签数据生成器。
 * <ul>
 *   <li>如故蛊[As Before Gu]地形恢复标签（{@link ModBlockTags#AS_BEFORE_TERRAIN}）仅收录天然地基方块（岩石、泥土、沙
 *       子、陶瓦、砂岩等），排除苔藓、冰、雪层与流体；剥离标签（{@link ModBlockTags#AS_BEFORE_STRIPPED}）收录矿石与粗金
 *       属块。</li>
 *   <li>荡魂山石进 {@link BlockTags#SNOW_LAYER_CANNOT_SURVIVE_ON}：冻结地物与降雪都先问雪层能否存活，山上因此不积雪；跟
 *       方块走而非跟生成走，山石被移到别处也不积雪。</li>
 *   <li>知心草作物加入 {@link BlockTags#MAINTAINS_FARMLAND}（防止下方干耕地退化为泥土）与 {@link BlockTags#CROPS}（使蜜
 *       蜂授粉、剑挖掘加速及黑土耕地随机刻加速生效），并加入农夫乐事的沃土排除标签 {@link #UNAFFECTED_BY_RICH_SOIL} 以
 *       禁止沃土对其触发骨粉催熟（未安装农夫乐事时该标签静默闲置）。</li>
 *   <li>黑土耕地加入原版耕地所属的锹挖掘、大型垂滴叶可放置与村民耕地三个标签；钢块加入镐挖掘、石质工具门槛、信标基座及
 *       通用钢块存储标签 {@link #STORAGE_BLOCKS_STEEL}（{@code c:storage_blocks/steel}）。</li>
 *   <li>荡魂山石同基岩加入凋灵免疫、末影龙免疫与地物不可替换三个标签，否则凋灵会啃穿山体、树等地物会覆盖山表；胆石加入
 *       镐挖掘（只加速，不要求工具）。两者都进 {@link ModBlockTags#MORTAL_AS_BEFORE_IMMUNE}：荡魂山只有仙蛊能还原，凡蛊
 *       山如故、江如故跳过它们。</li>
 * </ul>
 */

public class ModBlockTagsProvider extends BlockTagsProvider {

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
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.STEEL_BLOCK.get(), ModBlocks.GUTS_STONE.get());
        tag(BlockTags.WITHER_IMMUNE).add(ModBlocks.SOULQUAKE_STONE.get());
        tag(BlockTags.DRAGON_IMMUNE).add(ModBlocks.SOULQUAKE_STONE.get());
        tag(BlockTags.FEATURES_CANNOT_REPLACE).add(ModBlocks.SOULQUAKE_STONE.get());
        tag(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON).add(ModBlocks.SOULQUAKE_STONE.get());
        tag(ModBlockTags.MORTAL_AS_BEFORE_IMMUNE).add(ModBlocks.SOULQUAKE_STONE.get(), ModBlocks.GUTS_STONE.get());
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
