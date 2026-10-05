package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * How much of the accumulated strength [力道] a body can actually bring to bear: the capacity [承受上限]
 * ramp. Static, read-only service; the capacity comes from the ten-extreme physique [十绝体], the
 * strength itself from {@link PathStrengthService}. {@code isUnleashed} checks 全力以赴.
 *
 * <p>⚠ {@code getUsableJin(int, int)} is a deliberate seam so the ramp is unit-testable without a {@link
 * Player} (a boundary bug once jumped 101 straight to 120; only arithmetic catches that). ⚠ The tail
 * is EARNED over {@code [capacity, capacity × LOCK_MULTIPLE]} in 20 linear steps; the lock scales WITH
 * the physique [体质] (cap 300 locks at 3000). ⚠ 兽力 sits OUTSIDE the ramp and outside 全力以赴's
 * lift -- it counts in 一猪之力, not 斤. ⚠ A mortal reads {@code ExtremePhysique.NONE} → capacity 100.
 *
 * <p>{@link #getHardshipCapacityBonus} adds {@link #HARDSHIP_BONUS_STEP} for every threshold in
 * {@link #HARDSHIP_HEALTH_THRESHOLDS} the health fraction is not above, top down: above 60% adds
 * nothing, 10% or less adds 120.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthService
 * @see BodyService
 * @since 1.0.0
 */

public final class BodyStrengthService {

    private BodyStrengthService() {}

    public static final int OVERFLOW_JIN = 20;
    public static final int LOCK_MULTIPLE = 10;
    public static final int HARDSHIP_BONUS_STEP = 20;
    private static final double[] HARDSHIP_HEALTH_THRESHOLDS = { 0.6D, 0.5D, 0.4D, 0.3D, 0.2D, 0.1D };

    public static int getCapacity(@NotNull Player player) {
        int base = BodyService.getExtremePhysique(player).getStrengthCapacity();
        if (!player.hasEffect(ModEffects.HARDSHIP_STRENGTH_GU)) return base;

        double healthFraction = (double) player.getHealth() / player.getMaxHealth();
        return base + getHardshipCapacityBonus(healthFraction);
    }

    public static int getHardshipCapacityBonus(double healthFraction) {
        int bonus = 0;
        for (double threshold : HARDSHIP_HEALTH_THRESHOLDS) {
            if (healthFraction > threshold) return bonus;
            bonus += HARDSHIP_BONUS_STEP;
        }
        return bonus;
    }

    public static boolean isUnleashed(@NotNull Player player) { return player.hasEffect(ModEffects.ALL_OUT_EFFORT); }

    public static int getUsableJin(@NotNull Player player) {
        int total = PathStrengthService.get(player).totalJin();
        return isUnleashed(player) ? total : getUsableJin(getCapacity(player), total);
    }

    public static int getUsableJin(int capacity, int total) {
        if (total <= capacity) return total;

        int span = capacity * (LOCK_MULTIPLE - 1);
        return capacity + OVERFLOW_JIN * Math.min(total - capacity, span) / span;
    }
}
