package net.alex.guzhenren.gameplay.attribute;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthAmplifier;
import net.alex.guzhenren.gameplay.path.strength.StrengthAmplifierProvider;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The strength settlement [力道结算]: how many of a body's strength points [力量点] it brings to bear. Static,
 * read-only service above the aperture and the body, since the capacity reads both. The points come in three
 * kinds: the permanent record ({@link PathStrengthData}: human jin and beast strengths by family), the running
 * {@link StrengthAmplifier}s ({@link #getAmplifiers}: timed beast buffs and every path effect payload that is a
 * {@link StrengthAmplifierProvider}), and the points a round's beast release [解放] counts past the capacity
 * ({@link ReleaseSource}, set by the round keeper). {@link #snapshot} gathers the first two kinds and what they
 * settle to once ({@link StrengthSnapshot}); the capacity, the total and the usable points of a player each read one.
 *
 * <p>The normal carrying capacity [常态承受力] is {@link #getNormalCapacity}: {@link #getBaseCapacity}, which is
 * {@link #MORTAL_CAPACITY} plus {@link #CAPACITY_PER_RANK} per rank [转] (16, then 24 to 56 for ranks one to five)
 * times the physique's multiplier, plus what the worn skin, tendons and bones add -- tempering sits outside the
 * multiplier. The current capacity ({@link #getCapacity}) adds every amplifier's capacity bonus and, while the All-Out
 * Effort Gu [全力以赴] runs, {@link #ALL_OUT_CAPACITY_FACTOR} minus one more normal capacities: every bonus is added on
 * the normal capacity, never multiplied with another. A body uses {@code min(total, capacity)} of the CURRENT capacity
 * ({@link #getUsablePoints}) plus the release extra, and keeps the rest; there is no overflow ramp and no load.
 *
 * <p>{@link #getReleasedExtra} is one beast family brought to bear in a round: its points count in full past the
 * capacity while the rest are still cut, and a full release [全面解放] adds {@link #FULL_RELEASE_FACTOR} minus one
 * more of the family's PERMANENT points, which no capacity limits. {@link #canAdmit} is the gate of a new permanent
 * beast strength: the permanent beasts held plus the new one may not pass the NORMAL capacity; amplifiers and the
 * All-Out lift admit nothing.
 *
 * <p>⚠ The {@code int} overloads are deliberate seams so the arithmetic is unit-testable without a {@link Player}.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthService
 * @see AttackDamageService
 * @since 1.0.0
 */

public final class StrengthService {

    private StrengthService() {}

    public static final int MORTAL_CAPACITY = 16;
    public static final int CAPACITY_PER_RANK = 8;
    /** All-Out Effort makes the current capacity this many normal capacities (300%). */
    public static final int ALL_OUT_CAPACITY_FACTOR = 3;
    /** A full release counts the released family's permanent points this many times. */
    public static final int FULL_RELEASE_FACTOR = 3;
    private static ReleaseSource releaseSource = (player, snapshot) -> 0;

    /** The round keeper's answer: the points a release lets past the capacity right now, zero outside a round. */
    @FunctionalInterface
    public interface ReleaseSource {

        int getReleasedExtra(@NotNull Player player, @NotNull StrengthSnapshot snapshot);
    }

    public static void setReleaseSource(@NotNull ReleaseSource source) { releaseSource = source; }

    public static int getBaseCapacity(@NotNull Rank rank, @NotNull ExtremePhysique physique) {
        int rankIndex = Math.min(rank.ordinal(), Rank.HIGHEST.ordinal());
        return (MORTAL_CAPACITY + CAPACITY_PER_RANK * rankIndex) * physique.getCapacityMultiplier();
    }

    /** The normal capacity [常态承受力]: rank, physique and tempering, nothing temporary. */
    public static int getNormalCapacity(@NotNull Player player) {
        BodyData body = BodyService.get(player);
        return getBaseCapacity(ApertureService.getHealthRank(player), body.extremePhysique())
                + body.skin().getCapacityBonus()
                + body.tendon().getCapacityBonus()
                + body.bone().getCapacityBonus();
    }

    /** The current capacity: the normal one plus every amplifier's bonus plus the All-Out lift. */
    public static int getCapacity(@NotNull Player player) { return snapshot(player).capacity(); }

    public static int getCapacity(int normalCapacity, int amplifierBonus, boolean allOut) {
        return normalCapacity + amplifierBonus + (allOut ? (ALL_OUT_CAPACITY_FACTOR - 1) * normalCapacity : 0);
    }

    public static int getPermanentPoints(@NotNull Player player) {
        return PathStrengthService.get(player).getStrengthPoints();
    }

    /** Every running amplifier: the timed beast buffs, then every path effect payload that provides one. */
    public static @NotNull List<StrengthAmplifier> getAmplifiers(@NotNull Player player) {
        return getAmplifiers(player, getPermanentPoints(player), getNormalCapacity(player));
    }

    private static List<StrengthAmplifier> getAmplifiers(Player player, int permanent, int normal) {
        List<StrengthAmplifier> amplifiers = new ArrayList<>(PathStrengthService.getBeastBuffs(player));
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            if (!(effect.payload() instanceof StrengthAmplifierProvider provider)) continue;

            StrengthAmplifier amplifier = provider.getAmplifier(player, permanent, normal);
            if (amplifier != null) amplifiers.add(amplifier);
        }
        return amplifiers;
    }

    public static int getAmplifierPoints(@NotNull List<StrengthAmplifier> amplifiers) {
        int total = 0;
        for (StrengthAmplifier amplifier : amplifiers) total += amplifier.points();
        return total;
    }

    public static int getCapacityBonus(@NotNull List<StrengthAmplifier> amplifiers) {
        int total = 0;
        for (StrengthAmplifier amplifier : amplifiers) total += amplifier.capacityBonus();
        return total;
    }

    /** The whole strength: permanent points plus every amplifier's points. */
    public static int getStrengthPoints(@NotNull Player player) { return snapshot(player).totalPoints(); }

    public static boolean isAllOut(@NotNull Player player) { return player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU); }

    public static @NotNull StrengthSnapshot snapshot(@NotNull Player player) {
        int normal = getNormalCapacity(player);
        int permanent = getPermanentPoints(player);
        return StrengthSnapshot.of(normal, permanent, getAmplifiers(player, permanent, normal), isAllOut(player));
    }

    /** What the body brings to bear right now: the cut total plus whatever the round releases past the capacity. */
    public static int getUsablePoints(@NotNull Player player) { return getUsablePoints(player, snapshot(player)); }

    public static int getUsablePoints(@NotNull Player player, @NotNull StrengthSnapshot snapshot) {
        return getUsablePoints(snapshot.capacity(), snapshot.totalPoints())
                + releaseSource.getReleasedExtra(player, snapshot);
    }

    public static int getUsablePoints(int capacity, int total) { return Math.min(capacity, total); }

    /**
     * The points one released family adds past {@code getUsablePoints(capacity, total)}: its {@code familyPoints}
     * count in full, and a full release adds {@link #FULL_RELEASE_FACTOR} minus one more of
     * {@code fullReleasePoints}, the family's permanent points, or zero for a plain release.
     */
    public static int getReleasedExtra(int capacity, int total, int familyPoints, int fullReleasePoints) {
        int family = Math.max(0, Math.min(familyPoints, total));
        int plain = Math.max(0, getUsablePoints(capacity, total - family) + family - getUsablePoints(capacity, total));
        return plain + (FULL_RELEASE_FACTOR - 1) * Math.max(0, fullReleasePoints);
    }

    /** Whether the normal capacity has room for one more permanent beast strength beside those held. */
    public static boolean canAdmit(@NotNull Player player, @NotNull BeastStrength beast) {
        return canAdmit(getNormalCapacity(player), PathStrengthService.get(player).getBeastPoints(), beast.getPoints());
    }

    public static boolean canAdmit(int normalCapacity, int heldBeastPoints, int newBeastPoints) {
        return heldBeastPoints + newBeastPoints <= normalCapacity;
    }
}
