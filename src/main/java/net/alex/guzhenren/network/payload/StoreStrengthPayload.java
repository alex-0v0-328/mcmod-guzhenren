package net.alex.guzhenren.network.payload;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.network.ClientIntents;
import net.alex.guzhenren.network.ClientIntents.ChargeAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * The client-intent payload for the Store Strength Gu's [蓄力蛊] charge [蓄力]: its start, its cancel, or its release
 * at the id of the target the client had locked on [锁定]. {@link ClientIntents#storeStrength} hands it to
 * {@code StoreStrength}, which checks the buff, the branch, the fist, the charge and the target again.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.path.strength.StoreStrength
 * @since 1.0.0
 */

public record StoreStrengthPayload(ChargeAction action, int target) implements CustomPacketPayload {

    public static final int NO_TARGET = -1;
    public static final Type<StoreStrengthPayload> TYPE = new Type<>(Guzhenren.id("store_strength"));
    public static final StreamCodec<FriendlyByteBuf, StoreStrengthPayload> STREAM_CODEC = StreamCodec.composite(
            ModByteBufs.enumCodec(ChargeAction.class), StoreStrengthPayload::action,
            ByteBufCodecs.VAR_INT, StoreStrengthPayload::target,
            StoreStrengthPayload::new);

    @Override
    public @NotNull Type<StoreStrengthPayload> type() { return TYPE; }
}
