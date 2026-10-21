package net.alex.guzhenren.gameplay.mind;

import com.google.common.math.LongMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * One mind pool of one {@link MindPoolType}; leaf record nested inside {@link MindData};
 * immutable, standard {@code DEFAULT}-codec-stream shape.
 *
 * <p>⚠ Not self-clamping: only some wisdom types may burst past their cap, so the clamp lives in {@code
 * MindService} and every write passes through there. ⚠
 * {@code burstAt()} divides before multiplying ({@code max / DENOM * NUMER}) and saturates the final
 * product, preserving the intended floor-before-multiply ratio without overflow; it never falls below
 * {@code max}, so a full pool is never overflowing, whatever its cap. ⚠ {@code slept()}
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

    public MindPool {
        current = Math.max(0L, current);
        max = Math.max(0L, max);
        bufferUsed = bufferUsed || current > max;
    }

    public static @NotNull MindPool decode(@NotNull FriendlyByteBuf buf) {
        return new MindPool(buf.readVarLong(), buf.readVarLong(), buf.readBoolean());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull MindPool value) {
        buf.writeVarLong(value.current());
        buf.writeVarLong(value.max());
        buf.writeBoolean(value.bufferUsed());
    }

    public static MindPool of(MindPoolType type) { return new MindPool(0L, type.getDefaultCapacity(), false); }

    public long burstAt() {
        long burst = LongMath.saturatedMultiply(max / MindPoolType.BURST_DENOMINATOR, MindPoolType.BURST_NUMERATOR);
        return Math.max(max, burst);
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
