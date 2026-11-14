package net.alex.guzhenren.gameplay.clock;

import com.google.common.math.LongMath;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家时钟：时间流速，随时间缩放的量只经此进出。
 * <ul>
 *   <li>总倍率 = 自身 × 世界。{@link #getSelfRate} 累加效果中的 {@link TimeRateContributor}（两更蛊[Second Watch Gu]、
 *       三更蛊[Third Watch Gu] 等），最低 1，仅宙道[Time Path]用；⚠ 宙道造诣项未定，日后也放这里。</li>
 *   <li>世界倍率取所站维度 {@link ModDimensions.AnchoredDimension#timeRate()}，可低于 1；故 {@link #getRate} 为
 *       {@code double}，下限 {@link #MIN_RATE}，等待不无限拉长。</li>
 *   <li>⚠ 只出三个动词，调用处择一；自算倍率会让寿元倒流，仅 {@code InfoModel} 可读 {@link #getSelfRate}。</li>
 *   <li>⚠ {@link #shortenWait(Player, int)}：缩等待（蓄力、冷却、仪式），下限 1 tick，免得快时钟除成 0 读作「不用
 *       等」。</li>
 *   <li>⚠ {@link #scale(Player, long)}：放大每步收支（真元[Essence]、念、寿元），故加速更耗寿元。</li>
 *   <li>{@link #getSteps}：温养[Nourish]、炼蛊等耦合计数本拍跑几步，单独缩放会让进度与费用舍入错开。</li>
 *   <li>小数倍率按小数概率进 1（{@link #scale(double, long, RandomSource)}、{@link #getSteps(double, RandomSource)}），
 *       期望精确；整数倍率不掷随机；{@code scale(double)} 留小数给调用方累计（真元存 {@code ESSENCE_CARRY}）。</li>
 *   <li>心跳经 {@link Beat} 每拍只读一次；纯算术重载收倍率，无需世界即可断言。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeRateContributor
 * @see Beat
 * @since 1.0.0
 */

public final class TimeFlow {

    private TimeFlow() {}

    public static final int NORMAL_RATE = 1;
    public static final double MIN_RATE = 0.125;

    //region the rate [速率]
    public static int getSelfRate(@NotNull Player player) {
        int rate = 0;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().value() instanceof TimeRateContributor contributor) {
                rate += contributor.timeRate(instance.getAmplifier());
            }
        }
        //   TODO(宙道造诣): a grade term joins HERE, so that no caller has to learn about it.
        return Math.max(NORMAL_RATE, rate);
    }

    private static double getWorldRate(@NotNull Level level) {
        ModDimensions.AnchoredDimension dimension = ModDimensions.ANCHORED_DIMENSIONS.get(level.dimension());
        return dimension == null ? NORMAL_RATE : dimension.timeRate();
    }

    public static double getRate(@NotNull Player player) {
        return Math.max(MIN_RATE, getSelfRate(player) * getWorldRate(player.level()));
    }
    //endregion

    //region the three verbs [三个动词]
    public static int shortenWait(@NotNull Player player, int ticks) { return shortenWait(getRate(player), ticks); }

    public static long scale(@NotNull Player player, long amount) {
        return scale(getRate(player), amount, player.getRandom());
    }

    public static int getSteps(@NotNull Player player) { return getSteps(getRate(player), player.getRandom()); }
    //endregion

    //region the arithmetic alone [纯算术]
    public static int shortenWait(double rate, int ticks) {
        return ticks <= 0 ? ticks : Math.clamp((long) (ticks / rate), 1, Integer.MAX_VALUE);
    }

    public static long scale(double rate, long amount, @NotNull RandomSource random) {
        if (rate == Math.rint(rate)) return LongMath.saturatedMultiply(amount, (long) rate);
        return roll(amount * rate, random);
    }

    public static double scale(double rate, double amount) { return amount * rate; }

    public static int getSteps(double rate, @NotNull RandomSource random) { return (int) roll(rate, random); }

    private static long roll(double value, RandomSource random) {
        long whole = (long) Math.floor(value);
        double fraction = value - whole;
        return fraction > 0.0 && random.nextDouble() < fraction ? whole + 1 : whole;
    }
    //endregion
}
