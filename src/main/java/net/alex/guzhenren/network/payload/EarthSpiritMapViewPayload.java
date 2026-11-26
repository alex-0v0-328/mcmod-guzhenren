package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵面板地图页上行的视口订阅载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册）：一个 {@link MapView}，
 * 编码见 {@link MapView#STREAM_CODEC}；空矩形（{@code min > max}）即退订，服务端复判见 {@code LandMaps#view}。
 */

public record EarthSpiritMapViewPayload(@NotNull MapView view) implements CustomPacketPayload {

    public static final Type<EarthSpiritMapViewPayload> TYPE = new Type<>(Guzhenren.id("earth_spirit_map_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EarthSpiritMapViewPayload> STREAM_CODEC =
            MapView.STREAM_CODEC.map(EarthSpiritMapViewPayload::new, EarthSpiritMapViewPayload::view).cast();

    @Override
    public @NotNull Type<EarthSpiritMapViewPayload> type() { return TYPE; }
}
