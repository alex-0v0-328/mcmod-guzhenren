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
 * 玩家脑海状态附件（键 {@code mind_data}）：承载才情[Brilliance]、念/意/情三池（{@link MindPool}）以及特殊念头标签份额
 * （{@link ThoughtTag}）。
 * <ul>
 *   <li>运行时仅由 {@link MindService} 写入，紧凑构造器自动用 {@link MindPool#of} 补齐缺失的 {@link MindPoolType} 条
 *       目。</li>
 *   <li>{@link #isOverflowing} 仅检查允许超限爆裂的池（{@link MindPoolType#isBurstable}），不可超限的池不会触发脑海炸
 *       裂。</li>
 *   <li>⚠ 才情与三池保存在同一附件中，切勿拆成独立附件以免重置或同步时产生漂移。</li>
 *   <li>⚠ 当特殊念头标签份额总和超过当前念头总量（{@code current(THOUGHTS)}）时，紧凑构造器会将各标签份额按比例缩减至
 *       不超过当前总念；因此业务层下调当前念头时应同步扣减标签份额，避免被构造器按比例静默截断。</li>
 * </ul>
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
