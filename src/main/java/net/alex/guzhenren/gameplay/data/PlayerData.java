package net.alex.guzhenren.gameplay.data;

import java.util.Objects;
import java.util.function.Supplier;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.MugwortShieldData;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.path.PathData;
import net.alex.guzhenren.gameplay.path.qi.PathQiData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindData;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Everything this mod keeps on a player, read and written through one object: fourteen saved values, the
 * second-stage flight [二段飞行] mark and the plunge [坠击] top, synced but never saved, and one in-memory carry, the
 * essence regen remainder ({@link #getEssenceCarry}).
 *
 * <p>{@link #get} is the single entry point on both sides. Each setter stores the value and returns
 * whether it changed; ten values reach their owner's client on change ({@code ModAttachments} syncs
 * them {@code OWNER_ONLY}). {@link #resetCarriedOnDeath} returns every value a death carries to its
 * default.
 *
 * <p>⚠ The only class that touches the attachment API: a view over NeoForge attachments. Services use these
 * methods only.
 *
 * <p>⚠ Three values are a presence underneath, a shape the methods hide: the second-stage mark is on while its
 * attachment exists, and a relog or a death drops it; the plunge top is {@code null} unless the player is
 * plunging, and drops the same way; the health carry is either stored or absent, and {@link #takeHealthCarry}
 * returns and clears it.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class PlayerData {

    private final Player owner;

    private PlayerData(@NotNull Player owner) { this.owner = owner; }

    public static @NotNull PlayerData get(@NotNull Player player) { return new PlayerData(player); }

    public ApertureData getAperture() { return owner.getData(ModAttachments.APERTURE); }

    public float[] getEssenceCarry() { return owner.getData(ModAttachments.ESSENCE_CARRY); }

    public ApertureStorage getApertureStorage() { return owner.getData(ModAttachments.APERTURE_STORAGE); }

    public ApertureNourishData getNourish() { return owner.getData(ModAttachments.APERTURE_NOURISH); }

    public BodyData getBody() { return owner.getData(ModAttachments.BODY); }

    public MugwortShieldData getMugwortShield() { return owner.getData(ModAttachments.MUGWORT_SHIELD); }

    public SoulData getSoul() { return owner.getData(ModAttachments.SOUL); }

    public PathData getPath() { return owner.getData(ModAttachments.PATH); }

    public PathStrengthData getStrength() { return owner.getData(ModAttachments.PATH_STRENGTH); }

    public PathQiData getQi() { return owner.getData(ModAttachments.PATH_QI); }

    public MindData getMind() { return owner.getData(ModAttachments.MIND); }

    public DimensionReturnData getDimensionReturn() { return owner.getData(ModAttachments.DIMENSION_RETURN); }

    public PlayerRewindData getRewind() { return owner.getData(ModAttachments.PLAYER_REWIND); }

    public boolean isInSecondStage() { return owner.hasData(ModAttachments.SECOND_STAGE); }

    public @Nullable Double getPlungeTopY() {
        return owner.hasData(ModAttachments.PLUNGE) ? owner.getData(ModAttachments.PLUNGE) : null;
    }

    public boolean isBorn() { return owner.getData(ModAttachments.BORN); }

    public boolean setAperture(@NotNull ApertureData value) { return set(ModAttachments.APERTURE, value); }

    public boolean setApertureStorage(@NotNull ApertureStorage value) {
        return set(ModAttachments.APERTURE_STORAGE, value);
    }

    public boolean setNourish(@NotNull ApertureNourishData value) {
        return set(ModAttachments.APERTURE_NOURISH, value);
    }

    public boolean setBody(@NotNull BodyData value) { return set(ModAttachments.BODY, value); }

    public boolean setMugwortShield(@NotNull MugwortShieldData value) {
        return set(ModAttachments.MUGWORT_SHIELD, value);
    }

    public boolean setSoul(@NotNull SoulData value) { return set(ModAttachments.SOUL, value); }

    public boolean setPath(@NotNull PathData value) { return set(ModAttachments.PATH, value); }

    public boolean setStrength(@NotNull PathStrengthData value) { return set(ModAttachments.PATH_STRENGTH, value); }

    public boolean setQi(@NotNull PathQiData value) { return set(ModAttachments.PATH_QI, value); }

    public boolean setMind(@NotNull MindData value) { return set(ModAttachments.MIND, value); }

    public boolean setDimensionReturn(@NotNull DimensionReturnData value) {
        return set(ModAttachments.DIMENSION_RETURN, value);
    }

    public boolean setRewind(@NotNull PlayerRewindData value) { return set(ModAttachments.PLAYER_REWIND, value); }

    public boolean setSecondStage(boolean value) {
        if (value == isInSecondStage()) return false;
        if (value) {
            owner.setData(ModAttachments.SECOND_STAGE, true);
        } else {
            owner.removeData(ModAttachments.SECOND_STAGE);
        }
        return true;
    }

    public boolean setPlungeTopY(@Nullable Double value) {
        if (Objects.equals(value, getPlungeTopY())) return false;
        if (value == null) {
            owner.removeData(ModAttachments.PLUNGE);
        } else {
            owner.setData(ModAttachments.PLUNGE, value);
        }
        return true;
    }

    public boolean setBorn(boolean value) { return set(ModAttachments.BORN, value); }

    public void setHealthCarry(float value) { owner.setData(ModAttachments.HEALTH_CARRY, value); }

    /** The carried health, cleared; {@code null} when nothing was carried. */
    public @Nullable Float takeHealthCarry() { return owner.removeData(ModAttachments.HEALTH_CARRY); }

    public void resetEssenceCarry() { owner.removeData(ModAttachments.ESSENCE_CARRY); }

    /** Every value a death carries back to its default, synced; the reset of a death without keepInventory. */
    public void resetCarriedOnDeath() {
        for (AttachmentType<?> type : ModAttachments.getInherited()) owner.removeData(type);
    }

    private <T> boolean set(Supplier<AttachmentType<T>> type, T value) {
        if (value.equals(owner.getData(type))) return false;
        owner.setData(type, value);
        return true;
    }
}
