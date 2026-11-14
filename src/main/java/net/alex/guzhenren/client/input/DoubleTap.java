package net.alex.guzhenren.client.input;

/**
 * 以客户端 tick 计的纯双击判定，不碰游戏状态，纯 JUnit 可测。
 * <ul>
 *   <li>{@link #press}：距上次按下不超过 {@link #windowTicks} tick 即报双击并忘掉两次，第三次快按开始新一对而不再切换；
 *       {@link #reset} 忘掉待配对的按下。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageInput
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

    public void reset() { lastPressTicks = NONE; }
}
