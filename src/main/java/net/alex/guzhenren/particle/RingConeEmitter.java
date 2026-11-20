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
 * 服务端激波环与范围伤害环的发射器（对应客户端粒子渲染见 {@link net.alex.guzhenren.client.particle.RingParticle}），所
 * 有环均在无伤害、无碰撞箱的前提下提供纯视觉反馈。
 * <ul>
 *   <li>本包所有视觉环统一通过 {@link #sendRing} 以数量 0、速度倍率 1.0 发送，使粒子系统直接将速度向量解读为朝向法线
 *       （{@code FACING_MOTION}）；速度仅承载长度为 {@link #RING_NORMAL_DRIFT} 格/刻的法线方向（避免零向量触发
 *       {@code RingGeometry} 平放回退，同时粒子寿命内漂移不足 1/4 格而近似静止）。</li>
 *   <li>冲刺轨迹（{@link #dashCone}）沿实际水平位移通过 {@link RingTrailSpacing} 每 {@link RingTrailSpacing#SPACING} 格
 *       铺设 {@code shockwave_ring}（离地 {@link #DASH_RING_HEIGHT} 格，从起点顺次到终点，最多 {@link #DASH_MAX_RINGS}
 *       个以覆盖约 32 格最长冲刺）；采用实测位移而非固定逐刻发放，可避免因服务端动画位移分块到达导致粒子挤在起止两
 *       端。</li>
 *   <li>玩家同一时间至多存在一个活动 {@link DashBurst}（新触发在持续期间直接跳过，防止窗口期内重复发包重放整条轨迹）；
 *       当环数耗尽、达到 {@link #DASH_MAX_TICKS} 上限、或连续 {@link #DASH_IDLE_LIMIT} 刻水平速度平方低于
 *       {@link #DASH_IDLE_SPEED_SQR} 时自动结束。</li>
 *   <li>范围伤害（{@link #areaDamageRing}）从锥体原点前方 {@link #AREA_DAMAGE_RING_START} 格沿攻击轴线发射一枚
 *       {@code area_damage_ring}，令其在 {@link #AREA_DAMAGE_TRAVEL_TICKS} 刻（恰好等于五帧绽开动画的末帧年龄）抵达锥底
 *       并停下扩散淡出。</li>
 *   <li>玩家登出或切换维度时通过 {@link #forget} 立即丢弃进行中的爆发状态（防止跨维度连线铺环），常规逐刻推进由
 *       {@link RingConeEvents} 在玩家刻末尾调用 {@link #tick} 触发。</li>
 * </ul>
 */

public final class RingConeEmitter {

    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-6D;
    private static final int DASH_MAX_RINGS = 16;
    private static final int DASH_MAX_TICKS = 25;
    private static final int DASH_IDLE_LIMIT = 3;
    private static final double DASH_IDLE_SPEED_SQR = 0.0025D;
    private static final double DASH_RING_HEIGHT = 1.0D;
    private static final double RING_NORMAL_DRIFT = 0.03D;
    public static final int AREA_DAMAGE_TRAVEL_TICKS = 4;
    private static final double AREA_DAMAGE_RING_START = 1.0D;

    private static final Map<UUID, Burst> DASH_BURSTS = new HashMap<>();

    private RingConeEmitter() {}

    public static void dashCone(ServerPlayer player, Vec3 motionDirection) {
        if (DASH_BURSTS.containsKey(player.getUUID())) return;
        if (motionDirection.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) return;
        DASH_BURSTS.put(player.getUUID(), new DashBurst(motionDirection.normalize(),
                DASH_MAX_TICKS, player.position(), 0.0D, DASH_MAX_RINGS, 0));
    }

    public static void areaDamageRing(ServerPlayer player, Vec3 axis, double reach) {
        areaDamageRing(player.serverLevel(), player.getEyePosition(), axis, reach);
    }

    public static void areaDamageRing(ServerLevel level, Vec3 origin, Vec3 axis, double reach) {
        if (axis.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) return;
        Vec3 direction = axis.normalize();
        Vec3 ring = origin.add(direction.scale(AREA_DAMAGE_RING_START));
        Vec3 velocity = direction.scale(getAreaDamageRingSpeed(reach));
        level.sendParticles(ModParticles.AREA_DAMAGE_RING.get(), ring.x, ring.y, ring.z, 0,
                velocity.x, velocity.y, velocity.z, 1.0D);
    }

    /** 范围伤害环从出生处到距原点 {@code reach} 格所需的速度，格/tick。 */
    public static double getAreaDamageRingSpeed(double reach) {
        return (reach - AREA_DAMAGE_RING_START) / AREA_DAMAGE_TRAVEL_TICKS;
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
