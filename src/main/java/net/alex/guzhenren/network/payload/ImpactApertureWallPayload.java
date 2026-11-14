package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 冲刷窍壁的客户端意图：零字节单例，只代表一次按键；结果全凭掷骰。
 * <ul>
 *   <li>由 {@link net.alex.guzhenren.network.ModPayloads} 处理，转交 {@link ApertureNourishService}。</li>
 *   <li>不上传玩家数据；附件同步唯独不承载客户端意图。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record ImpactApertureWallPayload() implements CustomPacketPayload {

    public static final ImpactApertureWallPayload INSTANCE = new ImpactApertureWallPayload();
    public static final Type<ImpactApertureWallPayload> TYPE = new Type<>(Guzhenren.id("impact_aperture_wall"));
    public static final StreamCodec<FriendlyByteBuf, ImpactApertureWallPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<ImpactApertureWallPayload> type() { return TYPE; }
}
