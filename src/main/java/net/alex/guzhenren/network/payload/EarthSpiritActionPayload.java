package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelActions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵经营面板上行的操作意图载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册）：一个类型化的
 * {@link PanelAction}，编码见 {@link PanelActions}。
 */

public record EarthSpiritActionPayload(PanelAction action) implements CustomPacketPayload {

    public static final Type<EarthSpiritActionPayload> TYPE = new Type<>(Guzhenren.id("earth_spirit_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EarthSpiritActionPayload> STREAM_CODEC =
            PanelActions.STREAM_CODEC.map(EarthSpiritActionPayload::new, EarthSpiritActionPayload::action);

    @Override
    public @NotNull Type<EarthSpiritActionPayload> type() { return TYPE; }
}
