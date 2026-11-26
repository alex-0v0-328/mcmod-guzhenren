package net.alex.guzhenren.client.screen.immortalaperture.map;

/**
 * 地图昼夜色调[Map Tint]：客户端维度时间 → RGB 乘数，贴地图贴图时整张相乘，不烘进像素。
 * <ul>
 *   <li>四关键色：昼 {@link #DAY}、黄昏 {@link #DUSK}、夜 {@link #NIGHT}、黎明 {@link #DAWN}；一天内的关键帧（刻）：
 *       1000 与 11000 昼、12500 黄昏、14000 与 22000 夜、23250 黎明，相邻两帧线性插值，跨午夜循环。</li>
 *   <li>昼与夜各有平台段，黄昏、黎明只在过渡中点取满值。</li>
 * </ul>
 */

public record MapTint(float red, float green, float blue) {

    public static final MapTint DAY = new MapTint(1.0F, 1.0F, 1.0F);
    public static final MapTint DUSK = new MapTint(1.0F, 0.70F, 0.48F);
    public static final MapTint NIGHT = new MapTint(0.36F, 0.44F, 0.74F);
    public static final MapTint DAWN = new MapTint(1.0F, 0.78F, 0.66F);
    private static final int DAY_TICKS = 24000;
    private static final int[] KEY_TICKS = { 1000, 11000, 12500, 14000, 22000, 23250, 25000 };
    private static final MapTint[] KEY_TINTS = { DAY, DAY, DUSK, NIGHT, NIGHT, DAWN, DAY };

    public static MapTint at(long dayTime) {
        int tick = (int) Math.floorMod(dayTime, (long) DAY_TICKS);
        if (tick < KEY_TICKS[0]) tick += DAY_TICKS;
        for (int i = 0; i < KEY_TICKS.length - 1; i++) {
            if (tick <= KEY_TICKS[i + 1]) {
                float t = (float) (tick - KEY_TICKS[i]) / (KEY_TICKS[i + 1] - KEY_TICKS[i]);
                return KEY_TINTS[i].mix(KEY_TINTS[i + 1], t);
            }
        }
        return DAY;
    }

    public MapTint mix(MapTint other, float t) {
        return new MapTint(red + (other.red - red) * t, green + (other.green - green) * t,
                blue + (other.blue - blue) * t);
    }

    public int apply(int argb) {
        return argb & 0xFF000000 | scale(argb >> 16, red) << 16 | scale(argb >> 8, green) << 8 | scale(argb, blue);
    }

    private static int scale(int channel, float factor) {
        return Math.min(255, Math.round((channel & 0xFF) * factor));
    }
}
