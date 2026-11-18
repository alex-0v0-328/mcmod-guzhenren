package net.alex.guzhenren.registry.world;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 本模组跨维度旅行所使用的锚定维度 {@link ResourceKey} 与落点白名单定义。
 * <ul>
 *   <li>维度的实际类型、群系、{@code LevelStem} 与天空渲染归属配套模组 Gu World（{@code guworld}）；因本模组不能反向依
 *       赖 Gu World 的类，此处仅按 ID 字符串约定声明维度键；未安装 Gu World 时维度不存在，{@code /guworld enter} 会因
 *       {@link net.alex.guzhenren.gameplay.dimension.DimensionTravelService#enter} 找不到目标维度而拒绝传送，显示名
 *       {@code dimension.<namespace>.<path>} 亦由 Gu World 提供。</li>
 *   <li>⚠ {@link #TREASURE_YELLOW_HEAVEN} 与四地区维度键直接手写 {@code guworld} 命名空间（无法直接引用其常量）：若 Gu
 *       World 侧修改任意维度 ID，本类必须同步修改，否则维度传送将失效。</li>
 *   <li>{@link AnchoredDimension} 描述 {@code /guworld enter} 的合法目标（进入后应通过 {@code /guworld exit} 沿记录点返
 *       回）：{@link #spawn} 为固定目标坐标，{@link #rank} 标明转数（6–7 为福地、8–9 为洞天，凡间记 0），
 *       {@link #timeRate} 为相对主世界的时间流速倍率（乘入 {@code TimeFlow#getRate}，暂均为 1.0）。</li>
 *   <li>{@link Arrival#FLIGHT} 降落在固定坐标并授予区域飞行权限，{@link Arrival#SURFACE} 则忽略坐标 y 值、降落在该柱面
 *       最高的阻挡移动或含流体方块顶面且不授予飞行。</li>
 *   <li>{@link #ANCHORED_DIMENSIONS} 作为安全白名单：宝黄天[Treasure Yellow Heaven]为以飞行进入的八转洞天，凡间四地区
 *       （南疆、西漠、东海、北原）则步行进入并落于原点上方地表（东海若原点无岛则落于海面）；后续新增福地与洞天统一在此
 *       登记。</li>
 * </ul>
 */

public final class ModDimensions {

    private ModDimensions() {}

    public static final ResourceKey<Level> TREASURE_YELLOW_HEAVEN = createGuWorldLevel("treasure_yellow_heaven");
    public static final Vec3 TREASURE_YELLOW_HEAVEN_SPAWN = new Vec3(0.5, 64.0, 0.5);
    public static final ResourceKey<Level> SOUTHERN_BORDER = createGuWorldLevel("southern_border");
    public static final ResourceKey<Level> WESTERN_DESERT = createGuWorldLevel("western_desert");
    public static final ResourceKey<Level> EASTERN_SEA = createGuWorldLevel("eastern_sea");
    public static final ResourceKey<Level> NORTHERN_PLAINS = createGuWorldLevel("northern_plains");
    private static final Vec3 REGION_ORIGIN = new Vec3(0.5, 0.0, 0.5);

    public enum Arrival { FLIGHT, SURFACE }

    public record AnchoredDimension(ResourceKey<Level> level, Vec3 spawn, Arrival arrival, int rank, double timeRate) {}

    public static final Map<ResourceKey<Level>, AnchoredDimension> ANCHORED_DIMENSIONS = Map.of(
            TREASURE_YELLOW_HEAVEN,
            new AnchoredDimension(TREASURE_YELLOW_HEAVEN, TREASURE_YELLOW_HEAVEN_SPAWN, Arrival.FLIGHT, 8, 1.0),
            SOUTHERN_BORDER, new AnchoredDimension(SOUTHERN_BORDER, REGION_ORIGIN, Arrival.SURFACE, 0, 1.0),
            WESTERN_DESERT, new AnchoredDimension(WESTERN_DESERT, REGION_ORIGIN, Arrival.SURFACE, 0, 1.0),
            EASTERN_SEA, new AnchoredDimension(EASTERN_SEA, REGION_ORIGIN, Arrival.SURFACE, 0, 1.0),
            NORTHERN_PLAINS, new AnchoredDimension(NORTHERN_PLAINS, REGION_ORIGIN, Arrival.SURFACE, 0, 1.0));

    private static ResourceKey<Level> createGuWorldLevel(String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("guworld", path));
    }

    public static boolean isFlightGranted(ResourceKey<Level> level) {
        AnchoredDimension dimension = ANCHORED_DIMENSIONS.get(level);
        return dimension != null && dimension.arrival() == Arrival.FLIGHT;
    }
}
