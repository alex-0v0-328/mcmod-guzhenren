package net.alex.guzhenren.network.payload;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.map.MapMark;
import net.alex.guzhenren.gameplay.blissful.map.MapMarks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 下发给地图订阅者的活物标记载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册，{@code EarthSpiritMapNotices}
 * 发送）：每秒一份完整的标记表，至多 {@value MapMarks#LIMIT} 个 {@link MapMark}，替换客户端上一份。
 */

public record EarthSpiritMapMarksPayload(List<MapMark> marks) implements CustomPacketPayload {

    public static final Type<EarthSpiritMapMarksPayload> TYPE = new Type<>(Guzhenren.id("earth_spirit_map_marks"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EarthSpiritMapMarksPayload> STREAM_CODEC =
            MapMark.STREAM_CODEC.apply(ByteBufCodecs.list(MapMarks.LIMIT))
                    .map(EarthSpiritMapMarksPayload::new, EarthSpiritMapMarksPayload::marks).cast();

    @Override
    public @NotNull Type<EarthSpiritMapMarksPayload> type() { return TYPE; }
}
