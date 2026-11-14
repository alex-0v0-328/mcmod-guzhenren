package net.alex.guzhenren.gameplay.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 锚定维度旅行的返回点快照（宝黄天[Treasure Yellow Heaven]、福地、洞天）。同时只在一个锚定维度，一个可选
 * {@link ReturnPoint} 够用；仅服务端，存 NBT 不同步；外层 {@code point}、内层维度键非 {@code null}；{@code flying}
 * 指进入时在飞。飞行权限不存：锚定维度授权是临时属性修饰符，旧记录的 {@code mayfly} 键解码忽略。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.dimension.DimensionTravelService
 * @since 1.0.0
 */

public record DimensionReturnData(Optional<ReturnPoint> point) {

    public static final DimensionReturnData DEFAULT = new DimensionReturnData(Optional.empty());

    public static final Codec<DimensionReturnData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ReturnPoint.CODEC.optionalFieldOf("point").forGetter(DimensionReturnData::point)
    ).apply(instance, DimensionReturnData::new));

    public DimensionReturnData {
        Objects.requireNonNull(point, "point");
    }

    public boolean isPresent() {
        return point.isPresent();
    }

    public static DimensionReturnData of(@NotNull ReturnPoint point) {
        return new DimensionReturnData(Optional.of(point));
    }

    public static DimensionReturnData of(@NotNull ResourceKey<Level> level, double x, double y, double z,
            float yaw, float pitch, boolean flying) {
        return of(new ReturnPoint(level, x, y, z, yaw, pitch, flying));
    }

    public record ReturnPoint(ResourceKey<Level> level, double x, double y, double z, float yaw, float pitch,
            boolean flying) {

        public static final Codec<ReturnPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("level").forGetter(ReturnPoint::level),
                Codec.DOUBLE.fieldOf("x").forGetter(ReturnPoint::x),
                Codec.DOUBLE.fieldOf("y").forGetter(ReturnPoint::y),
                Codec.DOUBLE.fieldOf("z").forGetter(ReturnPoint::z),
                Codec.FLOAT.fieldOf("yaw").forGetter(ReturnPoint::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(ReturnPoint::pitch),
                Codec.BOOL.fieldOf("flying").forGetter(ReturnPoint::flying)
        ).apply(instance, ReturnPoint::new));

        public ReturnPoint {
            Objects.requireNonNull(level, "level");
        }
    }
}
