package net.alex.guzhenren.datagen.world;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.world.ModBiomeTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
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
    }
}
