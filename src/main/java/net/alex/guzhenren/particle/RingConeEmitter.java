package net.alex.guzhenren.particle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 激波环背后的服务端锥形轨迹：每环在出生处原地由小到大绽开；纯视觉，无伤害、无碰撞箱。
 * <ul>
 *   <li>本包所有环经 {@link #sendRing} 发出：count 0、speed 1.0，精确速度交给粒子，环亦读作朝向法线
 *       （{@code FACING_MOTION}）；速度只承载法线（{@link #RING_NORMAL_DRIFT}，格/tick）：零向量触发
 *       {@code RingGeometry} 无方向回退、环被放平；寿命内漂移不足 1/4 格，看似静止。</li>
 *   <li>今后速度感、力量感动作调 {@code dashCone}（或新增同类入口），不直接生成环。</li>
 *   <li>{@link #dashCone} 沿路径每实际移动 {@link RingTrailSpacing#SPACING} 格一环，起点先现、终点最后，至多
 *       {@link #DASH_MAX_RINGS} 个（约 24 格路径，冲撞蛊[Crash Gu]最长冲刺）。</li>
 *   <li>按实测移动而非逐 tick：服务端闪避位移成块不均到达（Epic Fight 锁住移动、由动画驱动），逐 tick 则环挤
 *       两端；按距离则前摇、跳跃、撞墙截断都均匀；{@link DashBurst} 把每环摆在本 tick 移动线段上。</li>
 *   <li>{@link DashBurst} 在环数用尽、{@link #DASH_MAX_TICKS} 安全上限耗尽，或连续 {@link #DASH_IDLE_LIMIT} tick
 *       水平速度平方低于 {@link #DASH_IDLE_SPEED_SQR} 时结束；0.05 格/tick 算静止，闪避前摇比该时长短。</li>
 *   <li>{@link #DASH_RING_HEIGHT}：冲刺环离脚高度，即站立环身体中部（最大半径 1.1）。</li>
 *   <li>{@link #DASH_BURSTS} 每名玩家只存一个活动爆发：存活时重触发（窗口内第二个 payload）会重放整条轨迹，
 *       故窗口内冲刺直接跳过、不替换；{@link Burst} 的 {@code tick} 铺本 tick 的环并返回下一状态，耗尽返回 null。</li>
 *   <li>登出或换维度丢弃爆发（{@link #forget}）：爆发在单一维度内量距离，带入另一维度会沿两点连线铺环；
 *       {@link #tick} 由 {@code RingConeEvents} 在每个玩家 tick 末调用，表空即返回。</li>
 *   <li>{@link #areaImpactRing} 是范围冲击的同类入口：一个 {@code area_impact_ring}，生成在锥体原点（拳击为眼睛，
 *       远程拳爆开为爆点）前 {@link #AREA_IMPACT_RING_START} 格，沿轴朝向。</li>
 *   <li>环带 {@link #AREA_IMPACT_TRAVEL_TICKS} tick 内抵达锥末的速度，到达后停下、扩大并淡出；该 tick 数等于绽开时长
 *       （五帧，末帧 age 4），环恰在绽开结束时停下，由 {@code RingGeometryTest} 钉住。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.particle.RingParticle
 * @since 1.0.0
 */

public final class RingConeEmitter {

    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-6D;
    private static final int DASH_MAX_RINGS = 12;
    private static final int DASH_MAX_TICKS = 25;
    private static final int DASH_IDLE_LIMIT = 3;
    private static final double DASH_IDLE_SPEED_SQR = 0.0025D;
    private static final double DASH_RING_HEIGHT = 1.0D;
    private static final double RING_NORMAL_DRIFT = 0.03D;
    public static final int AREA_IMPACT_TRAVEL_TICKS = 4;
    private static final double AREA_IMPACT_RING_START = 1.0D;

    private static final Map<UUID, Burst> DASH_BURSTS = new HashMap<>();

    private RingConeEmitter() {}

    public static void dashCone(ServerPlayer player, Vec3 motionDirection) {
        if (DASH_BURSTS.containsKey(player.getUUID())) return;
        if (motionDirection.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) return;
        DASH_BURSTS.put(player.getUUID(), new DashBurst(motionDirection.normalize(),
                DASH_MAX_TICKS, player.position(), 0.0D, DASH_MAX_RINGS, 0));
    }

    public static void areaImpactRing(ServerPlayer player, Vec3 axis, double reach) {
        areaImpactRing(player.serverLevel(), player.getEyePosition(), axis, reach);
    }

    public static void areaImpactRing(ServerLevel level, Vec3 origin, Vec3 axis, double reach) {
        if (axis.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) return;
        Vec3 direction = axis.normalize();
        Vec3 ring = origin.add(direction.scale(AREA_IMPACT_RING_START));
        Vec3 velocity = direction.scale(getAreaImpactRingSpeed(reach));
        level.sendParticles(ModParticles.AREA_IMPACT_RING.get(), ring.x, ring.y, ring.z, 0,
                velocity.x, velocity.y, velocity.z, 1.0D);
    }

    /** 范围冲击环从出生处到距原点 {@code reach} 格所需的速度，格/tick。 */
    public static double getAreaImpactRingSpeed(double reach) {
        return (reach - AREA_IMPACT_RING_START) / AREA_IMPACT_TRAVEL_TICKS;
    }

    public static void tick(ServerPlayer player) {
        if (DASH_BURSTS.isEmpty()) return;
        if (player.isRemoved() || player.isDeadOrDying()
                || !(player.level() instanceof ServerLevel level)) {
            forget(player);
            return;
        }
        tickBurst(DASH_BURSTS, player, level);
    }

    public static void forget(Player player) {
        DASH_BURSTS.remove(player.getUUID());
    }

    /** 在给定点发一个环，面朝 {@code facing}，朝向由漂移速度承载。 */
    static void sendRing(ServerLevel level, ParticleOptions ring, double x, double y, double z, Vec3 facing) {
        Vec3 velocity = facing.scale(RING_NORMAL_DRIFT);
        level.sendParticles(ring, x, y, z, 0, velocity.x, velocity.y, velocity.z, 1.0D);
    }

    private static void tickBurst(Map<UUID, Burst> bursts, ServerPlayer player, ServerLevel level) {
        Burst burst = bursts.get(player.getUUID());
        if (burst == null) return;
        Burst next = burst.tick(player, level);
        if (next == null) bursts.remove(player.getUUID());
        else bursts.put(player.getUUID(), next);
    }

    private interface Burst {

        Burst tick(ServerPlayer player, ServerLevel level);
    }

    private record DashBurst(Vec3 direction, int ticksLeft, Vec3 lastPos, double carry,
                             int ringsLeft, int idleTicks) implements Burst {

        @Override
        public Burst tick(ServerPlayer player, ServerLevel level) {
            if (ticksLeft <= 0 || ringsLeft <= 0 || idleTicks >= DASH_IDLE_LIMIT) return null;
            Vec3 position = player.position();
            Vec3 segment = position.subtract(lastPos);
            double segLen = Math.sqrt(segment.x * segment.x + segment.z * segment.z);
            int idle = segLen * segLen < DASH_IDLE_SPEED_SQR ? idleTicks + 1 : 0;

            double newCarry = carry;
            int newRingsLeft = ringsLeft;
            if (segLen > 0.0D) {
                RingTrailSpacing.Drops drops = RingTrailSpacing.getDrops(carry, segLen, ringsLeft);
                if (drops.offsets().length > 0) {
                    Vec3 segDir = new Vec3(segment.x, 0.0D, segment.z).normalize();
                    for (double offset : drops.offsets()) {
                        Vec3 anchor = lastPos.add(segDir.scale(offset));
                        sendRing(level, ModParticles.SHOCKWAVE_RING.get(), anchor.x, player.getY() + DASH_RING_HEIGHT,
                                anchor.z, direction);
                    }
                }
                newCarry = drops.carry();
                newRingsLeft = drops.ringsLeft();
            }
            return new DashBurst(direction, ticksLeft - 1, position, newCarry, newRingsLeft, idle);
        }
    }
}
