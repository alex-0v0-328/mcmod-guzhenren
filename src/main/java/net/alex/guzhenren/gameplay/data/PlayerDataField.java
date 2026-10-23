package net.alex.guzhenren.gameplay.data;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.path.PathData;
import net.alex.guzhenren.gameplay.path.qi.PathQiData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.minecraft.network.FriendlyByteBuf;

/**
 * The nine {@link PlayerData} values its owner's client sees, each with its wire form.
 *
 * <p>A sync packet snapshots one field's value on the server ({@link #get}), writes it ({@link #write}),
 * reads it back on the client's network thread ({@link #read}) and stores it into the client copy on the
 * main thread ({@link #set}). Every synced value is an immutable record or a boolean, so the snapshot
 * cannot change under the packet.
 *
 * <p>⚠ The ordinal is the wire id: append new fields, never reorder.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum PlayerDataField {
    APERTURE(new Wire<>(data -> data.aperture, (data, value) -> data.aperture = value,
            ApertureData::encode, ApertureData::decode)),
    NOURISH(new Wire<>(data -> data.nourish, (data, value) -> data.nourish = value,
            ApertureNourishData::encode, ApertureNourishData::decode)),
    BODY(new Wire<>(data -> data.body, (data, value) -> data.body = value, BodyData::encode, BodyData::decode)),
    SOUL(new Wire<>(data -> data.soul, (data, value) -> data.soul = value, SoulData::encode, SoulData::decode)),
    PATH(new Wire<>(data -> data.path, (data, value) -> data.path = value, PathData::encode, PathData::decode)),
    STRENGTH(new Wire<>(data -> data.strength, (data, value) -> data.strength = value,
            PathStrengthData::encode, PathStrengthData::decode)),
    QI(new Wire<>(data -> data.qi, (data, value) -> data.qi = value, PathQiData::encode, PathQiData::decode)),
    MIND(new Wire<>(data -> data.mind, (data, value) -> data.mind = value, MindData::encode, MindData::decode)),
    BURST(new Wire<Boolean>(data -> data.burst, (data, value) -> data.burst = value,
            FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean));

    private final Wire<?> wire;

    PlayerDataField(Wire<?> wire) { this.wire = wire; }

    public Object get(PlayerData data) { return wire.get().apply(data); }

    public void write(FriendlyByteBuf buf, Object value) { wire.write(buf, value); }

    public Object read(FriendlyByteBuf buf) { return wire.read().apply(buf); }

    public void set(PlayerData data, Object value) { wire.set(data, value); }

    /** One field's accessors and wire form, typed; the untyped packet value is cast back here only. */
    private record Wire<T>(Function<PlayerData, T> get, BiConsumer<PlayerData, T> store,
                           BiConsumer<FriendlyByteBuf, T> encode, Function<FriendlyByteBuf, T> read) {

        void write(FriendlyByteBuf buf, Object value) { encode.accept(buf, cast(value)); }

        void set(PlayerData data, Object value) { store.accept(data, cast(value)); }

        @SuppressWarnings("unchecked")
        private T cast(Object value) { return (T) value; }
    }
}
