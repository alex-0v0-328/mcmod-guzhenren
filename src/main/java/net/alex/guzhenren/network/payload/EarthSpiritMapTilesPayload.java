package net.alex.guzhenren.network.payload;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.map.LandMaps;
import net.alex.guzhenren.gameplay.blissful.map.MapTile;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 下发给地图订阅者的地图片载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册，{@code EarthSpiritMapNotices}
 * 发送）：至多 {@value LandMaps#TILES_PER_PACKET} 片 {@link MapTile}，超出的解码即拒（坏包）。
 */

public record EarthSpiritMapTilesPayload(List<MapTile> tiles) implements CustomPacketPayload {

    public static final Type<EarthSpiritMapTilesPayload> TYPE = new Type<>(Guzhenren.id("earth_spirit_map_tiles"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EarthSpiritMapTilesPayload> STREAM_CODEC =
            MapTile.STREAM_CODEC.apply(ByteBufCodecs.list(LandMaps.TILES_PER_PACKET))
                    .map(EarthSpiritMapTilesPayload::new, EarthSpiritMapTilesPayload::tiles).cast();

    @Override
    public @NotNull Type<EarthSpiritMapTilesPayload> type() { return TYPE; }
}
