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
 * 服务端通知：某玩家刚换上皮肤，看得到该玩家的客户端按皮肤色调画闪光。
 * 下行：皮肤经附件同步只到本人，闪光要的是换上那一刻；旁观者从不收他人肉体数据；只带实体 id 与皮肤，无可据以行动之物。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.ModPayloads
 * @since 1.0.0
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
