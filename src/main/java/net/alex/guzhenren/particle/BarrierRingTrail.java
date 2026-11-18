package net.alex.guzhenren.particle;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.flight.SecondStageCruise;
import net.minecraft.world.phys.Vec3;

/**
 * 单次二段飞行[Second-Stage Flight]期间逐刻生成音障环的纯状态轨迹模型，供 {@link SecondStageRingEmitter} 驱动并可由纯
 * JVM 单元测试直接验证。
 * <ul>
 *   <li>刚进入二段飞行时已有入场音爆环，因此在最初 {@link #QUIET_TICKS} 刻内保持静默；静默期结束后，只要平滑速度达到
 *       {@link #BARRIER_SPEED}（创造模式满速普通飞行 {@link SecondStageCruise#PLAIN_FLIGHT_SPEED} 的 3 倍），由于
 *       {@link #start} 初始自带满间距 {@link #SPACING}，会立即触发首个音障环。</li>
 *   <li>之后每实际三维移动 {@link #SPACING} 格经 {@link RingTrailSpacing} 铺设一环（6 倍飞行速度下约每 5 刻 1 环、每秒
 *       4 环），低于音障速度时既不成环也不累计距离。</li>
 *   <li>每刻位移经 {@link #SMOOTHING} 系数做指数平滑，以抵消服务端按网络包到达顺序处理移动时偶发的一刻两次移动包抖
 *       动。</li>
 *   <li>每次生成的 {@link Ring} 位于轨迹点前缘 {@link #HEAD_LEAD} 格（由发射器抬高至身体中部）并面朝前进方向，使飞行者
 *       穿环而过；单刻位移超过 {@link #TELEPORT_DISTANCE} 视作传送，直接重接轨迹并清零速度以避免跨段铺环。</li>
 * </ul>
 */

record BarrierRingTrail(Vec3 lastPos, double carry, double speed, int age) {

    static final int QUIET_TICKS = 40;
    static final double SPACING = 16.0D;
    static final double BARRIER_SPEED = SecondStageCruise.BARRIER_SPEED;
    static final double HEAD_LEAD = 1.0D;
    static final double TELEPORT_DISTANCE = 16.0D;
    private static final double SMOOTHING = 0.5D;
    private static final double MIN_LENGTH = 1.0E-6D;

    static BarrierRingTrail start(Vec3 position) { return new BarrierRingTrail(position, SPACING, 0.0D, 0); }

    Step step(Vec3 position) {
        int nextAge = Math.min(age + 1, QUIET_TICKS + 1);
        Vec3 segment = position.subtract(lastPos);
        double length = segment.length();
        if (length > TELEPORT_DISTANCE) {
            return new Step(new BarrierRingTrail(position, carry, 0.0D, nextAge), List.of());
        }

        double smoothed = SMOOTHING * speed + (1.0D - SMOOTHING) * length;
        if (nextAge <= QUIET_TICKS || smoothed < BARRIER_SPEED || length < MIN_LENGTH) {
            return new Step(new BarrierRingTrail(position, carry, smoothed, nextAge), List.of());
        }

        Vec3 direction = segment.scale(1.0D / length);
        RingTrailSpacing.Drops drops = RingTrailSpacing.getDrops(SPACING, carry, length, Integer.MAX_VALUE);
        List<Ring> rings = new ArrayList<>();
        for (double offset : drops.offsets()) {
            rings.add(new Ring(lastPos.add(direction.scale(offset + HEAD_LEAD)), direction));
        }
        return new Step(new BarrierRingTrail(position, drops.carry(), smoothed, nextAge), rings);
    }

    record Ring(Vec3 center, Vec3 facing) {}

    record Step(BarrierRingTrail next, List<Ring> rings) {}
}
