package net.alex.guzhenren.registry.world;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * The biome tags this mod declares.
 *
 * <p>Tag-key holder (not a DeferredRegister) for the land biomes each wild Gu family and the wild
 * beasts spawn in; where the Spirit Spring [元泉] generates is the sibling mod Gu World's tag.
 * The wild boar tag is deliberately limited to its five forest biomes; the bear tag carries every
 * forest-family biome (the boar's five, the four taigas and the three jungles), and the tiger tag
 * the three jungles.
 * The wild Gu lists must NOT collapse to {@code #minecraft:is_overworld} -- that
 * carries the oceans, whose surface sits at sea level where the height check cannot hold a mote off
 * the water.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModBiomeTags {

    private ModBiomeTags() {}

    public static final TagKey<Biome> HOPE_GU_SPAWNS = key("hope_gu_spawns");
    public static final TagKey<Biome> BOAR_GU_SPAWNS = key("boar_gu_spawns");
    public static final TagKey<Biome> RHINOCEROS_BEETLE_GU_SPAWNS = key("rhinoceros_beetle_gu_spawns");
    public static final TagKey<Biome> WILD_BOAR_SPAWNS = key("wild_boar_spawns");
    public static final TagKey<Biome> BEAR_SPAWNS = key("bear_spawns");
    public static final TagKey<Biome> TIGER_SPAWNS = key("tiger_spawns");

    private static TagKey<Biome> key(String name) {
        return TagKey.create(Registries.BIOME, Guzhenren.id(name));
    }
}
