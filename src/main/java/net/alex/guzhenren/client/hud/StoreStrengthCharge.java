package net.alex.guzhenren.client.hud;

/**
 * {@link ChargeHud} 读取的蓄力蛊[Store Strength Gu]蓄力状态：是否在蓄、进度、服务端是否判满。
 * <ul>
 *   <li>唯一写入者为客户端输入层（{@code net.alex.guzhenren.client.input.StoreStrengthInput}），同其推送
 *       {@link SonicBoomHud}；放在此处，蓄力条不必上探输入层；登出经 {@link #reset} 清空。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
