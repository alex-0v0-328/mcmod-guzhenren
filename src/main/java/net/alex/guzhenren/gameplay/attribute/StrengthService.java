package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The strength settlement [力道结算]: how many of a body's strength points [力量点] it brings to bear. Static,
 * read-only service above the aperture and the body, since the capacity reads both; the points themselves come
 * from {@link PathStrengthService#getStrengthPoints}.
 *
 * <p>The body's carrying capacity [承受力] is {@link #getBodyCapacity}: {@link #getBaseCapacity}, which is
 * {@link #MORTAL_CAPACITY} plus {@link #CAPACITY_PER_RANK} per rank [转] (16, then 24 to 56 for ranks one to five)
 * times the physique's multiplier (a ten-extreme body doubles it, the Great Strength True Martial body quadruples
 * it), plus what the worn skin, tendons and bones add -- tempering sits outside the multiplier. A body always uses
 * {@code min(points, capacity)} of the CURRENT capacity ({@link #getCapacity}) and keeps the rest; there is no
 * overflow ramp.
 *
 * <p>{@link #isUnleashed} lifts the current capacity to {@code max(body capacity, points)}: the All-Out Effort Gu
 * [全力以赴] and the Hardship Strength Gu [苦力蛊] both make every point count, read as their vanilla effects like
 * every attribute rule (the path effect is the truth and re-projects the icon each heartbeat; {@code gameplay}
 * cannot reach {@code TimedEffects}). Only the first pays for it:
 * {@link #getLoadDamage} is the load [负载] a hit lands on its own body while All-Out Effort runs without Hardship
 * Strength, {@link #LOAD_PER_EXCESS_POINT} health per point past the BODY capacity, and
 * {@code StrengthLoadEvents} deals it. Hardship Strength pays with the healing ceiling instead. ⚠ The excess reads
 * {@link #getBodyCapacity}, never the lifted one, which would leave no excess while the lift runs.
 *
 * <p>{@link #getPhantomBonusPoints} is the Beast Phantom [兽力虚影]: for the one hit it manifests on, the chosen
 * beast's points count in full past the capacity while the rest are still cut; it costs the body no load.
 *
 * <p>⚠ {@code getUsablePoints(int, int)}, {@code getCapacity(int, int, boolean)}, {@code getBaseCapacity},
 * {@code getPhantomBonusPoints} and {@code getLoadDamage(int)} are deliberate seams so the arithmetic is
 * unit-testable without a {@link Player}.
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
    public static final float LOAD_PER_EXCESS_POINT = 0.0625F;

    public static int getBaseCapacity(@NotNull Rank rank, @NotNull ExtremePhysique physique) {
        int rankIndex = Math.min(rank.ordinal(), Rank.HIGHEST.ordinal());
        return (MORTAL_CAPACITY + CAPACITY_PER_RANK * rankIndex) * physique.getCapacityMultiplier();
    }

    public static int getBodyCapacity(@NotNull Player player) {
        BodyData body = BodyService.get(player);
        return getBaseCapacity(ApertureService.getHealthRank(player), body.extremePhysique())
                + body.skin().getCapacityBonus()
                + body.tendon().getCapacityBonus()
                + body.bone().getCapacityBonus();
    }

    /** The current capacity: the body's own, lifted to the whole strength while {@link #isUnleashed}. */
    public static int getCapacity(@NotNull Player player) {
        return getCapacity(getBodyCapacity(player), getStrengthPoints(player), isUnleashed(player));
    }

    public static int getCapacity(int bodyCapacity, int points, boolean unleashed) {
        return unleashed ? Math.max(bodyCapacity, points) : bodyCapacity;
    }

    public static int getStrengthPoints(@NotNull Player player) { return PathStrengthService.getStrengthPoints(player); }

    public static boolean isUnleashed(@NotNull Player player) {
        return player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU) || isHardship(player);
    }

    public static boolean isHardship(@NotNull Player player) {
        return player.hasEffect(ModEffects.HARDSHIP_STRENGTH_GU);
    }

    public static int getUsablePoints(@NotNull Player player) {
        return getUsablePoints(getCapacity(player), getStrengthPoints(player));
    }

    public static int getUsablePoints(int capacity, int total) { return Math.min(capacity, total); }

    public static int getPhantomBonusPoints(int capacity, int total, int beastPoints) {
        int beast = Math.max(0, Math.min(beastPoints, total));
        return Math.max(0, getUsablePoints(capacity, total - beast) + beast - getUsablePoints(capacity, total));
    }

    /** The points past the body capacity, zero when nothing is. */
    public static int getExcessPoints(@NotNull Player player) {
        return Math.max(0, getStrengthPoints(player) - getBodyCapacity(player));
    }

    /** The load one landed hit costs its own body, zero unless All-Out Effort runs without Hardship Strength. */
    public static float getLoadDamage(@NotNull Player player) {
        if (!player.hasEffect(ModEffects.ALL_OUT_EFFORT_GU) || isHardship(player)) return 0.0F;

        return getLoadDamage(getExcessPoints(player));
    }

    public static float getLoadDamage(int excessPoints) { return Math.max(0, excessPoints) * LOAD_PER_EXCESS_POINT; }
}
