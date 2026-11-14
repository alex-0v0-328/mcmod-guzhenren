package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 打开炼蛊界面的客户端意图：零字节单例，只代表一次按键。
 * <ul>
 *   <li>由 {@link net.alex.guzhenren.network.ModPayloads} 处理，打开 {@link RefinementMenu}。</li>
 *   <li>不上传玩家数据；附件同步唯独不承载客户端意图。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
 */

public record OpenRefinementPayload() implements CustomPacketPayload {

    public static final OpenRefinementPayload INSTANCE = new OpenRefinementPayload();
    public static final Type<OpenRefinementPayload> TYPE = new Type<>(Guzhenren.id("open_refinement"));
    public static final StreamCodec<FriendlyByteBuf, OpenRefinementPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<OpenRefinementPayload> type() { return TYPE; }
}
