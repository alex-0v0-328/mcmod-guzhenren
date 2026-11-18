package net.alex.guzhenren.particle;

/**
 * 冲刺轨迹（{@link RingConeEmitter}）与音障环轨迹（{@link BarrierRingTrail}）共用的等距落点累加器，用于计算本刻位移线段
 * 上应生成各环的精确偏移。
 * <ul>
 *   <li>因服务端接收动画驱动位移时常呈分块不均匀到达，按实测移动距离分段可保证前摇、跳跃或撞墙截断时环距依然均匀。</li>
 *   <li>{@link #SPACING} 定义冲刺轨迹默认间距（格），音障环则通过重载传入自身间距；{@link #getDrops} 依据距上次落环的累
 *       计距离 {@code carry} 与剩余环数上限 {@code ringsLeft}，返回包含本段升序偏移数组、结余距离与剩余环数的
 *       {@link Drops} 记录（当 {@code carry} 恰等于间距时会在本段起点立即落环）。</li>
 * </ul>
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
