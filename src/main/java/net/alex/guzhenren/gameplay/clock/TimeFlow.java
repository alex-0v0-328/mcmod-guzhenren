package net.alex.guzhenren.gameplay.clock;

import com.google.common.math.LongMath;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家时间流速服务：所有随自身时间或维度时间缩放的数值均须经此类的三个入口换算。
 * <ul>
 *   <li>总时间倍率 = 自身时间倍率 × 所处维度时间倍率（下限为 {@link #MIN_RATE}，防止低速维度把冷却无限拉长）。
 *       {@link #getSelfRate} 遍历玩家身上的活动药水效果并累加 {@link TimeRateContributor}（如两更蛊[Second Watch Gu]、
 *       三更蛊[Third Watch Gu]），保底为 {@link #NORMAL_RATE}；未来宙道造诣加成也在此处唯一接入。</li>
 *   <li>维度时间倍率取自玩家所在维度的 {@link ModDimensions.AnchoredDimension#timeRate()}（可小于 {@code 1}）。</li>
 *   <li>⚠ 业务调用处只允许按语义选用以下三个换算方法之一，切勿自行读取倍率手写乘除（仅界面 {@code InfoModel} 可直接读
 *       {@link #getSelfRate} 展示）：</li>
 *   <li>⚠ {@link #shortenWait}：缩短蓄力、冷却或仪式等等待时长，结果下限锁为 {@code 1} tick，防止高倍率下除成 {@code 0}
 *       变成瞬发。</li>
 *   <li>⚠ {@link #scale(Player, long)}：放大每秒心跳的产出与消耗（真元、念头、寿元），因此自身时间加速时寿元流逝同步加
 *       快。</li>
 *   <li>{@link #getSteps}：计算温养[Nourish]、炼蛊等进度与扣费耦合的系统在本拍应执行的整步数，避免对进度和费用分别取整
 *       造成累计偏差。</li>
 *   <li>非整数倍率对整数的缩放（{@link #scale(double, long, RandomSource)} 与 {@link #getSteps(double, RandomSource)}）
 *       按小数部分作概率进位以保持数学期望精确，整数倍率不消耗随机数；{@link #scale(double, double)} 保留浮点数供调用方
 *       自行累积余数（如真元回复的 {@code essenceCarry}）。</li>
 *   <li>每秒心跳通过 {@link Beat} 在拍初采样一次倍率；纯算术重载方法直接接收 {@code rate}，供无世界环境的单元测试调
 *       用。</li>
 * </ul>
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
