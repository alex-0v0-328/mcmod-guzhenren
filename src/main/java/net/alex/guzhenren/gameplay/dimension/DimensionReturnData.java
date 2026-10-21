package net.alex.guzhenren.gameplay.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Return-point snapshot for anchored dimension travel [宝黄天 / 福地 / 洞天]. A player can only be inside
 * one anchored dimension at a time, so one optional {@link ReturnPoint} is enough. The record is
 * server-only: it is serialized to NBT but never synced to clients.
 *
 * <p>The compact constructor rejects a null outer {@code point}; the nested compact constructor rejects
 * a null level key.
 *
 * <p>{@link ReturnPoint} is one saved location: dimension, position, rotation, and whether the
 * player was flying on entry. Flight permission itself is not saved -- the anchored-dimension grant
 * is a transient attribute modifier, so a record written before it may still carry a {@code mayfly}
 * key that decoding ignores.
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
