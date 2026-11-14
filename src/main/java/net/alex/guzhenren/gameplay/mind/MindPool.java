package net.alex.guzhenren.gameplay.mind;

import com.google.common.math.LongMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 一种 {@link MindPoolType} 的脑海池，叶子 record，嵌在 {@link MindData} 内。
 *
 * <ul>
 *   <li>⚠ 不自行夹紧：只有部分池类型可超上限，夹紧在 {@code MindService}，每次写入都经过。</li>
 *   <li>⚠ {@code burstAt()} 先除后乘（{@code max / DENOM * NUMER}），末积饱和，保比例不溢出；不低于 {@code max}，
 *       满池不算超限。</li>
 *   <li>⚠ 用过缓冲时 {@code withSlept()} 只补缺额一半；从不降 {@code current}。</li>
 * </ul>
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

    public MindPool withSlept() {
        long restored = bufferUsed && current < max ? current + (max - current) / 2 : Math.max(current, max);
        return new MindPool(restored, max, false);
    }

    public MindPool withEmptied() { return new MindPool(0L, max, false); }
}
