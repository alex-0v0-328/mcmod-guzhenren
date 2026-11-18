package net.alex.guzhenren.gameplay.plunge;

/**
 * 坠击落地的伤害、炸坑威力与表现强度纯函数（由 {@link PlungeService} 调用，支持裸 JVM 单元测试）。
 * <ul>
 *   <li>总下落高度 {@link #getHeight} = 起砸前已累计的坠落距离 + 起砸后的高度差；自身摔伤 {@link #getDamage} 为原版
 *       1.21 摔落伤害公式 {@code ceil((height - safeFallDistance) * fallDamageMultiplier)} 的
 *       {@link #DAMAGE_MULTIPLIER}（3）倍。</li>
 *   <li>炸坑爆炸威力 {@link #getCraterPower}：下落不足 {@link #CRATER_MIN_HEIGHT}（4 格）时为 {@code 0}，满 4 格从
 *       {@link #MIN_POWER} 起步，每多下落 {@link #POWER_STEP}（15 格）威力 {@code +1}，至 64 格封顶于
 *       {@link #MAX_POWER}（6.0，等同闪电苦力怕）。</li>
 *   <li>落地视觉表现强度 {@link #getImpactPower}（驱动地裂半径与震屏幅度）：下落不足 {@link #IMPACT_MIN_HEIGHT}（3 格）
 *       时为 {@code 0}，3 至 4 格之间取 {@link #MIN_POWER}（已受摔伤但不挖坑），4 格及以上与炸坑威力一致（即便关闭地形
 *       破坏也照常播放表现）。</li>
 * </ul>
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
