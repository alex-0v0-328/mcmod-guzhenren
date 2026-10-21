package net.alex.guzhenren.gameplay.data;

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
    APERTURE {
        @Override public Object get(PlayerData data) { return data.aperture; }
        @Override public void write(FriendlyByteBuf buf, Object value) { ApertureData.encode(buf, (ApertureData) value); }
        @Override public Object read(FriendlyByteBuf buf) { return ApertureData.decode(buf); }
        @Override public void set(PlayerData data, Object value) { data.aperture = (ApertureData) value; }
    },
    NOURISH {
        @Override public Object get(PlayerData data) { return data.nourish; }
        @Override public void write(FriendlyByteBuf buf, Object value) {
            ApertureNourishData.encode(buf, (ApertureNourishData) value);
        }
        @Override public Object read(FriendlyByteBuf buf) { return ApertureNourishData.decode(buf); }
        @Override public void set(PlayerData data, Object value) { data.nourish = (ApertureNourishData) value; }
    },
    BODY {
        @Override public Object get(PlayerData data) { return data.body; }
        @Override public void write(FriendlyByteBuf buf, Object value) { BodyData.encode(buf, (BodyData) value); }
        @Override public Object read(FriendlyByteBuf buf) { return BodyData.decode(buf); }
        @Override public void set(PlayerData data, Object value) { data.body = (BodyData) value; }
    },
    SOUL {
        @Override public Object get(PlayerData data) { return data.soul; }
        @Override public void write(FriendlyByteBuf buf, Object value) { SoulData.encode(buf, (SoulData) value); }
        @Override public Object read(FriendlyByteBuf buf) { return SoulData.decode(buf); }
        @Override public void set(PlayerData data, Object value) { data.soul = (SoulData) value; }
    },
    PATH {
        @Override public Object get(PlayerData data) { return data.path; }
        @Override public void write(FriendlyByteBuf buf, Object value) { PathData.encode(buf, (PathData) value); }
        @Override public Object read(FriendlyByteBuf buf) { return PathData.decode(buf); }
        @Override public void set(PlayerData data, Object value) { data.path = (PathData) value; }
    },
    STRENGTH {
        @Override public Object get(PlayerData data) { return data.strength; }
        @Override public void write(FriendlyByteBuf buf, Object value) {
            PathStrengthData.encode(buf, (PathStrengthData) value);
        }
        @Override public Object read(FriendlyByteBuf buf) { return PathStrengthData.decode(buf); }
        @Override public void set(PlayerData data, Object value) { data.strength = (PathStrengthData) value; }
    },
    QI {
        @Override public Object get(PlayerData data) { return data.qi; }
        @Override public void write(FriendlyByteBuf buf, Object value) { PathQiData.encode(buf, (PathQiData) value); }
        @Override public Object read(FriendlyByteBuf buf) { return PathQiData.decode(buf); }
        @Override public void set(PlayerData data, Object value) { data.qi = (PathQiData) value; }
    },
    MIND {
        @Override public Object get(PlayerData data) { return data.mind; }
        @Override public void write(FriendlyByteBuf buf, Object value) { MindData.encode(buf, (MindData) value); }
        @Override public Object read(FriendlyByteBuf buf) { return MindData.decode(buf); }
        @Override public void set(PlayerData data, Object value) { data.mind = (MindData) value; }
    },
    BURST {
        @Override public Object get(PlayerData data) { return data.burst; }
        @Override public void write(FriendlyByteBuf buf, Object value) { buf.writeBoolean((Boolean) value); }
        @Override public Object read(FriendlyByteBuf buf) { return buf.readBoolean(); }
        @Override public void set(PlayerData data, Object value) { data.burst = (Boolean) value; }
    };

    public abstract Object get(PlayerData data);

    public abstract void write(FriendlyByteBuf buf, Object value);

    public abstract Object read(FriendlyByteBuf buf);

    public abstract void set(PlayerData data, Object value);
}
