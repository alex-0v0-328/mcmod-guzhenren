package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * 兽力冲击（虚影扑击）的伤害分摊纯函数：一拳的远程总伤害按各出形兽种在结算里的份额分给各道兽力冲击。
 * <ul>
 *   <li>份额（{@link #getWeights}）= 该兽种永久兽力[Beast Strength]点数 × 来源倍率（本击全面解放的兽种为
 *       {@link StrengthService#FULL_RELEASE_FACTOR}，随机补出形体的兽种为 {@code 1}）。</li>
 *   <li>{@link #split} 把总伤害按份额比例分摊，各道之和等于总伤害；只有一道时它带全部伤害，份额全为 {@code 0} 时平
 *       分。</li>
 * </ul>
 */

public final class BeastChargeDamage {

    private BeastChargeDamage() {}

    public static int @NotNull [] getWeights(@NotNull PathStrengthData data,
                                             @NotNull List<BeastStrengthFamily> families,
                                             @NotNull List<BeastStrengthFamily> released) {
        int[] weights = new int[families.size()];
        for (int i = 0; i < weights.length; i++) {
            BeastStrengthFamily family = families.get(i);
            weights[i] = Math.max(0, data.getBeastPoints(family))
                    * (released.contains(family) ? StrengthService.FULL_RELEASE_FACTOR : 1);
        }
        return weights;
    }

    public static float @NotNull [] split(float total, int @NotNull [] weights) {
        long sum = 0L;
        for (int weight : weights) sum += Math.max(0, weight);

        float[] damages = new float[weights.length];
        for (int i = 0; i < weights.length; i++) {
            damages[i] = sum > 0L ? (float) ((double) total * Math.max(0, weights[i]) / sum) : total / weights.length;
        }
        return damages;
    }
}
