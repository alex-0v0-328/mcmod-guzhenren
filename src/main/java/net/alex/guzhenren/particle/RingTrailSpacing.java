package net.alex.guzhenren.particle;

/**
 * 冲刺轨迹与音障环共用的等距累加器：算下一批环在本 tick 移动线段上的落点。
 * <ul>
 *   <li>按距离而非 tick 铺环：服务端闪避位移成块不均到达（Epic Fight 锁住移动、由动画驱动），逐 tick 铺会挤在路径
 *       两端；按实测移动则前摇、跳跃、撞墙截断都均匀。</li>
 *   <li>{@link #SPACING}：冲刺路径相邻两环间距（格）；音障环传入自己的间距（{@code BarrierRingTrail}）。</li>
 *   <li>{@link #getDrops} 的 {@code carry}：距上次落环已移距离，小于间距，本线段起点就该落环时恰等于间距；
 *       {@code ringsLeft}：本次爆发剩余环数；{@link Drops} 另带新 carry 与剩余环数。</li>
 *   <li>{@link Drops#offsets}：本 tick 线段上的升序落环偏移。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

final class RingTrailSpacing {

    private RingTrailSpacing() {}

    static final double SPACING = 2.0D;

    static Drops getDrops(double carry, double segLen, int ringsLeft) {
        return getDrops(SPACING, carry, segLen, ringsLeft);
    }

    static Drops getDrops(double spacing, double carry, double segLen, int ringsLeft) {
        double along = spacing - carry;
        int count = 0;
        while (count < ringsLeft && along + count * spacing <= segLen) count++;
        double[] offsets = new double[count];
        for (int i = 0; i < count; i++) offsets[i] = along + i * spacing;
        double newCarry = count == 0 ? carry + segLen : segLen - (along + (count - 1) * spacing);
        return new Drops(offsets, newCarry, ringsLeft - count);
    }

    record Drops(double[] offsets, double carry, int ringsLeft) {}
}
