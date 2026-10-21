package net.alex.guzhenren.network.packet;

import java.util.function.Supplier;
import net.alex.guzhenren.client.network.ClientPacketHandlers;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.data.PlayerDataField;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Server to owner: one {@link PlayerData} value, so the owner's client copy matches the server.
 *
 * <p>Carries the field's ordinal and its value; an out-of-range ordinal fails decoding as a malformed
 * packet ({@link ModByteBufs#readEnum}).
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public record PlayerDataSyncPacket(PlayerDataField field, Object value) {

    public static @NotNull PlayerDataSyncPacket of(@NotNull PlayerDataField field, @NotNull PlayerData data) {
        return new PlayerDataSyncPacket(field, field.get(data));
    }

    public static void encode(PlayerDataSyncPacket packet, FriendlyByteBuf buf) {
        ModByteBufs.writeEnum(buf, packet.field);
        packet.field.write(buf, packet.value);
    }

    public static PlayerDataSyncPacket decode(FriendlyByteBuf buf) {
        PlayerDataField field = ModByteBufs.readEnum(buf, PlayerDataField.class);
        return new PlayerDataSyncPacket(field, field.read(buf));
    }

    public static void handle(PlayerDataSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.applyPlayerData(packet));
    }
}
