package net.alex.guzhenren.client.input;

/**
 * Pure double-tap detection over client ticks, kept free of game state so plain JUnit covers it.
 *
 * <p>{@link #press} reports a double tap when the previous press came at most {@link #windowTicks} ticks earlier,
 * then forgets both, so a third quick press starts a new pair instead of toggling again.
 *
 * @author Alex
 * @version 1.0.0
 * @see BurstInput
 * @since 1.0.0
 */

public final class DoubleTap {

    private static final long NONE = Long.MIN_VALUE;
    private final int windowTicks;
    private long lastPressTicks = NONE;

    public DoubleTap(int windowTicks) {
        this.windowTicks = windowTicks;
    }

    public boolean press(long ticks) {
        if (lastPressTicks != NONE && ticks - lastPressTicks <= windowTicks) {
            lastPressTicks = NONE;
            return true;
        }
        lastPressTicks = ticks;
        return false;
    }
}
