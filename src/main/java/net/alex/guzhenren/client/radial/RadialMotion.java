package net.alex.guzhenren.client.radial;

/**
 * 扇形选择器的动画。
 *
 * <ul>
 *   <li>以毫秒为参数的纯函数，纯 JUnit 可钉住时间线；{@link #mix} 与 {@link #fade} 按通道处理 ARGB 颜色。</li>
 *   <li>打开：环从 {@link #OPEN_FROM} 长到全尺寸并轻微过冲（{@link #getOpenScale}），同时淡入
 *       （{@link #getOpenAlpha}）；扇区顺时针依次晚 {@link #STAGGER_MS} 出现（{@link #getAppear}）。</li>
 *   <li>悬停权重指向缓至 1、离开回 0（{@link #approach}）；名称 {@link #LABEL_MS} 内升起（{@link #getLabelIn}）。</li>
 *   <li>选中离场：被选扇区闪白（{@link #getFlash}），环胀到 {@link #PICK_TO}，短停后淡出；取消离场：环缩到
 *       {@link #CANCEL_TO} 并淡出（{@link #getExitScale}、{@link #getExitAlpha}、{@link #isExitDone}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see RadialMenu
 * @since 1.0.0
 */

public final class RadialMotion {

    public static final int OPEN_MS = 140;
    public static final int FADE_IN_MS = 100;
    public static final int STAGGER_MS = 30;
    public static final int APPEAR_MS = 120;
    public static final int LABEL_MS = 100;
    public static final int FLASH_MS = 90;
    public static final int PICK_GROW_MS = 180;
    public static final int PICK_HOLD_MS = 60;
    public static final int PICK_FADE_MS = 160;
    public static final int CANCEL_MS = 120;
    public static final float OPEN_FROM = 0.6F;
    public static final float PICK_TO = 1.08F;
    public static final float CANCEL_TO = 0.8F;
    private static final double HOVER_RATE = 14.0D;
    private static final double BACK = 1.70158D;

    private RadialMotion() {}

    public static float getOpenScale(long ms) {
        return OPEN_FROM + (1.0F - OPEN_FROM) * easeOutBack(getProgress(ms, OPEN_MS));
    }

    public static float getOpenAlpha(long ms) { return easeOutCubic(getProgress(ms, FADE_IN_MS)); }

    public static float getAppear(long ms, int index) {
        return easeOutCubic(getProgress(ms - (long) index * STAGGER_MS, APPEAR_MS));
    }

    public static float getLabelIn(long ms) { return easeOutCubic(getProgress(ms, LABEL_MS)); }

    public static float getFlash(long ms) { return 1.0F - getProgress(ms, FLASH_MS); }

    public static float getExitScale(long ms, boolean picked) {
        return picked ? 1.0F + (PICK_TO - 1.0F) * easeOutCubic(getProgress(ms, PICK_GROW_MS))
                : 1.0F - (1.0F - CANCEL_TO) * easeOutCubic(getProgress(ms, CANCEL_MS));
    }

    public static float getExitAlpha(long ms, boolean picked) {
        return picked ? 1.0F - getProgress(ms - PICK_HOLD_MS, PICK_FADE_MS) : 1.0F - getProgress(ms, CANCEL_MS);
    }

    public static boolean isExitDone(long ms, boolean picked) {
        return ms >= (picked ? PICK_HOLD_MS + PICK_FADE_MS : CANCEL_MS);
    }

    public static float approach(float current, float target, float seconds) {
        return current + (target - current) * (float) Math.min(1.0D, seconds * HOVER_RATE);
    }

    public static int mix(int from, int to, float amount) {
        int color = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int a = from >>> shift & 0xFF;
            int b = to >>> shift & 0xFF;
            color |= Math.round(a + (b - a) * amount) << shift;
        }
        return color;
    }

    public static int fade(int color, float alpha) {
        return Math.round((color >>> 24) * alpha) << 24 | color & 0xFFFFFF;
    }

    static float getProgress(long ms, int duration) {
        return ms <= 0L ? 0.0F : Math.min(1.0F, ms / (float) duration);
    }

    private static float easeOutCubic(float x) {
        float rest = 1.0F - x;
        return 1.0F - rest * rest * rest;
    }

    private static float easeOutBack(float x) {
        double rest = x - 1.0D;
        return (float) (1.0D + (BACK + 1.0D) * rest * rest * rest + BACK * rest * rest);
    }
}
