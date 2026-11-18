package net.alex.guzhenren.gameplay.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 锚定维度旅行的返回点快照附件（用于宝黄天[Treasure Yellow Heaven]、福地、洞天）。
 * <ul>
 *   <li>玩家同一时刻最多身处一个锚定维度，因此只需保存单个可选 {@link ReturnPoint}；仅服务端落盘、不同步，外层
 *       {@code point} 与内层维度键均不可为 {@code null}。</li>
 *   <li>{@code flying} 记录进入锚定维度前是否正在飞行；飞行授权本身不存入此记录（锚定维度内的飞行授权通过临时属性修饰符
 *       挂载，旧存档残留的 {@code mayfly} 字段在反序列化时自动忽略）。</li>
 * </ul>
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
