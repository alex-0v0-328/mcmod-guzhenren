package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * The Heavenly Mugwort shield [天蓬护盾]: the points left, the points it was raised with and its end tick.
 * Immutable record attachment keyed {@code mugwort_shield_data}; {@code MugwortShieldService} is the only writer.
 *
 * <p>The shield is up while points remain and the game time is short of {@code endTick}; {@code max} is kept only
 * so the HUD can read {@code points / max}. The {@code HEAVENLY_MUGWORT_GU} effect is only its projection.
 *
 * @author Alex
 * @version 1.0.0
 * @see MugwortShieldService
 * @since 1.0.0
 */

public record MugwortShieldData(float points, float max, long endTick) {

    public static final MugwortShieldData DEFAULT = new MugwortShieldData(0.0F, 0.0F, BodyData.UNTRACKED);

    public static final Codec<MugwortShieldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("points", 0.0F).forGetter(MugwortShieldData::points),
            Codec.FLOAT.optionalFieldOf("max", 0.0F).forGetter(MugwortShieldData::max),
            Codec.LONG.optionalFieldOf("end_tick", BodyData.UNTRACKED).forGetter(MugwortShieldData::endTick)
    ).apply(instance, MugwortShieldData::new));

    public static @NotNull MugwortShieldData decode(@NotNull FriendlyByteBuf buf) {
        return new MugwortShieldData(buf.readFloat(), buf.readFloat(), buf.readVarLong());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull MugwortShieldData value) {
        buf.writeFloat(value.points());
        buf.writeFloat(value.max());
        buf.writeVarLong(value.endTick());
    }

    public boolean isUp(long now) { return points > 0.0F && now < endTick; }

    public long getTicksLeft(long now) { return Math.max(0L, endTick - now); }

    public MugwortShieldData withPoints(float value) { return new MugwortShieldData(value, max, endTick); }
}
