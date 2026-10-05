package net.alex.guzhenren.network.payload;

import io.netty.buffer.ByteBuf;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for one dash.
 *
 * <p>Sent by the client while a matching effect is running; carries the vertical and horizontal
 * directions plus the facing they were taken at. {@link #canDash} is the one dash rule, read by both
 * ends: the client checks it before sending, the server again before moving the player. No Mortal Gu
 * [凡蛊] may be in the main hand, and each axis moved needs its Crash Gu effect -- the charging one
 * covers both.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.effect.timed.CrashGuEffect
 * @since 1.0.0
 */

public record DashPayload(int vertical, int horizontal, float yRot) implements CustomPacketPayload {

    public static final Type<DashPayload> TYPE = new Type<>(Guzhenren.id("dash"));
    public static final StreamCodec<ByteBuf, DashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DashPayload::vertical,
            ByteBufCodecs.INT, DashPayload::horizontal,
            ByteBufCodecs.FLOAT, DashPayload::yRot,
            DashPayload::new);

    public static boolean canDash(ItemStack mainHand) {
        return !(mainHand.getItem() instanceof MortalGuItem);
    }

    public static boolean canDash(ItemStack mainHand, int vertical, int horizontal,
                                  boolean horizontalCrash, boolean verticalCrash, boolean chargingCrash) {
        if (!canDash(mainHand) || vertical == 0 && horizontal == 0) return false;
        return (horizontal == 0 || horizontalCrash || chargingCrash)
                && (vertical == 0 || verticalCrash || chargingCrash);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
