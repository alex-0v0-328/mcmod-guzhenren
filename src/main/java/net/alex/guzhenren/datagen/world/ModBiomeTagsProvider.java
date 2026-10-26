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
 * Writes the biome tags deciding where wild Gu [野生蛊虫] and the wild boar [野猪] may spawn.
 *
 * <p>Extends {@link net.minecraft.data.tags.TagsProvider} for {@link net.minecraft.world.level.biome.Biome}.
 * Populates the hope, boar and rhinoceros beetle Gu tags from one shared list of 39 land biomes (the
 * sibling mod Gu World keeps a copy for the Spirit Spring [元泉]), the wild boar tag
 * from the five forest biomes where it naturally occurs, the bear tag from every forest-family biome
 * (12), and the tiger tag from the three jungle biomes.
 * Must NOT collapse to {@code #minecraft:is_overworld} because that carries the
 * oceans, whose surface sits at sea level.
 *
 * <p>The four regions of Gu World, the Southern Border [南疆], the Western Desert [西漠], the Eastern Sea [东海] and
 * the Northern Plains [北原], draw on that mod's own biomes, in no vanilla tag, so the three wild Gu tags also name
 * each region's land biomes by id, as optional entries, a biome two regions share (the rainforest) once: the tags still
 * load without Gu World. The wild boar, bear
 * and tiger tags name no region biome: the regions spawn vanilla animals and wild Gu only, by Alex's decision.
 *
 * <p>⚠ The region lists are Gu World's biome ids, those of each region's roster ({@code Replicas}, {@code Ports}
 * and the region packages there, placed by each region's layout): renaming or adding one there means changing it
 * here in the same task.
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
