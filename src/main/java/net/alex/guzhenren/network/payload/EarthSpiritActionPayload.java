package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.PanelAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵经营面板上行的操作意图载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册）：操作、整数参数、字符串参
 * 数，参数约定见 {@link PanelAction}；字符串至多 {@value #MAX_TEXT} 字符。
 */

public record EarthSpiritActionPayload(PanelAction action, int value, String text) implements CustomPacketPayload {

    public static final int MAX_TEXT = 256;
    public static final Type<EarthSpiritActionPayload> TYPE = new Type<>(Guzhenren.id("earth_spirit_action"));
    public static final StreamCodec<FriendlyByteBuf, EarthSpiritActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(index -> PanelAction.values()[index], PanelAction::ordinal),
            EarthSpiritActionPayload::action,
            ByteBufCodecs.VAR_INT, EarthSpiritActionPayload::value,
            ByteBufCodecs.stringUtf8(MAX_TEXT), EarthSpiritActionPayload::text,
            EarthSpiritActionPayload::new);

    @Override
    public @NotNull Type<EarthSpiritActionPayload> type() { return TYPE; }
}
