package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * What a Beast Charge [兽力冲击] hits for: the strength its own family adds to the body, never the punch's. Each
 * charge counts its family's permanent points [力量点] ({@link PathStrengthData#getBeastPoints}) times a release
 * factor, one for a family only shown or drawn at random, and is worth {@link #ATTACK_PER_POINT} attack a point,
 * twice the fist's {@link PathStrengthData#ATTACK_PER_POINT}.
 *
 * <p>The Strength Qi Gu's [力气蛊] ranged punch counts a family the round fully releases [全面解放]
 * {@link StrengthService#FULL_RELEASE_FACTOR} times and cuts the charges' summed points to the current capacity
 * [承受力], All-Out Effort's [全力以赴] lift included, each charge keeping its share: a rank five bear alone hits for
 * 10.0, released 14.0, released under the rank five All-Out Effort 30.0. The charged punch [蓄力一拳] counts a family
 * its charge releases {@code StoreStrength.CHARGED_RELEASE_FACTOR} times and cuts nothing ({@link #UNCAPPED}): the
 * bear's 120.0; its impact backlash still reads the punch's own points.
 *
 * <p>⚠ {@link #ATTACK_PER_POINT} is Alex's number and provisional.
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthRound#getFullyReleased
 * @see StoreStrength#getReleasedFamilies
 * @since 1.0.0
 */

public final class BeastChargeDamage {

    private BeastChargeDamage() {}

    public static final double ATTACK_PER_POINT = 0.25D;
    public static final int UNCAPPED = -1;

    public static float @NotNull [] getDamages(@NotNull PathStrengthData data,
                                               @NotNull List<BeastStrengthFamily> families,
                                               @NotNull List<BeastStrengthFamily> released, int releaseFactor,
                                               int cap) {
        int[] points = new int[families.size()];
        for (int i = 0; i < points.length; i++) {
            BeastStrengthFamily family = families.get(i);
            points[i] = getPoints(data.getBeastPoints(family), released.contains(family) ? releaseFactor : 1);
        }
        return getDamages(points, cap);
    }

    public static int getPoints(int familyPoints, int factor) {
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, familyPoints) * Math.max(0, factor));
    }

    public static float @NotNull [] getDamages(int @NotNull [] points, int cap) {
        long total = 0L;
        for (int point : points) total += Math.max(0, point);
        double share = cap >= 0 && total > cap ? (double) cap / total : 1.0D;

        float[] damages = new float[points.length];
        for (int i = 0; i < points.length; i++) {
            damages[i] = (float) (Math.max(0, points[i]) * share * ATTACK_PER_POINT);
        }
        return damages;
    }
}
