package net.alex.guzhenren.particle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 二段飞行[Second-Stage Flight]的服务端环：进入时音爆环、高速时音障环、撞击时撞击环。
 * <ul>
 *   <li>服务端发送，粒子范围内玩家都看得到；纯反馈，无伤害、无碰撞箱。</li>
 *   <li>{@link #sonicBoom} 响应每个 {@code SecondStageStartedEvent}（{@link SecondStageRingEvents}）：一个
 *       {@code sonic_boom_ring}，绽开到约 4 格，面朝视线，位于身体中部（{@link #RING_HEIGHT}），沿视线后退
 *       {@link #BOOM_BEHIND} 格，在飞行者第一人称镜头后绽开；飞行者改看屏幕环
 *       （{@code net.alex.guzhenren.client.hud.SonicBoomHud}）；就地开始 {@link BarrierRingTrail}。</li>
 *   <li>同时以 {@link #BOOM_VOLUME} 播放监守者音爆，约 32 格外可闻，同环的发送距离。</li>
 *   <li>{@link #tick} 在 {@link SecondStageService#isActive} 时推进轨迹，每成一环在身体中部发 {@code barrier_ring}；
 *       头 2 秒不成环，之后保持普通飞行 3 倍以上时约每 16 格一环。</li>
 *   <li>环就地由小到大绽开，再扩到 4 倍并淡出（{@code RingParticle} 扩散退场）；满速每秒 4 环、每环存活 15 tick，
 *       每名飞行者身后约同时 3 个。</li>
 *   <li>开启时轨迹缺失（换维度已丢弃）则就地重开、重新静默；轨迹随二段飞行结束，登出、换维度时丢弃
 *       （{@link #forget}）；{@link SecondStageRingEvents} 在每个玩家 tick 末调用 {@link #tick}。</li>
 *   <li>{@link #crash} 响应每个服务端 {@code SecondStageCrashEvent}：身体中部贴撞击面（沿航向 {@link #CRASH_AHEAD}
 *       格）发 {@code sonic_boom_ring}，三维面朝航向；俯冲平铺地面、爬升平贴天花板（{@code RingGeometry#getFacing}
 *       处理竖直法线）；撞击声来自撞坑爆炸。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.particle.RingParticle
 * @see RingConeEmitter
 * @since 1.0.0
 */

public final class SecondStageRingEmitter {

    private static final double RING_HEIGHT = 1.0D;
    private static final double BOOM_BEHIND = 1.0D;
    private static final float BOOM_VOLUME = 2.0F;
    private static final float BOOM_PITCH = 1.0F;
    private static final double CRASH_AHEAD = 0.5D;

    private static final Map<UUID, BarrierRingTrail> TRAILS = new HashMap<>();

    private SecondStageRingEmitter() {}

    public static void sonicBoom(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Vec3 look = player.getLookAngle();
        Vec3 ring = player.position().add(0.0D, RING_HEIGHT, 0.0D).subtract(look.scale(BOOM_BEHIND));
        RingConeEmitter.sendRing(level, ModParticles.SONIC_BOOM_RING.get(), ring.x, ring.y, ring.z, look);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, BOOM_VOLUME, BOOM_PITCH);
        TRAILS.put(player.getUUID(), BarrierRingTrail.start(player.position()));
    }

    public static void crash(ServerPlayer player, Vec3 heading) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Vec3 ring = player.position().add(0.0D, RING_HEIGHT, 0.0D).add(heading.scale(CRASH_AHEAD));
        RingConeEmitter.sendRing(level, ModParticles.SONIC_BOOM_RING.get(), ring.x, ring.y, ring.z, heading);
    }

    public static void tick(ServerPlayer player) {
        if (!SecondStageService.isActive(player) || player.isRemoved() || player.isDeadOrDying()
                || !(player.level() instanceof ServerLevel level)) {
            TRAILS.remove(player.getUUID());
            return;
        }
        BarrierRingTrail trail = TRAILS.get(player.getUUID());
        if (trail == null) {
            TRAILS.put(player.getUUID(), BarrierRingTrail.start(player.position()));
            return;
        }
        BarrierRingTrail.Step step = trail.step(player.position());
        for (BarrierRingTrail.Ring ring : step.rings()) {
            RingConeEmitter.sendRing(level, ModParticles.BARRIER_RING.get(), ring.center().x,
                    ring.center().y + RING_HEIGHT, ring.center().z, ring.facing());
        }
        TRAILS.put(player.getUUID(), step.next());
    }

    public static void forget(Player player) { TRAILS.remove(player.getUUID()); }
}
