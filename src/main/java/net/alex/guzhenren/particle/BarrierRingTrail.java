package net.alex.guzhenren.particle;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.flight.SecondStageCruise;
import net.minecraft.world.phys.Vec3;

/**
 * 单次二段飞行[Second-Stage Flight]逐 tick 生成音障环的轨迹状态；纯状态，纯 JVM 测试可钉住。
 * <ul>
 *   <li>{@link SecondStageRingEmitter} 发送 {@link #step} 的返回值。</li>
 *   <li>进入时已有音爆环，故 {@link #QUIET_TICKS} 内静默；之后速度超 {@link #BARRIER_SPEED}（创造满速普通飞行
 *       {@link SecondStageCruise#PLAIN_FLIGHT_SPEED} 的 3 倍）立即成环，因新轨迹（{@link #start}）自带满间距。</li>
 *   <li>此后每实际三维移动 {@link #SPACING} 格一环，与冲刺轨迹同经 {@link RingTrailSpacing}；低于音障速度不成环、
 *       不计距离。</li>
 *   <li>速度取每 tick 位移经 {@link #SMOOTHING} 平滑：服务端按到达顺序收玩家移动，有时一 tick 两次。</li>
 *   <li>{@link Ring} 生成于路径脚部高度、移动前缘前 {@link #HEAD_LEAD} 格，面朝移动方向；飞行者穿环而过。</li>
 *   <li>单 tick 位移超 {@link #TELEPORT_DISTANCE} 视为传送：落点重接轨迹、速度清零，跳跃路径不铺环。</li>
 *   <li>6 倍飞行速度（约 3.3 格/tick）约每 5 tick 一环，每秒 4 个。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageRingEmitter
 * @since 1.0.0
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
