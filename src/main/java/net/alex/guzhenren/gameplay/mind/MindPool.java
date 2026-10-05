package net.alex.guzhenren.gameplay.mind;

import com.google.common.math.LongMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One mind pool of one {@link MindPoolType}; leaf record nested inside {@link MindData};
 * immutable, standard {@code DEFAULT}-codec-stream shape.
 *
 * <p>⚠ Not self-clamping: only some wisdom types may burst past their cap, so the clamp lives in {@link
 * MindService} and every write passes through there. ⚠
 * {@code burstAt()} divides before multiplying ({@code max / DENOM * NUMER}) and saturates the final
 * product, preserving the intended floor-before-multiply ratio without overflow. ⚠ {@code slept()}
 * restores only HALF the deficit when the buffer was used -- never reduce {@code current}.
 *
 * @author Alex
 * @version 1.0.0
 * @see MindData
 * @see MindService
 * @since 1.0.0
 */

public record MindPool(long current, long max, boolean bufferUsed) {

    public static final Codec<MindPool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("current", 0L).forGetter(MindPool::current),
            Codec.LONG.optionalFieldOf("max", 0L).forGetter(MindPool::max),
            Codec.BOOL.optionalFieldOf("buffer_used", false).forGetter(MindPool::bufferUsed)
    ).apply(instance, MindPool::new));
    public static final StreamCodec<ByteBuf, MindPool> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, MindPool::current,
            ByteBufCodecs.VAR_LONG, MindPool::max,
            ByteBufCodecs.BOOL, MindPool::bufferUsed,
            MindPool::new);

    public MindPool {
        current = Math.max(0L, current);
        max = Math.max(0L, max);
        bufferUsed = bufferUsed || current > max;
    }

    public static MindPool of(MindPoolType type) { return new MindPool(0L, type.getDefaultCapacity(), false); }

    public long burstAt() {
        return LongMath.saturatedMultiply(max / MindPoolType.BURST_DENOMINATOR, MindPoolType.BURST_NUMERATOR);
    }

    public boolean isOverflowing() { return current > burstAt(); }

    public MindPool withCurrent(long value) { return new MindPool(value, max, bufferUsed); }

    public MindPool withMax(long value) { return new MindPool(current, value, bufferUsed); }

    public MindPool slept() {
        long restored = bufferUsed && current < max ? current + (max - current) / 2 : Math.max(current, max);
        return new MindPool(restored, max, false);
    }

    public MindPool emptied() { return new MindPool(0L, max, false); }
}
