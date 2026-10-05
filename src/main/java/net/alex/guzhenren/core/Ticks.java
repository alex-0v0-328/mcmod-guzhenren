package net.alex.guzhenren.core;

/**
 * Every unit of time this mod speaks, in ticks.
 *
 * <p>The sole source of time constants for the entire mod: {@code SECOND 20}, {@code HALF_SECOND 10},
 * {@code MINUTE 1200}, {@code HALF_MINUTE 600},
 * {@code DAY 24000}, {@code HALF_DAY 12000}. There is no config in this mod, and there must never be
 * one -- a server owner who "configured" a day to 5000 would only desync aging, regen and feeding from
 * the day/night cycle. Never declare a second {@code 24000} anywhere else; use this class.
 *
 * <p>⚠ A duration someone might one day want to tune still belongs here as a named constant rather
 * than inline in whatever code happens to use it first.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class Ticks {

    private Ticks() {}

    public static final int SECOND = 20;
    public static final int HALF_SECOND = SECOND / 2;
    public static final int MINUTE = 60 * SECOND;
    public static final int HALF_MINUTE = MINUTE / 2;
    public static final int DAY = 24000;
    public static final int HALF_DAY = DAY / 2;
}
