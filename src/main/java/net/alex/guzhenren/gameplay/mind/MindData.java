package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.math.BigInteger;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 脑海：才情及其驱动的念、意、情三池。
 *
 * <ul>
 *   <li>附件 {@code mind_data}，运行时只由 {@code MindService} 写，{@code PlayerDataService} 整体重置；构造器用
 *       {@link MindPool#of} 补齐缺的 {@link MindPoolType} 池。</li>
 *   <li>{@link #isOverflowing} 只问可超限的池，过不了上限的池不会炸裂脑海。</li>
 *   <li>⚠ 才情不单独成附件：它就是池的回复速度，分存会漂移。</li>
 *   <li>⚠ 标签合计超过 {@code current(THOUGHTS)} 时，紧凑构造器把 {@code taggedThoughts} 按比例缩到当前念；
 *       降当前念须同时降标签合计，否则构造器悄悄代劳。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see MindPool
 * @see MindService
 * @since 1.0.0
 */

public record MindData(Brilliance brilliance, Map<MindPoolType, MindPool> pools, Map<ThoughtTag, Long> taggedThoughts) {

    public static final MindData DEFAULT = new MindData(Brilliance.ORDINARY, Map.of(), Map.of());
    public static final Codec<MindData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Brilliance.CODEC.optionalFieldOf("brilliance", Brilliance.ORDINARY).forGetter(MindData::brilliance),
            Codec.unboundedMap(MindPoolType.CODEC, MindPool.CODEC)
                    .optionalFieldOf("pools", Map.of()).forGetter(MindData::pools),
            Codec.unboundedMap(ThoughtTag.CODEC, Codec.LONG)
                    .optionalFieldOf("tagged_thoughts", Map.of()).forGetter(MindData::taggedThoughts)
    ).apply(instance, MindData::new));

    public MindData {
        Map<MindPoolType, MindPool> dense = new EnumMap<>(MindPoolType.class);
        for (MindPoolType type : MindPoolType.values()) {
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
        long thoughtsCurrent = dense.get(MindPoolType.THOUGHTS).current();
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

    public static @NotNull MindData decode(@NotNull FriendlyByteBuf buf) {
        return new MindData(
                ModByteBufs.readEnum(buf, Brilliance.class),
                ModByteBufs.readEnumMap(buf, MindPoolType.class, MindPool::decode),
                ModByteBufs.readEnumMap(buf, ThoughtTag.class, FriendlyByteBuf::readVarLong));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull MindData value) {
        ModByteBufs.writeEnum(buf, value.brilliance());
        ModByteBufs.writeEnumMap(buf, value.pools(), MindPool::encode);
        ModByteBufs.writeEnumMap(buf, value.taggedThoughts(), FriendlyByteBuf::writeVarLong);
    }

    public static MindData createNewborn() { return new MindData(Brilliance.randomBrilliance(), Map.of(), Map.of()); }

    public MindPool getPool(MindPoolType type) { return pools.get(type); }

    public boolean isOverflowing() {
        return pools.entrySet().stream()
                .anyMatch(entry -> entry.getKey().isBurstable() && entry.getValue().isOverflowing());
    }

    public MindData withBrilliance(Brilliance brilliance) { return new MindData(brilliance, pools, taggedThoughts); }

    public MindData with(MindPoolType type, MindPool pool) {
        Map<MindPoolType, MindPool> next = new EnumMap<>(MindPoolType.class);
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

    public MindData withEmptied() {
        Map<MindPoolType, MindPool> next = new EnumMap<>(MindPoolType.class);
        pools.forEach((type, pool) -> next.put(type, pool.withEmptied()));
        return new MindData(brilliance, next, Map.of());
    }
}
