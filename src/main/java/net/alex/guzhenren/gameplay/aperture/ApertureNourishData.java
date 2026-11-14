package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 温养[Nourish]状态附件：是否在温养、温养哪个空窍[Aperture]。
 *
 * <ul>
 *   <li>键 {@code nourish_data}，运行时仅 {@code ApertureNourishService} 写；进度与石化锁存于 {@link Aperture}。</li>
 *   <li>同步给所有看得见该玩家的客户端：温养者在他人屏幕上也盘腿而坐。</li>
 *   <li>⚠ {@code starvedSinceTick} 默认 {@code NOT_STARVED}（-1）：{@code 0} 是真实时刻，新世界会读成开局即断供。</li>
 *   <li>⚠ {@code target} 须经服务按现有空窍夹取才可信，不直接信网络值。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Aperture
 * @see ApertureNourishService
 * @since 1.0.0
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

    /** 停止温养[Nourish]并清除断供，保留目标。 */
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
