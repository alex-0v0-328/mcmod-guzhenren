package net.alex.guzhenren.gameplay.plunge;

/**
 * 坠击落地的纯数值（纯 JVM 测试可钉），由 {@link PlungeService} 施加。
 * <ul>
 *   <li>高度 {@link #getHeight} 为整段坠落：开始时已坠距离 + 此后落差；{@link #getDamage} 为原版 1.21 摔落伤害
 *       {@code ceil((height - safe fall distance) * fall damage multiplier)} × {@link #DAMAGE_MULTIPLIER}，两属性取自玩
 *       家（{@code PlungeService#land}）。</li>
 *   <li>{@link #getCraterPower}：坑的原版爆炸威力；低于 {@link #CRATER_MIN_HEIGHT} 为 0，达到为 {@link #MIN_POWER}，每
 *       {@link #POWER_STEP} 格 +1，64 格起封顶 {@link #MAX_POWER}（闪电苦力怕的 6：泥土约 5 格宽的坑，石头小得多，按原
 *       版爆炸抗性）。</li>
 *   <li>{@link #getImpactPower}：落地表现强度（地裂半径、震屏幅度）；低于 {@link #IMPACT_MIN_HEIGHT} 为 0，4 格起取坑威
 *       力，其间取 {@link #MIN_POWER}；3–4 格已摔伤（原版安全距离 3）但不挖坑，关闭破坏时也不成坑，表现照出。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PlungeService
 * @since 1.0.0
 */

public final class PlungeImpact {

    private PlungeImpact() {}

    public static final int DAMAGE_MULTIPLIER = 3;
    public static final double CRATER_MIN_HEIGHT = 4.0D;
    public static final double IMPACT_MIN_HEIGHT = 3.0D;
    static final float MIN_POWER = 2.0F;
    static final float MAX_POWER = 6.0F;
    static final double POWER_STEP = 15.0D;

    public static double getHeight(double topY, double landingY) { return Math.max(0.0D, topY - landingY); }

    public static int getDamage(double height, double safeFallDistance, double fallDamageMultiplier) {
        int vanilla = (int) Math.ceil((height - safeFallDistance) * fallDamageMultiplier);
        return Math.max(0, vanilla) * DAMAGE_MULTIPLIER;
    }

    public static float getImpactPower(double height) {
        if (height < IMPACT_MIN_HEIGHT) return 0.0F;
        return Math.max(MIN_POWER, getCraterPower(height));
    }

    public static float getCraterPower(double height) {
        if (height < CRATER_MIN_HEIGHT) return 0.0F;
        return (float) Math.min(MAX_POWER, MIN_POWER + (height - CRATER_MIN_HEIGHT) / POWER_STEP);
    }
}
