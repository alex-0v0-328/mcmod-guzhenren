package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 空窍温养[Nourish]状态附件（键 {@code nourish_data}）：记录是否正在温养、目标空窍下标与断供起始时刻。
 * <ul>
 *   <li>运行时仅由 {@link ApertureNourishService} 写入；温养进度与石化锁存于各 {@link Aperture} 条目中，
 *       {@link #stopped} 停止温养并清除断供计时但保留目标下标。</li>
 *   <li>同步给所有追踪该玩家的客户端，使周围玩家也能看到温养者的盘腿打坐动作。</li>
 *   <li>⚠ {@code starvedSinceTick} 未断供时必须为 {@link #NOT_STARVED}（{@code -1}）：{@code 0} 是新世界合法的游戏时
 *       刻，用作默认值会被误判为开局即断供。</li>
 *   <li>⚠ {@code target} 读取时必须由服务按当前空窍数量夹取（{@link ApertureNourishService#getTargetIndex}），不能直
 *       接采信原始字段值。</li>
 * </ul>
 */

public record ApertureNourishData(boolean cultivating, int target, long starvedSinceTick) {

    public static final int FULL = 100;
    public static final long NOT_STARVED = -1L;
    public static final long STARVE_GRACE_TICKS = 10L * Ticks.SECOND;
    public static final ApertureNourishData DEFAULT = new ApertureNourishData(false, ApertureData.PRIMARY, NOT_STARVED);
    public static final Codec<ApertureNourishData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("cultivating", false).forGetter(ApertureNourishData::cultivating),
            Codec.INT.optionalFieldOf("target", ApertureData.PRIMARY).forGetter(ApertureNourishData::target),
            Codec.LONG.optionalFieldOf("starved_since_tick", NOT_STARVED)
                    .forGetter(ApertureNourishData::starvedSinceTick)
    ).apply(instance, ApertureNourishData::new));

    public static @NotNull ApertureNourishData decode(@NotNull FriendlyByteBuf buf) {
        return new ApertureNourishData(buf.readBoolean(), buf.readVarInt(), buf.readVarLong());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull ApertureNourishData value) {
        buf.writeBoolean(value.cultivating());
        buf.writeVarInt(value.target());
        buf.writeVarLong(value.starvedSinceTick());
    }

    public boolean isStarving() { return starvedSinceTick != NOT_STARVED; }

    public boolean hasStarvedOut(long now) { return isStarving() && now - starvedSinceTick >= STARVE_GRACE_TICKS; }

    public ApertureNourishData stopped() { return new ApertureNourishData(false, target, NOT_STARVED); }

    public ApertureNourishData withCultivating(boolean value) {
        return new ApertureNourishData(value, target, starvedSinceTick);
    }

    public ApertureNourishData withTarget(int value) {
        return new ApertureNourishData(cultivating, value, starvedSinceTick);
    }

    public ApertureNourishData withStarvedSinceTick(long value) {
        return new ApertureNourishData(cultivating, target, value);
    }
}
