package net.alex.guzhenren.network;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.flight.SecondStageCrashEvent;
import net.alex.guzhenren.gameplay.plunge.PlungeLandedEvent;
import net.alex.guzhenren.network.payload.ImpactShakePayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 将服务端重击事件转为距离衰减震屏通知（{@link ImpactShakePayload}，经 {@link ModPayloads} 下发）的转发监听器。
 * <ul>
 *   <li>{@link #onPlungeLanded} 响应 {@link PlungeLandedEvent}，按落地冲击力（2 至 6）向半径 {@link #getPlungeRadius}
 *       （16 至 32 格）内的所有玩家（含坠击者）发送强度 {@link #getPlungeStrength}（0.25° 至 0.65°）、持续
 *       {@link #PLUNGE_TICKS} 刻的震屏通知，并由 {@link #getFalloff} 从撞点线性衰减至半径边界的 0。</li>
 *   <li>{@link #onSecondStageCrash} 响应 {@link SecondStageCrashEvent}，向撞毁点 {@link #CRASH_RADIUS} 格内除飞行者外的
 *       所有玩家发送上限 {@link #CRASH_STRENGTH} 度、持续 {@link #CRASH_TICKS} 刻的震屏通知；飞行者本机已在物理撞毁当刻
 *       由客户端直接触发同等强度的本地震屏，以省去一次网络往返延迟。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ImpactShakeEvents {

    private ImpactShakeEvents() {}

    public static final float CRASH_STRENGTH = 0.75F;
    public static final int CRASH_TICKS = 10;
    static final double CRASH_RADIUS = 16.0D;
    static final int PLUNGE_TICKS = 15;
    private static final float PLUNGE_BASE_STRENGTH = 0.25F;
    private static final float PLUNGE_STRENGTH_PER_POWER = 0.1F;
    private static final double PLUNGE_BASE_RADIUS = 8.0D;
    private static final double PLUNGE_RADIUS_PER_POWER = 4.0D;
    private static final float MIN_POWER = 2.0F;
    private static final float MIN_STRENGTH = 0.01F;

    @SubscribeEvent
    public static void onPlungeLanded(PlungeLandedEvent event) {
        float power = event.getImpactPower();
        shake(event.getPlayer().serverLevel(), event.getPosition(), getPlungeStrength(power), getPlungeRadius(power),
                PLUNGE_TICKS, null);
    }

    @SubscribeEvent
    public static void onSecondStageCrash(SecondStageCrashEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        shake(player.serverLevel(), player.position(), CRASH_STRENGTH, CRASH_RADIUS, CRASH_TICKS, player);
    }

    static float getPlungeStrength(float power) {
        return PLUNGE_BASE_STRENGTH + PLUNGE_STRENGTH_PER_POWER * (power - MIN_POWER);
    }

    static double getPlungeRadius(float power) { return PLUNGE_BASE_RADIUS + PLUNGE_RADIUS_PER_POWER * power; }

    static float getFalloff(float strength, double distance, double radius) {
        return distance >= radius ? 0.0F : (float) (strength * (1.0D - distance / radius));
    }

    private static void shake(ServerLevel level, Vec3 origin, float strength, double radius, int ticks,
                              @Nullable ServerPlayer except) {
        for (ServerPlayer viewer : level.players()) {
            if (viewer == except) continue;
            float felt = getFalloff(strength, viewer.position().distanceTo(origin), radius);
            if (felt >= MIN_STRENGTH) PacketDistributor.sendToPlayer(viewer, new ImpactShakePayload(felt, ticks));
        }
    }
}
