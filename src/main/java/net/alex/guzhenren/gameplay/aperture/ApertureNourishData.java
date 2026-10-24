package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * Nourishing the Aperture [温养空窍]: whether a run is going, and which aperture it feeds. Immutable
 * record attachment keyed {@code nourish_data}; {@code
 * ApertureNourishService} is the only runtime writer; progress
 * percent and the petrified latch live on {@link Aperture} itself.
 *
 * <p>⚠ {@code starvedSinceTick} defaults to {@code NOT_STARVED} (-1), never {@code 0} -- zero is a real
 * game time, so a fresh world would read as starved since its beginning. ⚠ {@code target} is only
 * trusted once the service has clamped it against the apertures that exist, never straight from the wire.
 *
 * @author Alex
 * @version 1.0.0
 * @see Aperture
 * @see ApertureNourishService
 * @since 1.0.0
 */

public record ApertureNourishData(boolean cultivating, int target, long starvedSinceTick) {

    public static final int FULL = 100;
    public static final long NOT_STARVED = -1L;
    public static final long STARVE_GRACE_TICKS = 10L * Ticks.SECOND;
    public static final ApertureNourishData DEFAULT = new ApertureNourishData(false, ApertureData.PRIMARY, NOT_STARVED);
    public static final Codec<ApertureNourishData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("cultivating", false).forGetter(ApertureNourishData::cultivating),
            Codec.INT.optionalFieldOf("target", ApertureData.PRIMARY).forGetter(ApertureNourishData::target),
            Codec.LONG.optionalFieldOf("starved_since_tick", NOT_STARVED)
                    .forGetter(ApertureNourishData::starvedSinceTick)
    ).apply(instance, ApertureNourishData::new));

    public static @NotNull ApertureNourishData decode(@NotNull FriendlyByteBuf buf) {
        return new ApertureNourishData(buf.readBoolean(), buf.readVarInt(), buf.readVarLong());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull ApertureNourishData value) {
        buf.writeBoolean(value.cultivating());
        buf.writeVarInt(value.target());
        buf.writeVarLong(value.starvedSinceTick());
    }

    public boolean isStarving() { return starvedSinceTick != NOT_STARVED; }

    public boolean hasStarvedOut(long now) { return isStarving() && now - starvedSinceTick >= STARVE_GRACE_TICKS; }

    public ApertureNourishData withCultivating(boolean value) {
        return new ApertureNourishData(value, target, starvedSinceTick);
    }

    public ApertureNourishData withTarget(int value) {
        return new ApertureNourishData(cultivating, value, starvedSinceTick);
    }

    public ApertureNourishData withStarvedSinceTick(long value) {
        return new ApertureNourishData(cultivating, target, value);
    }
}
