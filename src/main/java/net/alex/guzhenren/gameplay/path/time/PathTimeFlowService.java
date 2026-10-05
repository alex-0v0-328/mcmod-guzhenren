package net.alex.guzhenren.gameplay.path.time;

import com.google.common.math.LongMath;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The only door the player's own clock [自身时间] is hastened through; Time Path [宙道] is all that comes
 * to it. {@code getRate()} walks {@code getActiveEffects()} for every {@link TimeRateContributor}, flooring at 1.
 *
 * <p>⚠ THREE verbs leave this class and a caller uses ONE: {@code shortenWait}, {@code scale}, {@code
 * getSteps}; doing arithmetic on {@code getRate()} at a call site is how 寿元 once aged BACKWARDS -- only
 * {@code InfoModel} may read {@code getRate()}. ⚠ {@code scale} is what 寿元 goes through, so a
 * hastened life is SPENT FASTER (same verb as essence [真元] and thoughts [念]); {@code shortenWait} floors
 * at one tick ({@link #shortenWait(int, int)}: a short wait divided by a fast clock is zero, which reads
 * as "no wait"). ⚠ A 造诣 grade term has no spec yet; when it lands it goes INSIDE {@code getRate()}.
 *
 * <p>{@link #shortenWait(Player, int)} is a stretch he has to sit through: a press held down, a cooldown,
 * a ritual waited out. {@link #scale(Player, long)} is what he earns or SPENDS in one step --
 * essence, thought, and the life it costs him. {@link #getSteps} is how many times a coupled counter
 * must run this beat, for the two that cannot take a bigger step: 温养 and 炼蛊, whose progress and
 * price would round apart if either were scaled on its own.
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeRateContributor
 * @since 1.0.0
 */

public final class PathTimeFlowService {

    private PathTimeFlowService() {}

    public static final int NORMAL_RATE = 1;

    public static int getRate(@NotNull Player player) {
        int rate = 0;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.getEffect().value() instanceof TimeRateContributor contributor) {
                rate += contributor.timeRate(instance.getAmplifier());
            }
        }
        //   TODO(宙道造诣): a grade term joins HERE, so that no caller has to learn about it.
        return Math.max(NORMAL_RATE, rate);
    }

    //region his own clock [自身时间] -- three verbs, because it only ever takes three shapes
    public static int shortenWait(@NotNull Player player, int ticks) { return shortenWait(getRate(player), ticks); }

    public static long scale(@NotNull Player player, long amount) { return scale(getRate(player), amount); }

    public static double scale(@NotNull Player player, double amount) { return scale(getRate(player), amount); }

    public static int getSteps(@NotNull Player player) { return getRate(player); }
    //endregion

    //region the arithmetic alone -- a rate rather than a player, so it can be asserted without a world
    public static int shortenWait(int rate, int ticks) { return ticks <= 0 ? ticks : Math.max(1, ticks / rate); }

    public static long scale(int rate, long amount) { return LongMath.saturatedMultiply(amount, rate); }

    public static double scale(int rate, double amount) { return amount * rate; }
    //endregion
}
