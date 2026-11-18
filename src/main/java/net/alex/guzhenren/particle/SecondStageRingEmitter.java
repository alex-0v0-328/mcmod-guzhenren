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
 * 二段飞行[Second-Stage Flight]期间的服务端视觉环发射器（基于 {@link RingConeEmitter#sendRing} 与 {@code RingParticle}
 * 渲染），在进入、高速巡航与撞击时向可见范围内的玩家发送纯视觉反馈。
 * <ul>
 *   <li>{@link #sonicBoom} 响应 {@link SecondStageStartedEvent}：在玩家身体中部（{@link #RING_HEIGHT}）沿视线后方
 *       {@link #BOOM_BEHIND} 格生成一枚面向视线的 {@code sonic_boom_ring}（在第一人称视角后方绽开，飞行者本机改看屏幕
 *       环），以 {@link #BOOM_VOLUME} 音量播放约 32 格可闻的监守者音爆声，并初始化 {@link BarrierRingTrail}。</li>
 *   <li>{@link #tick} 由 {@link SecondStageRingEvents} 在玩家刻末尾调用：在 {@link SecondStageService#isActive} 期间推
 *       进轨迹并沿身体中部发射 {@code barrier_ring}（前 2 秒静默，之后达到 3 倍普通飞行速度时约每 16 格 1 环、满速约每
 *       秒 4 环且每个粒子存活 15 刻并扩散至 4 倍后淡出）；若因切换维度导致轨迹缺失但仍处于飞行态，则就地重建轨迹并重新
 *       静默。</li>
 *   <li>{@link #crash} 响应服务端 {@link SecondStageCrashEvent}：在身体中部沿航向前方 {@link #CRASH_AHEAD} 格贴近撞击面
 *       生成三维朝向航向的 {@code sonic_boom_ring}（垂直俯冲或爬升时由 {@code RingGeometry#getFacing} 处理法线使环面贴
 *       合地面或天花板），撞击音效由弹坑爆炸提供。</li>
 *   <li>飞行结束、玩家登出或切换维度时调用 {@link #forget} 清除轨迹缓存。</li>
 * </ul>
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
