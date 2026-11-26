package net.alex.guzhenren.gameplay.blissful.map;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 福地地图上的一个活物标记[Map Mark]：方块坐标加种类，玩家另带 UUID 供客户端画头像。
 * <ul>
 *   <li>{@code kind}：{@value #PLAYER} 玩家、{@value #PENNED} 栏内动物、{@value #LOOSE} 栏外动物、
 *       {@value #WILD_GU} 野生蛊、{@value #SPIRIT} 地灵；只有玩家带 {@code player}。</li>
 *   <li>{@link #STREAM_CODEC}：种类 1 字节、x 与 z 各 varint、y 2 字节、可选 UUID。</li>
 * </ul>
 */

public record MapMark(byte kind, int x, short y, int z, Optional<UUID> player) {

    public static final byte PLAYER = 0;
    public static final byte PENNED = 1;
    public static final byte LOOSE = 2;
    public static final byte WILD_GU = 3;
    public static final byte SPIRIT = 4;
    public static final StreamCodec<ByteBuf, MapMark> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, MapMark::kind,
            ByteBufCodecs.VAR_INT, MapMark::x,
            ByteBufCodecs.SHORT, MapMark::y,
            ByteBufCodecs.VAR_INT, MapMark::z,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), MapMark::player,
            MapMark::new);
}
