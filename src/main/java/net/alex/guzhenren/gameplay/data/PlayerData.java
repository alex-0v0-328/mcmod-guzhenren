package net.alex.guzhenren.gameplay.data;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.alex.guzhenren.core.NbtCodecs;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.path.PathData;
import net.alex.guzhenren.gameplay.path.qi.PathQiData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.network.ModNetwork;
import net.alex.guzhenren.network.packet.PlayerDataSyncPacket;
import net.alex.guzhenren.registry.capability.ModCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Everything this mod keeps on a player, as one capability: twelve saved values, two in-memory carries.
 *
 * <p>{@link #get} is the single entry point on both sides; the client copy is filled by {@link
 * PlayerDataSyncPacket}. Each setter stores the value and returns whether it changed; a change on a
 * {@link ServerPlayer} sends that one value to its owner, so nine values ({@link PlayerDataField}) are
 * always current on the owner's client and nobody else's. {@link #copyFrom} carries values onto the
 * fresh player of a death or an End return; {@link #save} / {@link #load} write the twelve saved values
 * through each record's {@link Codec}.
 *
 * <p>⚠ What a clone carries: a death carries every saved value except {@code burst}, which falls back to
 * {@code false} (a death ends a burst); an End return carries all twelve saved values. The two carries
 * ({@code essenceCarry}, {@code healthCarry}) are never saved and never carried.
 *
 * <p>⚠ {@link #copyFrom} and {@link #load} assign fields directly and send nothing: during a respawn the
 * client still holds the old player entity, so the whole synced set goes out after the respawn instead
 * ({@code PlayerDataEvents}).
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class PlayerData {

    /** The ten record values {@link #save} writes, in tag order; the two flags follow as plain booleans. */
    private static final List<Saved<?>> SAVED = List.of(
            new Saved<>("aperture_data", ApertureData.CODEC, ApertureData.DEFAULT,
                    data -> data.aperture, (data, value) -> data.aperture = value),
            new Saved<>("aperture_storage", ApertureStorage.CODEC, ApertureStorage.DEFAULT,
                    data -> data.apertureStorage, (data, value) -> data.apertureStorage = value),
            new Saved<>("nourish_data", ApertureNourishData.CODEC, ApertureNourishData.DEFAULT,
                    data -> data.nourish, (data, value) -> data.nourish = value),
            new Saved<>("body_data", BodyData.CODEC, BodyData.DEFAULT,
                    data -> data.body, (data, value) -> data.body = value),
            new Saved<>("soul_data", SoulData.CODEC, SoulData.DEFAULT,
                    data -> data.soul, (data, value) -> data.soul = value),
            new Saved<>("path_data", PathData.CODEC, PathData.DEFAULT,
                    data -> data.path, (data, value) -> data.path = value),
            new Saved<>("strength_data", PathStrengthData.CODEC, PathStrengthData.DEFAULT,
                    data -> data.strength, (data, value) -> data.strength = value),
            new Saved<>("qi_data", PathQiData.CODEC, PathQiData.DEFAULT,
                    data -> data.qi, (data, value) -> data.qi = value),
            new Saved<>("mind_data", MindData.CODEC, MindData.DEFAULT,
                    data -> data.mind, (data, value) -> data.mind = value),
            new Saved<>("dimension_return", DimensionReturnData.CODEC, DimensionReturnData.DEFAULT,
                    data -> data.dimensionReturn, (data, value) -> data.dimensionReturn = value));

    private final Player owner;

    ApertureData aperture = ApertureData.DEFAULT;
    private final float[] essenceCarry = new float[ApertureData.MAX_APERTURES];
    private ApertureStorage apertureStorage = ApertureStorage.DEFAULT;
    ApertureNourishData nourish = ApertureNourishData.DEFAULT;
    BodyData body = BodyData.DEFAULT;
    SoulData soul = SoulData.DEFAULT;
    PathData path = PathData.DEFAULT;
    PathStrengthData strength = PathStrengthData.DEFAULT;
    PathQiData qi = PathQiData.DEFAULT;
    MindData mind = MindData.DEFAULT;
    private DimensionReturnData dimensionReturn = DimensionReturnData.DEFAULT;
    boolean burst = false;
    private boolean born = false;
    private @Nullable Float healthCarry;

    public PlayerData(@NotNull Player owner) { this.owner = owner; }

    public static @NotNull PlayerData get(@NotNull Player player) {
        return player.getCapability(ModCapabilities.PLAYER_DATA)
                .orElseThrow(() -> new IllegalStateException("No player data on " + player.getScoreboardName()));
    }

    public ApertureData getAperture() { return aperture; }

    public float[] getEssenceCarry() { return essenceCarry; }

    public ApertureStorage getApertureStorage() { return apertureStorage; }

    public ApertureNourishData getNourish() { return nourish; }

    public BodyData getBody() { return body; }

    public SoulData getSoul() { return soul; }

    public PathData getPath() { return path; }

    public PathStrengthData getStrength() { return strength; }

    public PathQiData getQi() { return qi; }

    public MindData getMind() { return mind; }

    public DimensionReturnData getDimensionReturn() { return dimensionReturn; }

    public boolean hasBurst() { return burst; }

    public boolean isBorn() { return born; }


    public boolean setAperture(@NotNull ApertureData value) { return change(PlayerDataField.APERTURE, value); }

    public boolean setApertureStorage(@NotNull ApertureStorage value) {
        if (value.equals(apertureStorage)) return false;
        apertureStorage = value;
        return true;
    }

    public boolean setNourish(@NotNull ApertureNourishData value) { return change(PlayerDataField.NOURISH, value); }

    public boolean setBody(@NotNull BodyData value) { return change(PlayerDataField.BODY, value); }

    public boolean setSoul(@NotNull SoulData value) { return change(PlayerDataField.SOUL, value); }

    public boolean setPath(@NotNull PathData value) { return change(PlayerDataField.PATH, value); }

    public boolean setStrength(@NotNull PathStrengthData value) { return change(PlayerDataField.STRENGTH, value); }

    public boolean setQi(@NotNull PathQiData value) { return change(PlayerDataField.QI, value); }

    public boolean setMind(@NotNull MindData value) { return change(PlayerDataField.MIND, value); }

    public boolean setDimensionReturn(@NotNull DimensionReturnData value) {
        if (value.equals(dimensionReturn)) return false;
        dimensionReturn = value;
        return true;
    }

    public boolean setBurst(boolean value) { return change(PlayerDataField.BURST, value); }

    public boolean setBorn(boolean value) {
        if (value == born) return false;
        born = value;
        return true;
    }

    public void setHealthCarry(float value) { healthCarry = value; }

    /** The carried health, cleared; {@code null} when nothing was carried. */
    public @Nullable Float takeHealthCarry() {
        Float carried = healthCarry;
        healthCarry = null;
        return carried;
    }

    public void resetEssenceCarry() { Arrays.fill(essenceCarry, 0.0F); }

    /** Every value a death carries back to its default, synced; the reset of a death without keepInventory. */
    public void resetCarriedOnDeath() {
        setAperture(ApertureData.DEFAULT);
        setApertureStorage(ApertureStorage.DEFAULT);
        setNourish(ApertureNourishData.DEFAULT);
        setBody(BodyData.DEFAULT);
        setSoul(SoulData.DEFAULT);
        setPath(PathData.DEFAULT);
        setStrength(PathStrengthData.DEFAULT);
        setQi(PathQiData.DEFAULT);
        setMind(MindData.DEFAULT);
        setDimensionReturn(DimensionReturnData.DEFAULT);
        setBorn(false);
    }

    public void copyFrom(@NotNull PlayerData from, boolean wasDeath) {
        aperture = from.aperture;
        apertureStorage = from.apertureStorage;
        nourish = from.nourish;
        body = from.body;
        soul = from.soul;
        path = from.path;
        strength = from.strength;
        qi = from.qi;
        mind = from.mind;
        dimensionReturn = from.dimensionReturn;
        born = from.born;
        burst = !wasDeath && from.burst;
        Arrays.fill(essenceCarry, 0.0F);
        healthCarry = null;
    }

    /** Sends every synced value to the owner; after login, respawn and dimension change. */
    public void syncAll() {
        for (PlayerDataField field : PlayerDataField.values()) sync(field);
    }

    /** Stores {@code value} into {@code field} and sends it to the owner; {@code false} when it was already held. */
    private boolean change(PlayerDataField field, Object value) {
        if (value.equals(field.get(this))) return false;
        field.set(this, value);
        sync(field);
        return true;
    }

    private void sync(PlayerDataField field) {
        if (owner instanceof ServerPlayer player && player.connection != null) {
            ModNetwork.sendToPlayer(player, PlayerDataSyncPacket.of(field, this));
        }
    }

    public @NotNull CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Saved<?> saved : SAVED) saved.save(this, tag);
        tag.putBoolean("burst_flag", burst);
        tag.putBoolean("born_flag", born);
        return tag;
    }

    public void load(@NotNull CompoundTag tag) {
        for (Saved<?> saved : SAVED) saved.load(this, tag);
        burst = tag.getBoolean("burst_flag");
        born = tag.getBoolean("born_flag");
    }

    /** One saved record value: its tag key, codec, fallback when missing or unreadable, and field. */
    private record Saved<T>(String key, Codec<T> codec, T fallback,
                            Function<PlayerData, T> get, BiConsumer<PlayerData, T> set) {

        void save(PlayerData data, CompoundTag tag) {
            NbtCodecs.encode(codec, get.apply(data), "save player data", key)
                    .ifPresent(encoded -> tag.put(key, encoded));
        }

        void load(PlayerData data, CompoundTag tag) {
            Tag encoded = tag.get(key);
            set.accept(data, encoded == null ? fallback
                    : NbtCodecs.decode(codec, encoded, "load player data", key).orElse(fallback));
        }
    }
}
