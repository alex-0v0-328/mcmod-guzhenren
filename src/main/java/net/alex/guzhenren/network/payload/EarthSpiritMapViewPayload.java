package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.map.MapView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵面板地图页上行的视口订阅载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册）：区块矩形两角，
 * 四个 varint；空矩形（{@code min > max}）即退订，服务端复判见 {@code LandMaps#view}。
 */

public record EarthSpiritMapViewPayload(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ)
        implements CustomPacketPayload {

    public static final Type<EarthSpiritMapViewPayload> TYPE = new Type<>(Guzhenren.id("earth_spirit_map_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EarthSpiritMapViewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, EarthSpiritMapViewPayload::minChunkX,
                    ByteBufCodecs.VAR_INT, EarthSpiritMapViewPayload::minChunkZ,
                    ByteBufCodecs.VAR_INT, EarthSpiritMapViewPayload::maxChunkX,
                    ByteBufCodecs.VAR_INT, EarthSpiritMapViewPayload::maxChunkZ,
                    EarthSpiritMapViewPayload::new);

    public @NotNull MapView view() { return new MapView(minChunkX, minChunkZ, maxChunkX, maxChunkZ); }

    @Override
    public @NotNull Type<EarthSpiritMapViewPayload> type() { return TYPE; }
}
