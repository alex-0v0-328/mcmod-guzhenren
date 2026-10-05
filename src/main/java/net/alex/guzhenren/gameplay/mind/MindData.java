package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.core.ModStreamCodecs;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Mind [脑海]: the brilliance [才情] and the three thought [念] pools it drives. Immutable record
 * attachment keyed {@code mind_data}; {@link MindService}
 * is the only writer; missing {@link WisdomType} pools are filled with {@link MindPool#of} by the ctor.
 *
 * <p>⚠ Brilliance lives here rather than in its own attachment because it IS the regen rate of these
 * pools; kept apart, the rate and the pools it drives could drift. ⚠ The compact constructor rescales
 * {@code taggedThoughts} down to {@code current(THOUGHTS)} when the tags oversum, so a write that lowers
 * current thoughts must also lower the tag totals or the ctor silently will.
 *
 * @author Alex
 * @version 1.0.0
 * @see MindPool
 * @see MindService
 * @since 1.0.0
 */

public record MindData(Brilliance brilliance, Map<WisdomType, MindPool> pools, Map<ThoughtTag, Long> taggedThoughts) {

    public static final MindData DEFAULT = new MindData(Brilliance.ORDINARY, Map.of(), Map.of());
    public static final Codec<MindData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Brilliance.CODEC.optionalFieldOf("brilliance", Brilliance.ORDINARY).forGetter(MindData::brilliance),
            Codec.unboundedMap(WisdomType.CODEC, MindPool.CODEC)
                    .optionalFieldOf("pools", Map.of()).forGetter(MindData::pools),
            Codec.unboundedMap(ThoughtTag.CODEC, Codec.LONG)
                    .optionalFieldOf("tagged_thoughts", Map.of()).forGetter(MindData::taggedThoughts)
    ).apply(instance, MindData::new));
    public static final StreamCodec<ByteBuf, MindData> STREAM_CODEC = StreamCodec.composite(
            ModStreamCodecs.ofEnum(Brilliance.class), MindData::brilliance,
            ModStreamCodecs.enumMap(WisdomType.class, MindPool.STREAM_CODEC), MindData::pools,
            ModStreamCodecs.enumMap(ThoughtTag.class, ByteBufCodecs.VAR_LONG), MindData::taggedThoughts,
            MindData::new);

    public MindData {
        Map<WisdomType, MindPool> dense = new EnumMap<>(WisdomType.class);
        for (WisdomType type : WisdomType.values()) {
            dense.put(type, pools.getOrDefault(type, MindPool.of(type)));
        }
        pools = Collections.unmodifiableMap(dense);

        Map<ThoughtTag, Long> tagDense = new EnumMap<>(ThoughtTag.class);
        for (Map.Entry<ThoughtTag, Long> entry : taggedThoughts.entrySet()) {
            ThoughtTag tag = entry.getKey();
            Long amount = entry.getValue();
            if (tag != null && tag != ThoughtTag.NATURAL && amount != null && amount > 0L) {
                tagDense.put(tag, amount);
            }
        }
        long thoughtsCurrent = dense.get(WisdomType.THOUGHTS).current();
        BigInteger sum = BigInteger.ZERO;
        for (long amount : tagDense.values()) sum = sum.add(BigInteger.valueOf(amount));
        if (sum.compareTo(BigInteger.valueOf(thoughtsCurrent)) > 0) {
            Map<ThoughtTag, Long> scaled = new EnumMap<>(ThoughtTag.class);
            for (Map.Entry<ThoughtTag, Long> entry : tagDense.entrySet()) {
                long kept = BigInteger.valueOf(entry.getValue()).multiply(BigInteger.valueOf(thoughtsCurrent))
                        .divide(sum).longValue();
                if (kept > 0L) scaled.put(entry.getKey(), kept);
            }
            tagDense = scaled;
        }
        taggedThoughts = tagDense.isEmpty() ? Map.of() : Collections.unmodifiableMap(tagDense);
    }

    public static MindData newborn() { return new MindData(Brilliance.randomBrilliance(), Map.of(), Map.of()); }

    public MindPool pool(WisdomType type) { return pools.get(type); }

    public boolean isOverflowing() { return pools.values().stream().anyMatch(MindPool::isOverflowing); }

    public MindData withBrilliance(Brilliance v) { return new MindData(v, pools, taggedThoughts); }

    public MindData with(WisdomType type, MindPool pool) {
        Map<WisdomType, MindPool> next = new EnumMap<>(WisdomType.class);
        next.putAll(pools);
        next.put(type, pool);
        return new MindData(brilliance, next, taggedThoughts);
    }

    public MindData withTagged(ThoughtTag tag, long amount) {
        Map<ThoughtTag, Long> next = new EnumMap<>(ThoughtTag.class);
        next.putAll(taggedThoughts);
        if (amount <= 0L) next.remove(tag);
        else next.put(tag, amount);
        return new MindData(brilliance, pools, next);
    }

    public MindData emptied() {
        Map<WisdomType, MindPool> next = new EnumMap<>(WisdomType.class);
        pools.forEach((type, pool) -> next.put(type, pool.emptied()));
        return new MindData(brilliance, next, Map.of());
    }
}
