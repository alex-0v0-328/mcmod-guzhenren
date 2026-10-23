package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.network.ClientIntents;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for one dash.
 *
 * <p>Sent by the client while a matching effect is running; carries the vertical and horizontal
 * directions plus the facing they were taken at. {@link ClientIntents#canDash} is the one dash rule, read by both
 * ends: the client checks it before sending, the server again before moving the player. No Mortal Gu
 * [凡蛊] may be in the main hand, and each axis moved needs its Crash Gu effect -- the charging one
 * covers both.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.effect.timed.CrashGuEffect
 * @since 1.0.0
 */

public record DashPayload(int vertical, int horizontal, float yRot) {

    public static @NotNull DashPayload decode(@NotNull FriendlyByteBuf buf) {
        return new DashPayload(buf.readInt(), buf.readInt(), buf.readFloat());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull DashPayload value) {
        buf.writeInt(value.vertical());
        buf.writeInt(value.horizontal());
        buf.writeFloat(value.yRot());
    }
}
