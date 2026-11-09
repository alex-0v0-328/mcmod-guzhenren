package net.alex.guzhenren.client.hud;

/**
 * What {@link ChargeHud} shows of the Store Strength Gu's [蓄力蛊] charge [蓄力]: whether one is running, how far the
 * bar has filled, and whether the server holds it full. The client's input layer
 * ({@code net.alex.guzhenren.client.input.StoreStrengthInput}) is the only writer, the way it pushes into
 * {@link SonicBoomHud}; keeping the state here lets the bar read it without reaching up into the input layer.
 * Forgotten on logout through {@link #reset}.
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
