package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.PanelSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 下发给开着地灵经营面板的主人的刷新通知载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册，
 * {@code EarthSpiritNotices} 发送）：一份完整的 {@link PanelSnapshot}，只供显示。
 */

public record EarthSpiritPanelPayload(PanelSnapshot snapshot) implements CustomPacketPayload {

    public static final Type<EarthSpiritPanelPayload> TYPE = new Type<>(Guzhenren.id("earth_spirit_panel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EarthSpiritPanelPayload> STREAM_CODEC =
            PanelSnapshot.STREAM_CODEC.map(EarthSpiritPanelPayload::new, EarthSpiritPanelPayload::snapshot);

    @Override
    public @NotNull Type<EarthSpiritPanelPayload> type() { return TYPE; }
}
