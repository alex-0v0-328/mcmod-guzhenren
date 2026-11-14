package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * 兽力冲击伤害：按自身兽种的力量，不按拳。
 * <ul>
 *   <li>每只 = 兽种永久力量点[Strength Points] × 解放倍数（仅出形或随机补出为 1）× {@link #ATTACK_PER_POINT}。</li>
 *   <li>力气蛊[Strength Qi Gu]远程拳：全面解放 × {@link StrengthService#FULL_RELEASE_FACTOR}，总点数截到当前承受力（含
 *       全力以赴）按比例分摊；五转单熊 10.0，解放 14.0，五转全力以赴解放 30.0。</li>
 *   <li>蓄力一拳：解放 × {@code StoreStrength.CHARGED_RELEASE_FACTOR}，不截断（{@link #UNCAPPED}），熊 120.0；
 *       冲击反噬按该拳自身点数。</li>
 *   <li>⚠ {@link #ATTACK_PER_POINT}（拳头的 2 倍）为 Alex 暂定值。</li>
 * </ul>
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
