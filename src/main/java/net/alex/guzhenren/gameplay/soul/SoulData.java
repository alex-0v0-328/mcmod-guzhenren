package net.alex.guzhenren.gameplay.soul;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家魂魄状态附件（键 {@code soul_data}）：唯一触底（{@link #isCollapsed}）即致死的资源池。
 * <ul>
 *   <li>运行时仅由 {@link SoulService} 写入；紧凑构造器将 {@code maxSoul} 钳至 {@code >= 0}、{@code currentSoul} 钳
 *       至 {@code [0, maxSoul]}。</li>
 *   <li>⚠ 上限直接落盘存储而非从其他系统推导：魂魄独立于肉身与修为（对比 Epic Fight 耐力由战斗系统决定上
 *       限）。</li>
 *   <li>⚠ {@link #withRevived()} 将当前魂魄置为 {@link #REVIVED_SOUL}（1）而非 0，且上限为 0 时恢复至
 *       {@link #DEFAULT_MAX_SOUL}，防止玩家重生后立即再次触发魂飞魄散致死。</li>
 * </ul>
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
