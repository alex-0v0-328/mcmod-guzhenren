package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.network.ClientIntents;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for one radial-menu pick of the Amplify Effect Gu [功倍蛊]: the hotbar slot of the Gu to
 * drive. {@link ClientIntents#amplifyDrive} checks it all again; the client's menu only lists what it would pass.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem
 * @since 1.0.0
 */

public record AmplifyDrivePayload(int slot) {

    public static @NotNull AmplifyDrivePayload decode(@NotNull FriendlyByteBuf buf) {
        return new AmplifyDrivePayload(buf.readVarInt());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull AmplifyDrivePayload value) {
        buf.writeVarInt(value.slot());
    }
}
