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
 * Server notice: a player just put on a skin [皮肤], so every client that sees them draws the flash in that skin's tint.
 *
 * <p>The one downstream record: the skin itself reaches the owner through attachment sync, but the
 * moment it was put on is what the flash needs, and watching players never receive another player's body
 * data. Carries the entity id and the skin, nothing a client could act on.
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
