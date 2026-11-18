package net.alex.guzhenren.client.input;

/**
 * 基于客户端 tick 计数的纯双击状态机：不依赖任何游戏状态，可直接由纯 JUnit 单元测试覆盖。
 * <ul>
 *   <li>{@link #press} 判定距离上次有效按下是否不超过 {@link #windowTicks} tick：若在窗口内则触发双击并清除两次按下记
 *       录（第三次快按重新作为新一轮首次按下，不会连续误切）；{@link #reset} 用于清空尚未配对的首按记录。</li>
 * </ul>
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
