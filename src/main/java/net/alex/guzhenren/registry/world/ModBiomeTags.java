package net.alex.guzhenren.registry.world;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 本模组声明的生物群系标签键定义，指定各野生蛊族与野兽自然生成的陆地群系范围（元泉[Spirit Spring]生成群系标签归配套模组
 * Gu World）。
 * <ul>
 *   <li>野猪仅包含 5 种森林群系；熊包含全部森林、针叶林与丛林类群系；虎归入 3 种丛林；龟归入 3 种热带草原与稀疏丛林；象
 *       归入 3 种热带草原与 3 种丛林；牛沿用原版刷牛的 21 个群系。</li>
 *   <li>⚠ 野生蛊的群系标签严禁合并使用 {@code #minecraft:is_overworld}：主世界标签包含海平面高度的海洋群系，仅靠高度检
 *       查无法阻止野生蛊在水面上生成。</li>
 * </ul>
 */

public final class ModBiomeTags {

    private ModBiomeTags() {}

    public static final TagKey<Biome> HOPE_GU_SPAWNS = key("hope_gu_spawns");
    public static final TagKey<Biome> BOAR_GU_SPAWNS = key("boar_gu_spawns");
    public static final TagKey<Biome> RHINOCEROS_BEETLE_GU_SPAWNS = key("rhinoceros_beetle_gu_spawns");
    public static final TagKey<Biome> WILD_BOAR_SPAWNS = key("wild_boar_spawns");
    public static final TagKey<Biome> BEAR_SPAWNS = key("bear_spawns");
    public static final TagKey<Biome> TIGER_SPAWNS = key("tiger_spawns");
    public static final TagKey<Biome> TORTOISE_SPAWNS = key("tortoise_spawns");
    public static final TagKey<Biome> CATTLE_SPAWNS = key("cattle_spawns");
    public static final TagKey<Biome> ELEPHANT_SPAWNS = key("elephant_spawns");

    private static TagKey<Biome> key(String name) {
        return TagKey.create(Registries.BIOME, Guzhenren.id(name));
    }
}
