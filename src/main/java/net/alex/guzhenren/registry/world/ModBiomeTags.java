package net.alex.guzhenren.registry.world;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 生物群系标签：各野生蛊族与野兽生成的陆地群系。
 *
 * <ul>
 *   <li>元泉[Spirit Spring]生成群系归姊妹模组 Gu World 的标签。</li>
 *   <li>野猪有意只含五个森林；熊含全部森林类：野猪的五个、四针叶林、三丛林。</li>
 *   <li>虎三丛林；龟三热带草原与稀疏丛林；象三热带草原与三丛林；牛为原版刷牛的 21 个群系。</li>
 *   <li>⚠ 野生蛊列表不得并成 {@code #minecraft:is_overworld}：其海洋地表在海平面，高度检查无法让野生蛊离水。</li>
 * </ul>
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
    public static final TagKey<Biome> TORTOISE_SPAWNS = key("tortoise_spawns");
    public static final TagKey<Biome> CATTLE_SPAWNS = key("cattle_spawns");
    public static final TagKey<Biome> ELEPHANT_SPAWNS = key("elephant_spawns");

    private static TagKey<Biome> key(String name) {
        return TagKey.create(Registries.BIOME, Guzhenren.id(name));
    }
}
