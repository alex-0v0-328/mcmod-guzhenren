package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

/**
 * 一个玩家的换视口或探明限流：每 {@value #WINDOW_TICKS} 刻的固定窗口内至多接受 {@value #LIMIT} 份（视口一个一份，
 * 探明一个区块一份），超出的整包忽略。
 * <ul>
 *   <li>{@link #tryAccept} 传服务端刻数与份数；窗口从本窗口第一次请求算起，过了就重开。退订（空视口）不经过这里。</li>
 * </ul>
 */

final class ViewLimiter {

    static final int LIMIT = 4;
    static final int WINDOW_TICKS = 20;
    private long windowStart = Long.MIN_VALUE;
    private int accepted;

    boolean tryAccept(long now) { return tryAccept(now, 1); }

    boolean tryAccept(long now, int count) {
        if (this.windowStart == Long.MIN_VALUE || now - this.windowStart >= WINDOW_TICKS) {
            this.windowStart = now;
            this.accepted = 0;
        }
        if (this.accepted + count > LIMIT) return false;
        this.accepted += count;
        return true;
    }
}
