package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * One shield [护盾] layer on {@code ShieldData}: its kind, the points left, the points it was raised with and its
 * end tick.
 *
 * <p>The layer is up while points remain and the game time is short of {@code endTick}; {@code max} is kept only
 * so the HUD can read {@code points / max}.
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldData
 * @since 1.0.0
 */

public record ShieldLayer(@NotNull ShieldType type, float points, float max, long endTick) {

    public static final Codec<ShieldLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ShieldType.CODEC.fieldOf("type").forGetter(ShieldLayer::type),
            Codec.FLOAT.optionalFieldOf("points", 0.0F).forGetter(ShieldLayer::points),
            Codec.FLOAT.optionalFieldOf("max", 0.0F).forGetter(ShieldLayer::max),
            Codec.LONG.optionalFieldOf("end_tick", BodyData.UNTRACKED).forGetter(ShieldLayer::endTick)
    ).apply(instance, ShieldLayer::new));

    public static @NotNull ShieldLayer decode(@NotNull FriendlyByteBuf buf) {
        return new ShieldLayer(buf.readEnum(ShieldType.class), buf.readFloat(), buf.readFloat(), buf.readVarLong());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull ShieldLayer value) {
        buf.writeEnum(value.type());
        buf.writeFloat(value.points());
        buf.writeFloat(value.max());
        buf.writeVarLong(value.endTick());
    }

    public boolean isUp(long now) { return points > 0.0F && now < endTick; }

    public long getTicksLeft(long now) { return Math.max(0L, endTick - now); }

    public ShieldLayer withPoints(float value) { return new ShieldLayer(type, value, max, endTick); }
}
