package net.alex.guzhenren.gameplay.data;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import net.alex.guzhenren.Guzhenren;
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
import net.minecraft.nbt.NbtOps;
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

    public ApertureData aperture() { return aperture; }

    public float[] essenceCarry() { return essenceCarry; }

    public ApertureStorage apertureStorage() { return apertureStorage; }

    public ApertureNourishData nourish() { return nourish; }

    public BodyData body() { return body; }

    public SoulData soul() { return soul; }

    public PathData path() { return path; }

    public PathStrengthData strength() { return strength; }

    public PathQiData qi() { return qi; }

    public MindData mind() { return mind; }

    public DimensionReturnData dimensionReturn() { return dimensionReturn; }

    public boolean burst() { return burst; }

    public boolean born() { return born; }


    public boolean setAperture(@NotNull ApertureData value) {
        if (value.equals(aperture)) return false;
        aperture = value;
        sync(PlayerDataField.APERTURE);
        return true;
    }

    public boolean setApertureStorage(@NotNull ApertureStorage value) {
        if (value.equals(apertureStorage)) return false;
        apertureStorage = value;
        return true;
    }

    public boolean setNourish(@NotNull ApertureNourishData value) {
        if (value.equals(nourish)) return false;
        nourish = value;
        sync(PlayerDataField.NOURISH);
        return true;
    }

    public boolean setBody(@NotNull BodyData value) {
        if (value.equals(body)) return false;
        body = value;
        sync(PlayerDataField.BODY);
        return true;
    }

    public boolean setSoul(@NotNull SoulData value) {
        if (value.equals(soul)) return false;
        soul = value;
        sync(PlayerDataField.SOUL);
        return true;
    }

    public boolean setPath(@NotNull PathData value) {
        if (value.equals(path)) return false;
        path = value;
        sync(PlayerDataField.PATH);
        return true;
    }

    public boolean setStrength(@NotNull PathStrengthData value) {
        if (value.equals(strength)) return false;
        strength = value;
        sync(PlayerDataField.STRENGTH);
        return true;
    }

    public boolean setQi(@NotNull PathQiData value) {
        if (value.equals(qi)) return false;
        qi = value;
        sync(PlayerDataField.QI);
        return true;
    }

    public boolean setMind(@NotNull MindData value) {
        if (value.equals(mind)) return false;
        mind = value;
        sync(PlayerDataField.MIND);
        return true;
    }

    public boolean setDimensionReturn(@NotNull DimensionReturnData value) {
        if (value.equals(dimensionReturn)) return false;
        dimensionReturn = value;
        return true;
    }

    public boolean setBurst(boolean value) {
        if (value == burst) return false;
        burst = value;
        sync(PlayerDataField.BURST);
        return true;
    }

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

    private void sync(PlayerDataField field) {
        if (owner instanceof ServerPlayer player && player.connection != null) {
            ModNetwork.sendToPlayer(player, PlayerDataSyncPacket.of(field, this));
        }
    }

    public @NotNull CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        put(tag, "aperture_data", ApertureData.CODEC, aperture);
        put(tag, "aperture_storage", ApertureStorage.CODEC, apertureStorage);
        put(tag, "nourish_data", ApertureNourishData.CODEC, nourish);
        put(tag, "body_data", BodyData.CODEC, body);
        put(tag, "soul_data", SoulData.CODEC, soul);
        put(tag, "path_data", PathData.CODEC, path);
        put(tag, "strength_data", PathStrengthData.CODEC, strength);
        put(tag, "qi_data", PathQiData.CODEC, qi);
        put(tag, "mind_data", MindData.CODEC, mind);
        put(tag, "dimension_return", DimensionReturnData.CODEC, dimensionReturn);
        tag.putBoolean("burst_flag", burst);
        tag.putBoolean("born_flag", born);
        return tag;
    }

    public void load(@NotNull CompoundTag tag) {
        aperture = read(tag, "aperture_data", ApertureData.CODEC, ApertureData.DEFAULT);
        apertureStorage = read(tag, "aperture_storage", ApertureStorage.CODEC, ApertureStorage.DEFAULT);
        nourish = read(tag, "nourish_data", ApertureNourishData.CODEC, ApertureNourishData.DEFAULT);
        body = read(tag, "body_data", BodyData.CODEC, BodyData.DEFAULT);
        soul = read(tag, "soul_data", SoulData.CODEC, SoulData.DEFAULT);
        path = read(tag, "path_data", PathData.CODEC, PathData.DEFAULT);
        strength = read(tag, "strength_data", PathStrengthData.CODEC, PathStrengthData.DEFAULT);
        qi = read(tag, "qi_data", PathQiData.CODEC, PathQiData.DEFAULT);
        mind = read(tag, "mind_data", MindData.CODEC, MindData.DEFAULT);
        dimensionReturn = read(tag, "dimension_return", DimensionReturnData.CODEC, DimensionReturnData.DEFAULT);
        burst = tag.getBoolean("burst_flag");
        born = tag.getBoolean("born_flag");
    }

    private static <T> void put(CompoundTag tag, String key, Codec<T> codec, T value) {
        codec.encodeStart(NbtOps.INSTANCE, value)
                .resultOrPartial(error -> Guzhenren.LOGGER.error("Cannot save player data {}: {}", key, error))
                .ifPresent(encoded -> tag.put(key, encoded));
    }

    private static <T> T read(CompoundTag tag, String key, Codec<T> codec, T fallback) {
        Tag encoded = tag.get(key);
        if (encoded == null) return fallback;
        return codec.parse(NbtOps.INSTANCE, encoded)
                .resultOrPartial(error -> Guzhenren.LOGGER.error("Cannot load player data {}: {}", key, error))
                .orElse(fallback);
    }
}
