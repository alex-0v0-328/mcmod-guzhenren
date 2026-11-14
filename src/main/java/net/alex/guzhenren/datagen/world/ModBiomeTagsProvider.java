package net.alex.guzhenren.datagen.world;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.world.ModBiomeTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 野生蛊与野猪生成位置的生物群系标签。
 *
 * <ul>
 *   <li>三个野生蛊标签（{@code HOPE_GU_SPAWNS}、{@code BOAR_GU_SPAWNS}、{@code RHINOCEROS_BEETLE_GU_SPAWNS}）共用一张
 *       39 个陆地群系的表，Gu World 为元泉[Spirit Spring]存有副本。</li>
 *   <li>野猪：原生 5 个森林；熊：全部森林类（12 个）；虎：3 个丛林；象：3 个热带草原与 3 个丛林。</li>
 *   <li>陆龟：3 个热带草原与稀疏丛林，即草地面的干燥群系（{@code animals_spawnable_on} 只含草方块）；牛：生成表含
 *       {@code minecraft:cow} 的 21 个原版群系（读自 1.21.1 群系 JSON），与原版牛同住。</li>
 *   <li>⚠ 不得收拢成 {@code #minecraft:is_overworld}：它含海洋，海洋表面在海平面。</li>
 *   <li>Gu World 四地区（南疆、西漠、东海、北原）群系不在原版标签：三个野生蛊标签另按 id 列各地区陆地群系作可选条目，两
 *       地区共有雨林只列一次，无 Gu World 照常加载；上述动物标签不含地区群系：地区只生成原版动物与野生蛊，Alex
 *       定。</li>
 *   <li>⚠ 地区列表即 Gu World 各地区名册的群系 id（那边 {@code Replicas}、{@code Ports} 与各地区包，按地区布局放置）：
 *       那边改名或新增，须同一任务改这里。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class ModBiomeTagsProvider extends TagsProvider<Biome> {

    private static final List<ResourceKey<Biome>> WILD_BOAR_BIOMES = List.of(
            Biomes.FOREST,
            Biomes.FLOWER_FOREST,
            Biomes.BIRCH_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.DARK_FOREST);
    private static final List<ResourceKey<Biome>> BEAR_BIOMES = List.of(
            Biomes.FOREST,
            Biomes.FLOWER_FOREST,
            Biomes.BIRCH_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.DARK_FOREST,
            Biomes.TAIGA,
            Biomes.SNOWY_TAIGA,
            Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA,
            Biomes.JUNGLE,
            Biomes.SPARSE_JUNGLE,
            Biomes.BAMBOO_JUNGLE);
    private static final List<ResourceKey<Biome>> TIGER_BIOMES = List.of(
            Biomes.JUNGLE,
            Biomes.SPARSE_JUNGLE,
            Biomes.BAMBOO_JUNGLE);
    private static final List<ResourceKey<Biome>> TORTOISE_BIOMES = List.of(
            Biomes.SAVANNA,
            Biomes.SAVANNA_PLATEAU,
            Biomes.WINDSWEPT_SAVANNA,
            Biomes.SPARSE_JUNGLE);
    private static final List<ResourceKey<Biome>> CATTLE_BIOMES = List.of(
            Biomes.PLAINS,
            Biomes.SUNFLOWER_PLAINS,
            Biomes.FOREST,
            Biomes.FLOWER_FOREST,
            Biomes.BIRCH_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.DARK_FOREST,
            Biomes.TAIGA,
            Biomes.SNOWY_TAIGA,
            Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA,
            Biomes.WINDSWEPT_HILLS,
            Biomes.WINDSWEPT_GRAVELLY_HILLS,
            Biomes.WINDSWEPT_FOREST,
            Biomes.WINDSWEPT_SAVANNA,
            Biomes.SAVANNA,
            Biomes.SAVANNA_PLATEAU,
            Biomes.JUNGLE,
            Biomes.SPARSE_JUNGLE,
            Biomes.BAMBOO_JUNGLE,
            Biomes.SWAMP);
    private static final List<ResourceKey<Biome>> ELEPHANT_BIOMES = List.of(
            Biomes.SAVANNA,
            Biomes.SAVANNA_PLATEAU,
            Biomes.WINDSWEPT_SAVANNA,
            Biomes.JUNGLE,
            Biomes.SPARSE_JUNGLE,
            Biomes.BAMBOO_JUNGLE);
    private static final List<String> SOUTHERN_BORDER_LAND = List.of("forested_plateau", "cloud_forest",
            "peach_grove", "misty_mountains", "bamboo_sea", "rainforest", "jungle_mountains", "emerald_peaks",
            "plains", "plateau", "hot_springs", "orchid_swamp", "alpine_meadow", "rocky_peaks", "painted_mountains",
            "granite_cliffs", "zhangshiyan_cliffs", "karst_peak_forest");
    private static final List<String> WESTERN_DESERT_LAND = List.of("ancient_sands", "arid_highlands", "badlands",
            "brushland", "bryce_canyon", "desert", "desert_canyon", "desert_oasis", "desert_spires", "eroded_badlands",
            "gravel_desert", "hot_shrubland", "lush_desert", "red_oasis", "sandstone_valley", "white_mesa",
            "wooded_badlands");
    private static final List<String> EASTERN_SEA_LAND = List.of("basalt_cliffs", "beach", "gravel_beach", "isles",
            "jungle", "mirage_isles", "rainforest", "stony_shore", "volcanic_crater", "volcanic_peaks",
            "white_cliffs");
    private static final List<String> NORTHERN_PLAINS_LAND = List.of("alpine_highlands", "birch_taiga",
            "cold_shrubland", "frozen_cliffs", "frozen_peaks", "glacial_chasm", "grove", "ice_marsh", "ice_spikes",
            "shield", "siberian_taiga", "snowy_plains", "snowy_shield", "snowy_taiga", "steppe", "wintry_forest",
            "wintry_lowlands");
    private static final List<String> REGION_LAND = getJoined(SOUTHERN_BORDER_LAND, WESTERN_DESERT_LAND,
            EASTERN_SEA_LAND, NORTHERN_PLAINS_LAND);
    private static final List<ResourceKey<Biome>> LAND_SPAWN_BIOMES = List.of(
            Biomes.PLAINS,
            Biomes.SUNFLOWER_PLAINS,
            Biomes.MEADOW,
            Biomes.CHERRY_GROVE,
            Biomes.FLOWER_FOREST,

            Biomes.FOREST,
            Biomes.BIRCH_FOREST,
            Biomes.DARK_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.WINDSWEPT_FOREST,

            Biomes.TAIGA,
            Biomes.SNOWY_TAIGA,
            Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA,
            Biomes.GROVE,

            Biomes.SAVANNA,
            Biomes.SAVANNA_PLATEAU,
            Biomes.WINDSWEPT_SAVANNA,

            Biomes.JUNGLE,
            Biomes.SPARSE_JUNGLE,
            Biomes.BAMBOO_JUNGLE,

            Biomes.DESERT,
            Biomes.BADLANDS,
            Biomes.WOODED_BADLANDS,
            Biomes.ERODED_BADLANDS,

            Biomes.SNOWY_PLAINS,
            Biomes.ICE_SPIKES,
            Biomes.SNOWY_SLOPES,
            Biomes.FROZEN_PEAKS,
            Biomes.JAGGED_PEAKS,
            Biomes.STONY_PEAKS,

            Biomes.WINDSWEPT_HILLS,
            Biomes.WINDSWEPT_GRAVELLY_HILLS,

            Biomes.SWAMP,
            Biomes.MANGROVE_SWAMP,

            Biomes.BEACH,
            Biomes.SNOWY_BEACH,
            Biomes.STONY_SHORE,

            Biomes.MUSHROOM_FIELDS);

    public ModBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Registries.BIOME, lookupProvider, Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        for (ResourceKey<Biome> biome : LAND_SPAWN_BIOMES) {
            tag(ModBiomeTags.HOPE_GU_SPAWNS).add(biome);
            tag(ModBiomeTags.BOAR_GU_SPAWNS).add(biome);
            tag(ModBiomeTags.RHINOCEROS_BEETLE_GU_SPAWNS).add(biome);
        }
        for (ResourceKey<Biome> biome : WILD_BOAR_BIOMES) {
            tag(ModBiomeTags.WILD_BOAR_SPAWNS).add(biome);
        }
        for (ResourceKey<Biome> biome : BEAR_BIOMES) {
            tag(ModBiomeTags.BEAR_SPAWNS).add(biome);
        }
        for (ResourceKey<Biome> biome : TIGER_BIOMES) {
            tag(ModBiomeTags.TIGER_SPAWNS).add(biome);
        }
        for (ResourceKey<Biome> biome : TORTOISE_BIOMES) {
            tag(ModBiomeTags.TORTOISE_SPAWNS).add(biome);
        }
        for (ResourceKey<Biome> biome : CATTLE_BIOMES) {
            tag(ModBiomeTags.CATTLE_SPAWNS).add(biome);
        }
        for (ResourceKey<Biome> biome : ELEPHANT_BIOMES) {
            tag(ModBiomeTags.ELEPHANT_SPAWNS).add(biome);
        }
        for (String biome : REGION_LAND) {
            tag(ModBiomeTags.HOPE_GU_SPAWNS).addOptional(getGuWorldBiome(biome));
            tag(ModBiomeTags.BOAR_GU_SPAWNS).addOptional(getGuWorldBiome(biome));
            tag(ModBiomeTags.RHINOCEROS_BEETLE_GU_SPAWNS).addOptional(getGuWorldBiome(biome));
        }
    }

    @SafeVarargs
    private static List<String> getJoined(List<String>... parts) {
        Set<String> joined = new LinkedHashSet<>();
        for (List<String> part : parts) {
            joined.addAll(part);
        }
        return List.copyOf(joined);
    }

    private static ResourceLocation getGuWorldBiome(String name) {
        return ResourceLocation.fromNamespaceAndPath("guworld", name);
    }
}
