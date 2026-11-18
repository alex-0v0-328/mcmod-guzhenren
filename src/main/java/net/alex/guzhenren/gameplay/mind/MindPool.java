package net.alex.guzhenren.gameplay.mind;

import com.google.common.math.LongMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 单个 {@link MindPoolType} 脑海池的不可变记录（嵌于 {@link MindData}）。
 * <ul>
 *   <li>⚠ 紧凑构造器仅保证非负而不把 {@code current} 夹取到 {@code max}：因为部分池类型允许超出上限进入缓冲区，上限夹取
 *       统一由 {@link MindService} 在每次写入时按池类型执行。</li>
 *   <li>⚠ {@link #burstAt} 采用先除后饱和乘（{@code max / BURST_DENOMINATOR * BURST_NUMERATOR}）并保底为 {@code max}，
 *       防止大上限乘法溢出，且确保满池（{@code current == max}）永不被判为超限（{@link #isOverflowing}）。</li>
 *   <li>⚠ 动用过超限缓冲区（{@code bufferUsed}）时，睡眠恢复 {@link #withSlept} 仅补齐缺失额的一半并清除标记，且绝不会
 *       把已高于 {@code max} 的当前值压低。</li>
 * </ul>
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
