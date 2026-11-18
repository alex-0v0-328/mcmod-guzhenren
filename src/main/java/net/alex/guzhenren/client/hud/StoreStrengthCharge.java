package net.alex.guzhenren.client.hud;

/**
 * 蓄力蛊[Store Strength Gu]本地蓄力状态容器：记录是否正在蓄力、当前进度与服务端判满状态，供 {@link ChargeHud} 读取。
 * <ul>
 *   <li>唯一写入方为客户端输入层（{@code net.alex.guzhenren.client.input.StoreStrengthInput}），类似
 *       {@link SonicBoomHud} 的信号推送模式，使 HUD 层无需向上依赖输入层；玩家登出时通过 {@link #reset} 清空状态。</li>
 * </ul>
 */

public final class StoreStrengthCharge {

    public static final StoreStrengthCharge INSTANCE = new StoreStrengthCharge();

    private StoreStrengthCharge() {}

    private boolean charging;
    private boolean full;
    private float progress;

    public boolean isCharging() { return charging; }

    public boolean isFull() { return full; }

    public float getFilled() { return progress; }

    public float getDisplayProgress() { return full ? 1.0F : progress; }

    public void start() {
        charging = true;
        progress = 0.0F;
    }

    public void setProgress(float progress) { this.progress = progress; }

    public void markFull() { full = true; }

    public void reset() {
        charging = false;
        full = false;
        progress = 0.0F;
    }
}
