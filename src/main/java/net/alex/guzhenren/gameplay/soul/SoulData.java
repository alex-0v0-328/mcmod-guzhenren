package net.alex.guzhenren.gameplay.soul;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Soul [魂魄], the one pool that is lethal at the bottom. Immutable record attachment keyed {@code
 * soul_data}; {@link SoulService} is the only runtime writer; the
 * compact ctor floors {@code maxSoul} at zero and clamps {@code currentSoul} to {@code [0, max]}.
 *
 * <p>⚠ The cap is STORED rather than derived, because nothing else determines it -- compare Epic Fight
 * stamina, whose cap the combat system owns. ⚠ {@code revived()} returns {@code 1} (not 0) and restores
 * {@code DEFAULT_MAX_SOUL} when the cap itself was 0: a respawn may never hand back a value the lethal
 * check would fire on.
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulService
 * @see SoulTier
 * @since 1.0.0
 */

public record SoulData(long maxSoul, long currentSoul) {

    public static final long DEFAULT_MAX_SOUL = 100L;
    public static final long REVIVED_SOUL = 1L;
    public static final SoulData DEFAULT = new SoulData(DEFAULT_MAX_SOUL, DEFAULT_MAX_SOUL);
    public static final Codec<SoulData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("max_soul", DEFAULT_MAX_SOUL).forGetter(SoulData::maxSoul),
            Codec.LONG.optionalFieldOf("current_soul", DEFAULT_MAX_SOUL).forGetter(SoulData::currentSoul)
    ).apply(instance, SoulData::new));
    public static final StreamCodec<ByteBuf, SoulData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SoulData::maxSoul,
            ByteBufCodecs.VAR_LONG, SoulData::currentSoul,
            SoulData::new);

    public SoulData {
        maxSoul = Math.max(0L, maxSoul);
        currentSoul = Math.clamp(currentSoul, 0L, maxSoul);
    }

    public SoulTier tier() { return SoulTier.fromSoul(maxSoul); }

    public boolean isCollapsed() { return currentSoul <= 0L; }

    public SoulData withMaxSoul(long value) { return new SoulData(value, currentSoul); }

    public SoulData withCurrentSoul(long value) { return new SoulData(maxSoul, value); }

    public SoulData refilled() { return new SoulData(maxSoul, maxSoul); }

    public SoulData revived() {
        return new SoulData(maxSoul > 0L ? maxSoul : DEFAULT_MAX_SOUL, REVIVED_SOUL);
    }
}
