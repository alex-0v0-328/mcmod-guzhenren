package net.alex.guzhenren.gameplay.soul;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 魂魄：唯一触底即致死的池，record 附件 {@code soul_data}。
 * <ul>
 *   <li>{@code SoulService} 唯一运行时写入；构造器钳 {@code maxSoul} ≥ 0、{@code currentSoul} ∈ {@code [0, max]}。</li>
 *   <li>⚠ 上限存储而非推导：无他物决定它（对比 Epic Fight 耐力，其上限归战斗系统）。</li>
 *   <li>⚠ {@code withRevived()} 回 1 而非 0，上限 0 时恢复 {@code DEFAULT_MAX_SOUL}：重生不得交回致死值。</li>
 * </ul>
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

    public SoulData {
        maxSoul = Math.max(0L, maxSoul);
        currentSoul = Math.clamp(currentSoul, 0L, maxSoul);
    }

    public static @NotNull SoulData decode(@NotNull FriendlyByteBuf buf) {
        return new SoulData(buf.readVarLong(), buf.readVarLong());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull SoulData value) {
        buf.writeVarLong(value.maxSoul());
        buf.writeVarLong(value.currentSoul());
    }

    public SoulTier getTier() { return SoulTier.fromSoul(maxSoul); }

    public boolean isCollapsed() { return currentSoul <= 0L; }

    public SoulData withMaxSoul(long value) { return new SoulData(value, currentSoul); }

    public SoulData withCurrentSoul(long value) { return new SoulData(maxSoul, value); }

    public SoulData withFullSoul() { return new SoulData(maxSoul, maxSoul); }

    public SoulData withRevived() {
        return new SoulData(maxSoul > 0L ? maxSoul : DEFAULT_MAX_SOUL, REVIVED_SOUL);
    }
}
