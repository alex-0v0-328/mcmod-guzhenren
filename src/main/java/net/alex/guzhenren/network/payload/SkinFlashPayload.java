package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.body.Skin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * 通知可见范围内客户端播放玩家换皮闪光特效的服务端下行载荷（由 {@link net.alex.guzhenren.network.ModPayloads} 注册），
 * 仅携带实体 ID 与 {@link Skin} 类型以弥补他人肉身附件不对旁观者同步及单靠附件无法感知换皮瞬间的限制。
 */

public record SkinFlashPayload(int entityId, Skin skin) implements CustomPacketPayload {

    public static final Type<SkinFlashPayload> TYPE = new Type<>(Guzhenren.id("skin_flash"));
    public static final StreamCodec<FriendlyByteBuf, SkinFlashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SkinFlashPayload::entityId,
            ModByteBufs.enumCodec(Skin.class), SkinFlashPayload::skin,
            SkinFlashPayload::new);

    @Override
    public @NotNull Type<SkinFlashPayload> type() { return TYPE; }
}
