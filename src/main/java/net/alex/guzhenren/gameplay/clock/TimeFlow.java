package net.alex.guzhenren.gameplay.clock;

import com.google.common.math.LongMath;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * The player's clock: how fast time runs for him, and the only door anything time-scaled goes through.
 *
 * <p>The rate is his own clock [自身时间] times the world's. {@link #getSelfRate} adds up every
 * {@link TimeRateContributor} among his active effects (the Watch Gu [更蛊] forms), flooring at 1; only the Time Path
 * [宙道] comes to it. {@link #getWorldRate} multiplies the rate of the dimension he stands in
 * ({@link ModDimensions.AnchoredDimension#timeRate}) by every {@link TimeFlowArea} added through {@link #addArea}. A
 * world rate may run below 1, so {@link #getRate} is a {@code double}, floored at {@link #MIN_RATE} so that no wait
 * stretches without end.
 *
 * <p>⚠ THREE verbs leave this class and a caller uses ONE: {@code shortenWait}, {@code scale}, {@code getSteps};
 * arithmetic on a rate at a call site runs 寿元 backwards -- only {@code InfoModel} may read
 * {@link #getSelfRate}. ⚠ {@code scale} is what 寿元 goes through, so a hastened life is SPENT FASTER (same verb as
 * essence [真元] and thoughts [念]); {@code shortenWait} floors at one tick (a short wait divided by a fast clock is
 * zero, which reads as "no wait"). ⚠ A 造诣 grade term has no spec yet; when it lands it goes INSIDE
 * {@link #getSelfRate}.
 *
 * <p>{@link #shortenWait(Player, int)} is a stretch he has to sit through: a press held down, a cooldown, a ritual
 * waited out. {@link #scale(Player, long)} is what he earns or SPENDS in one step -- essence, thought, and the life it
 * costs him. {@link #getSteps} is how many times a coupled counter must run this beat, for the two that cannot take a
 * bigger step: 温养 and 炼蛊, whose progress and price would round apart if either were scaled on its own. The
 * heartbeat reads the rate once into a {@link Beat} and its steps scale through that.
 *
 * <p>A fractional rate leaves a fraction a whole count cannot hold: {@link #scale(double, long, RandomSource)} and
 * {@link #getSteps(double, RandomSource)} round it up with exactly that probability, so the expected count is exact.
 * A whole rate never draws from the random, so whole rates stay exact and deterministic; {@code scale(double)} keeps
 * the fraction for the caller's own carry (essence banks it in {@code ESSENCE_CARRY}).
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
    private static final List<TimeFlowArea> AREAS = new CopyOnWriteArrayList<>();

    public static void addArea(@NotNull TimeFlowArea area) { AREAS.add(area); }

    //region the rate -- his own clock [自身时间] times the world's
    public static int getSelfRate(@NotNull Player player) {
        int rate = 0;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (ModEffects.getEffectOf(instance) instanceof TimeRateContributor contributor) {
                rate += contributor.timeRate(instance.getAmplifier());
            }
        }
        //   TODO(宙道造诣): a grade term joins HERE, so that no caller has to learn about it.
        return Math.max(NORMAL_RATE, rate);
    }

    public static double getWorldRate(@NotNull Level level, @NotNull Vec3 position) {
        ModDimensions.AnchoredDimension dimension = ModDimensions.ANCHORED_DIMENSIONS.get(level.dimension());
        double rate = dimension == null ? NORMAL_RATE : dimension.timeRate();
        for (TimeFlowArea area : AREAS) rate *= area.getTimeRate(level, position);
        return rate;
    }

    public static double getRate(@NotNull Player player) {
        return Math.max(MIN_RATE, getSelfRate(player) * getWorldRate(player.level(), player.position()));
    }
    //endregion

    //region the three verbs
    public static int shortenWait(@NotNull Player player, int ticks) { return shortenWait(getRate(player), ticks); }

    public static long scale(@NotNull Player player, long amount) {
        return scale(getRate(player), amount, player.getRandom());
    }

    public static double scale(@NotNull Player player, double amount) { return scale(getRate(player), amount); }

    public static int getSteps(@NotNull Player player) { return getSteps(getRate(player), player.getRandom()); }
    //endregion

    //region the arithmetic alone -- a rate rather than a player, so it can be asserted without a world
    public static int shortenWait(double rate, int ticks) {
        return ticks <= 0 ? ticks : (int) Math.min(Integer.MAX_VALUE, Math.max((long) (ticks / rate), 1L));
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
