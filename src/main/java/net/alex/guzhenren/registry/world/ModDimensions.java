package net.alex.guzhenren.registry.world;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 本模组维度旅行所用锚定维度的维度键。
 *
 * <ul>
 *   <li>维度本身（维度类型、生物群系、LevelStem、天空效果）归姊妹模组 Gu World（{@code guworld}）；它依赖本模组、
 *       本模组看不到其类，故此处只用键指名，由其 LevelStem 守约。</li>
 *   <li>未装该模组时维度不存在，{@code /guworld enter} 拒绝一切目标
 *       （{@link net.alex.guzhenren.gameplay.dimension.DimensionTravelService#enter} 找不到维度）。</li>
 *   <li>显示名 {@code dimension.<namespace>.<path>} 也是该模组的语言键。</li>
 *   <li>⚠ {@link #TREASURE_YELLOW_HEAVEN} 与四地区键手写 Gu World 命名空间（拿不到其 id 常量）；那边改名这里须同改，
 *       否则无法进入。</li>
 *   <li>{@link AnchoredDimension}：{@code /guworld enter} 可选目标，身处其中应经 {@code /guworld exit} 离开才用上记录的
 *       返回点；{@link AnchoredDimension#spawn} 是每个进入者的固定落点。</li>
 *   <li>{@link AnchoredDimension#rank}：6–9 转（6–7 福地，8–9 洞天），凡间记 0；{@link AnchoredDimension#timeRate}：
 *       相对主世界的时间流速，乘进其中玩家的时钟（{@code TimeFlow#getRate}），低于 1 变慢，Alex 设定前皆为 1。</li>
 *   <li>{@link Arrival#FLIGHT} 落于落点本身，停留时给飞行；{@link Arrival#SURFACE} 落于该柱顶（最高的阻挡移动或含流体
 *       方块，不用落点 y），不给飞行。</li>
 *   <li>{@link #ANCHORED_DIMENSIONS} 为白名单：宝黄天[Treasure Yellow Heaven]是飞行抵达的八转洞天；凡间四地区南疆、
 *       西漠、东海、北原步行进入，落原点上方地表，东海无岛则落海面；今后的福地、洞天在此登记。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
