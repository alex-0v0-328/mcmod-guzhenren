package net.alex.guzhenren.gameplay.clock;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

/**
 * One heartbeat's reading of the player's clock, taken once so that every step of the beat runs on the same time.
 *
 * <p>{@link #days} is how many whole days the day clock advanced since the last beat (the Gu-hunger walks spend it);
 * {@link #rate} is {@link TimeFlow#getRate} at the start of the beat. Its verbs are {@link TimeFlow}'s, bound to that
 * rate: a step scales through its beat and never asks the clock again.
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeFlow
 * @since 1.0.0
 */

public record Beat(long days, double rate, @NotNull RandomSource random) {

    public long scale(long amount) { return TimeFlow.scale(rate, amount, random); }

    public double scale(double amount) { return TimeFlow.scale(rate, amount); }

    public int getSteps() { return TimeFlow.getSteps(rate, random); }
}
