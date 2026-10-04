package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.alex.guzhenren.Ticks;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Nourishing the Aperture [温养空窍]: whether a run is going, and which aperture it feeds. Immutable
 * record attachment keyed {@code nourish_data}; {@link
 * ApertureNourishService} is the only writer; progress
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
    public static final StreamCodec<ByteBuf, ApertureNourishData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ApertureNourishData::cultivating,
            ByteBufCodecs.VAR_INT, ApertureNourishData::target,
            ByteBufCodecs.VAR_LONG, ApertureNourishData::starvedSinceTick,
            ApertureNourishData::new);

    public boolean isStarving() { return starvedSinceTick != NOT_STARVED; }

    public boolean starvedOut(long now) { return isStarving() && now - starvedSinceTick >= STARVE_GRACE_TICKS; }

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
