package net.alex.guzhenren.gameplay.aperture.pressure;

/**
 * 空窍[Aperture]压力爆炸坑的逐列形状：只依列、半径、种子的纯函数。
 * <ul>
 *   <li>{@link #getColumnJitter} 把列哈希到 {@code [0, 0.1)}；{@link #getColumnRadius} 为半径乘 {@code 0.9} 加扰动，
 *       坑边至多参差一成；同爆炸同坑。</li>
 *   <li>{@link #getColumnFloorY} 为该列球底下一格；坑底按此深度分层：冰 0 以下浮冰、-32 以下蓝冰；金 0 以下深层金矿石、
 *       32 以下金矿石、以上金块。</li>
 *   <li>熔岩、金、细雪各用己盐哈希列，互不重合；雪化环带伸出坑半径 8 至 16 格，至多 {@link #getRingReach}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AperturePressureExplosionTask
 * @since 1.0.0
 */

final class CraterShape {

    private CraterShape() {}

    private static final double NOISE_FLOOR = 0.9D;
    private static final double NOISE_RANGE = 0.1D;
    private static final int RING_MIN = 8;
    private static final double RING_NOISE_RANGE = 8.0D;
    private static final double LAVA_SHARE = 0.025D;
    private static final double GOLD_SHARE = 0.0125D;
    private static final double POWDER_SNOW_SHARE = 0.05D;
    private static final int BLUE_ICE_THRESHOLD = -32;
    private static final int GOLD_ORE_THRESHOLD = 32;

    static double getColumnJitter(int blockX, int blockZ, long seed) {
        long hash = blockX * 0x9E3779B97F4A7C15L ^ blockZ * 0xC2B2AE3D27D4EB4FL ^ seed;
        hash ^= hash >>> 30;
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= hash >>> 27;
        hash *= 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (hash & 0xFFFFFL) * (NOISE_RANGE / 0x10_0000L);
    }

    static double getMinColumnRadius(int radius) { return radius * NOISE_FLOOR; }

    static double getColumnRadius(int radius, int blockX, int blockZ, long seed) {
        return radius * (NOISE_FLOOR + getColumnJitter(blockX, blockZ, seed));
    }

    static int getColumnFloorY(double centerY, double columnRadius, double horizontalSquared) {
        return (int) Math.ceil(centerY - Math.sqrt(columnRadius * columnRadius - horizontalSquared) - 0.5D) - 1;
    }

    static int getIceTier(int floorY) {
        if (floorY < BLUE_ICE_THRESHOLD) return 2;
        return floorY < 0 ? 1 : 0;
    }

    static int getGoldTier(int floorY) {
        if (floorY < 0) return 0;
        return floorY < GOLD_ORE_THRESHOLD ? 1 : 2;
    }

    static boolean isLavaColumn(int blockX, int blockZ, long seed) {
        return getColumnJitter(blockX, blockZ, seed ^ 0x5DEECE66DL) < LAVA_SHARE;
    }

    static boolean isGoldColumn(int blockX, int blockZ, long seed) {
        return getColumnJitter(blockX, blockZ, seed ^ 0x2545F4914F6CDD1DL) < GOLD_SHARE;
    }

    static boolean isPowderSnowColumn(int blockX, int blockZ, long seed) {
        return getColumnJitter(blockX, blockZ, seed ^ 0x9E3779B9L) >= POWDER_SNOW_SHARE;
    }

    static double getRingOuterRadius(int radius, int blockX, int blockZ, long seed) {
        return radius + RING_MIN
                + getColumnJitter(blockX, blockZ, seed ^ 0xC2B2AE3DL) / NOISE_RANGE * RING_NOISE_RANGE;
    }

    static int getRingReach(int radius) { return radius + RING_MIN + (int) Math.ceil(RING_NOISE_RANGE); }
}
