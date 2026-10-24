package net.alex.guzhenren.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.world.ModBiomeTags;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The single provider for every datapack registry this mod writes.
 *
 * <p>Extends {@link net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider}. Builds damage
 * types and the wild-spawn biome modifiers in one {@code RegistrySetBuilder}; the dimensions and the
 * Spirit Spring [元泉] worldgen belong to the sibling mod Gu World. The
 * tag providers take {@code getRegistryProvider()} from this instance, not the plain lookup, so the tag
 * pass sees the types this run generates.
 *
 * <p>⚠ There can only be one. The builtin-entries provider reports a fixed name, so a second instance
 * fails datagen outright; add a registry to this one's builder instead.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class ModDatapackProvider extends DatapackBuiltinEntriesProvider {

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, ModDatapackProvider::registerDamageTypes)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModDatapackProvider::registerBiomeModifiers);

    public ModDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(Guzhenren.MOD_ID));
    }

    //region Damage types [伤害类型]
    private static void registerDamageTypes(BootstrapContext<DamageType> context) {
        context.register(ModDamageTypes.LIFESPAN_EXHAUSTED, new DamageType("guzhenren.lifespan_exhausted", 0.0F));
        context.register(ModDamageTypes.SOUL_COLLAPSE, new DamageType("guzhenren.soul_collapse", 0.0F));
        context.register(ModDamageTypes.MIND_OCEAN_SHATTERED, new DamageType("guzhenren.mind_ocean_shattered", 0.0F));
        context.register(ModDamageTypes.APERTURE_PRESSURE_EXPLOSION,
                new DamageType("guzhenren.aperture_pressure_explosion", 0.0F));
        context.register(ModDamageTypes.TEN_EXTREME_DISASTER,
                new DamageType("guzhenren.ten_extreme_disaster", 0.0F));
        context.register(ModDamageTypes.VITAL_GU_LOST, new DamageType("guzhenren.vital_gu_lost", 0.0F));
    }
    //endregion

    //region Biome modifiers [生态修改] -- where wild entities [野生实体] spawn
    private static final ResourceKey<BiomeModifier> SPAWN_HOPE_GU = createModifierKey("spawn_hope_gu");
    private static final ResourceKey<BiomeModifier> SPAWN_WHITE_BOAR_GU = createModifierKey("spawn_white_boar_gu");
    private static final ResourceKey<BiomeModifier> SPAWN_BLACK_BOAR_GU = createModifierKey("spawn_black_boar_gu");
    private static final ResourceKey<BiomeModifier> SPAWN_FLOWER_BOAR_GU = createModifierKey("spawn_flower_boar_gu");
    private static final ResourceKey<BiomeModifier> SPAWN_RHINOCEROS_BEETLE_GU =
            createModifierKey("spawn_rhinoceros_beetle_gu");
    private static final ResourceKey<BiomeModifier> SPAWN_WILD_BOAR = createModifierKey("spawn_wild_boar");
    private static final ResourceKey<BiomeModifier> SPAWN_BROWN_BEAR = createModifierKey("spawn_brown_bear");
    private static final ResourceKey<BiomeModifier> SPAWN_ASIAN_BLACK_BEAR =
            createModifierKey("spawn_asian_black_bear");
    private static final ResourceKey<BiomeModifier> SPAWN_AMERICAN_BLACK_BEAR =
            createModifierKey("spawn_american_black_bear");
    private static final ResourceKey<BiomeModifier> SPAWN_ALBINO_BEAR = createModifierKey("spawn_albino_bear");
    private static final ResourceKey<BiomeModifier> SPAWN_TIGER = createModifierKey("spawn_tiger");
    private static final ResourceKey<BiomeModifier> SPAWN_WHITE_TIGER = createModifierKey("spawn_white_tiger");
    private static final int HOPE_GU_SPAWN_WEIGHT = 8;
    private static final int HOPE_GU_PACK_MINIMUM = 1;
    private static final int HOPE_GU_PACK_MAXIMUM = 2;
    private static final int BOAR_SPAWN_WEIGHT = 4;
    private static final int BOAR_PACK_MINIMUM = 1;
    private static final int BOAR_PACK_MAXIMUM = 1;
    private static final int BEETLE_SPAWN_WEIGHT = 4;
    private static final int BEETLE_RANK_FOUR_SPAWN_WEIGHT = 2;
    private static final int BEETLE_RANK_FIVE_SPAWN_WEIGHT = 1;
    private static final int BEETLE_PACK_MINIMUM = 1;
    private static final int BEETLE_PACK_MAXIMUM = 1;
    private static final int WILD_BOAR_SPAWN_WEIGHT = 8;
    private static final int WILD_BOAR_PACK_MINIMUM = 1;
    private static final int WILD_BOAR_PACK_MAXIMUM = 3;
    private static final int BEAR_SPAWN_WEIGHT = 4;
    private static final int ALBINO_BEAR_SPAWN_WEIGHT = 1;
    private static final int TIGER_SPAWN_WEIGHT = 3;
    private static final int WHITE_TIGER_SPAWN_WEIGHT = 1;
    private static final int BEAST_PACK_MINIMUM = 1;
    private static final int BEAST_PACK_MAXIMUM = 1;

    private static void registerBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        context.register(SPAWN_HOPE_GU, createSpawns(biomes, ModBiomeTags.HOPE_GU_SPAWNS, createSpawner(
                ModEntityTypes.HOPE_GU_ENTITY.get(), HOPE_GU_SPAWN_WEIGHT,
                HOPE_GU_PACK_MINIMUM, HOPE_GU_PACK_MAXIMUM)));
        context.register(SPAWN_WHITE_BOAR_GU, createBoarSpawns(biomes, ModEntityTypes.WHITE_BOAR_GU_ENTITY.get()));
        context.register(SPAWN_BLACK_BOAR_GU, createBoarSpawns(biomes, ModEntityTypes.BLACK_BOAR_GU_ENTITY.get()));
        context.register(SPAWN_FLOWER_BOAR_GU, createBoarSpawns(biomes, ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get()));
        context.register(SPAWN_RHINOCEROS_BEETLE_GU, createSpawns(biomes, ModBiomeTags.RHINOCEROS_BEETLE_GU_SPAWNS,
                createBeetleSpawner(ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(), BEETLE_SPAWN_WEIGHT),
                createBeetleSpawner(ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get(), BEETLE_SPAWN_WEIGHT),
                createBeetleSpawner(ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get(), BEETLE_RANK_FOUR_SPAWN_WEIGHT),
                createBeetleSpawner(ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get(), BEETLE_RANK_FIVE_SPAWN_WEIGHT)));
        context.register(SPAWN_WILD_BOAR, createSpawns(biomes, ModBiomeTags.WILD_BOAR_SPAWNS, createSpawner(
                ModEntityTypes.WILD_BOAR.get(), WILD_BOAR_SPAWN_WEIGHT,
                WILD_BOAR_PACK_MINIMUM, WILD_BOAR_PACK_MAXIMUM)));
        context.register(SPAWN_BROWN_BEAR, createBeastSpawns(biomes, ModBiomeTags.BEAR_SPAWNS,
                ModEntityTypes.BROWN_BEAR.get(), BEAR_SPAWN_WEIGHT));
        context.register(SPAWN_ASIAN_BLACK_BEAR, createBeastSpawns(biomes, ModBiomeTags.BEAR_SPAWNS,
                ModEntityTypes.ASIAN_BLACK_BEAR.get(), BEAR_SPAWN_WEIGHT));
        context.register(SPAWN_AMERICAN_BLACK_BEAR, createBeastSpawns(biomes, ModBiomeTags.BEAR_SPAWNS,
                ModEntityTypes.AMERICAN_BLACK_BEAR.get(), BEAR_SPAWN_WEIGHT));
        context.register(SPAWN_ALBINO_BEAR, createBeastSpawns(biomes, ModBiomeTags.BEAR_SPAWNS,
                ModEntityTypes.ALBINO_BEAR.get(), ALBINO_BEAR_SPAWN_WEIGHT));
        context.register(SPAWN_TIGER, createBeastSpawns(biomes, ModBiomeTags.TIGER_SPAWNS,
                ModEntityTypes.TIGER.get(), TIGER_SPAWN_WEIGHT));
        context.register(SPAWN_WHITE_TIGER, createBeastSpawns(biomes, ModBiomeTags.TIGER_SPAWNS,
                ModEntityTypes.WHITE_TIGER.get(), WHITE_TIGER_SPAWN_WEIGHT));
    }

    private static ResourceKey<BiomeModifier> createModifierKey(String id) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Guzhenren.id(id));
    }

    private static BiomeModifier createSpawns(HolderGetter<Biome> biomes, TagKey<Biome> where,
                                              MobSpawnSettings.SpawnerData... spawners) {
        return new BiomeModifiers.AddSpawnsBiomeModifier(biomes.getOrThrow(where), List.of(spawners));
    }

    private static MobSpawnSettings.SpawnerData createSpawner(EntityType<?> type, int weight, int minimum,
                                                              int maximum) {
        return new MobSpawnSettings.SpawnerData(type, weight, minimum, maximum);
    }

    private static MobSpawnSettings.SpawnerData createBeetleSpawner(EntityType<?> type, int weight) {
        return createSpawner(type, weight, BEETLE_PACK_MINIMUM, BEETLE_PACK_MAXIMUM);
    }

    private static BiomeModifier createBoarSpawns(HolderGetter<Biome> biomes, EntityType<?> type) {
        return createSpawns(biomes, ModBiomeTags.BOAR_GU_SPAWNS,
                createSpawner(type, BOAR_SPAWN_WEIGHT, BOAR_PACK_MINIMUM, BOAR_PACK_MAXIMUM));
    }

    private static BiomeModifier createBeastSpawns(HolderGetter<Biome> biomes, TagKey<Biome> where, EntityType<?> type,
                                                   int weight) {
        return createSpawns(biomes, where, createSpawner(type, weight, BEAST_PACK_MINIMUM, BEAST_PACK_MAXIMUM));
    }
    //endregion
}
