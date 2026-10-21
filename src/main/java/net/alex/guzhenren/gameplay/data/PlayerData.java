package net.alex.guzhenren.gameplay.data;

import java.util.function.Supplier;
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
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Everything this mod keeps on a player, read and written through one object: twelve saved values, two
 * in-memory carries.
 *
 * <p>{@link #get} is the single entry point on both sides. Each setter stores the value and returns
 * whether it changed; nine values reach their owner's client on change ({@code ModAttachments} syncs
 * them {@code OWNER_ONLY}). {@link #resetCarriedOnDeath} returns every value a death carries to its
 * default.
 *
 * <p>⚠ A version seam: the only class that touches the loader's player-data API. Here it is a view over
 * NeoForge attachments; the Forge 1.20.1 port has a capability with the same public methods. Services
 * use these methods only, so they stay identical between the two.
 *
 * <p>⚠ Two values are a presence underneath, a shape the methods hide: {@code burst} is on while its
 * attachment exists and a death drops it; the health carry is either stored or absent, and {@link
 * #takeHealthCarry} returns and clears it.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class PlayerData {

    private final Player owner;

    private PlayerData(@NotNull Player owner) { this.owner = owner; }

    public static @NotNull PlayerData get(@NotNull Player player) { return new PlayerData(player); }

    public ApertureData aperture() { return owner.getData(ModAttachments.APERTURE); }

    public float[] essenceCarry() { return owner.getData(ModAttachments.ESSENCE_CARRY); }

    public ApertureStorage apertureStorage() { return owner.getData(ModAttachments.APERTURE_STORAGE); }

    public ApertureNourishData nourish() { return owner.getData(ModAttachments.APERTURE_NOURISH); }

    public BodyData body() { return owner.getData(ModAttachments.BODY); }

    public SoulData soul() { return owner.getData(ModAttachments.SOUL); }

    public PathData path() { return owner.getData(ModAttachments.PATH); }

    public PathStrengthData strength() { return owner.getData(ModAttachments.PATH_STRENGTH); }

    public PathQiData qi() { return owner.getData(ModAttachments.PATH_QI); }

    public MindData mind() { return owner.getData(ModAttachments.MIND); }

    public DimensionReturnData dimensionReturn() { return owner.getData(ModAttachments.DIMENSION_RETURN); }

    public boolean burst() { return owner.hasData(ModAttachments.BURST); }

    public boolean born() { return owner.getData(ModAttachments.BORN); }

    public boolean setAperture(@NotNull ApertureData value) { return set(ModAttachments.APERTURE, value); }

    public boolean setApertureStorage(@NotNull ApertureStorage value) {
        return set(ModAttachments.APERTURE_STORAGE, value);
    }

    public boolean setNourish(@NotNull ApertureNourishData value) {
        return set(ModAttachments.APERTURE_NOURISH, value);
    }

    public boolean setBody(@NotNull BodyData value) { return set(ModAttachments.BODY, value); }

    public boolean setSoul(@NotNull SoulData value) { return set(ModAttachments.SOUL, value); }

    public boolean setPath(@NotNull PathData value) { return set(ModAttachments.PATH, value); }

    public boolean setStrength(@NotNull PathStrengthData value) { return set(ModAttachments.PATH_STRENGTH, value); }

    public boolean setQi(@NotNull PathQiData value) { return set(ModAttachments.PATH_QI, value); }

    public boolean setMind(@NotNull MindData value) { return set(ModAttachments.MIND, value); }

    public boolean setDimensionReturn(@NotNull DimensionReturnData value) {
        return set(ModAttachments.DIMENSION_RETURN, value);
    }

    public boolean setBurst(boolean value) {
        if (value == burst()) return false;
        if (value) {
            owner.setData(ModAttachments.BURST, true);
        } else {
            owner.removeData(ModAttachments.BURST);
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
