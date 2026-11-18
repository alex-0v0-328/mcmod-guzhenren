package net.alex.guzhenren.client.event;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.flight.SecondStageCrashEvent;
import net.alex.guzhenren.network.ImpactShakeEvents;
import net.alex.guzhenren.network.payload.ImpactShakePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 本客户端镜头正在播放的撞击震屏管理器，仅运行于客户端。
 * <ul>
 *   <li>与 {@link SecondStageCamera} 同样只转动镜头角度而不移动画面，并随原版「屏幕扭曲效果」滑条等比缩放。</li>
 *   <li>震屏来源包括服务端下发的 {@link ImpactShakePayload}（俯冲落地与他人二段飞行[Second-Stage Flight]撞击，强度已按
 *       距离衰减）以及本地物理触发的 {@link SecondStageCrashEvent}（强度 {@link ImpactShakeEvents#CRASH_STRENGTH}，持续
 *       {@link ImpactShakeEvents#CRASH_TICKS}）。</li>
 *   <li>单次震屏以角度强度开场并按剩余时间平方衰减（{@link #getShake}），多源震屏叠加且受 {@link #MAX_STRENGTH} 上限约
 *       束，网络下发值亦钳制在 {@link #MAX_STRENGTH} 与 {@link #MAX_TICKS} 内；俯仰与偏航以两组接近 10 Hz 的频率摆
 *       动。</li>
 *   <li>持续时间按本类自有的未暂停客户端 tick 计数：玩家重生导致的实体 tick 重置不会重播震屏，游戏暂停时自动冻结。</li>
 *   <li>唯一可变静态字段 {@code ticks} 是本客户端单调递增的 tick 时钟，每次震屏在其上记录起点时间，登出无需清零。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class ImpactShakes {

    private ImpactShakes() {}

    static final float MAX_STRENGTH = 2.0F;
    static final int MAX_TICKS = 40;
    private static final float PITCH_RATE = 3.0F;
    private static final float YAW_RATE = 3.75F;
    private static final List<Shake> SHAKES = new ArrayList<>();
    private static long ticks;

    public static void onPayload(ImpactShakePayload payload) { start(payload.strength(), payload.ticks()); }

    @SubscribeEvent
    public static void onSecondStageCrash(SecondStageCrashEvent event) {
        if (event.getPlayer() == Minecraft.getInstance().player) {
            start(ImpactShakeEvents.CRASH_STRENGTH, ImpactShakeEvents.CRASH_TICKS);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) return;
        ticks++;
        SHAKES.removeIf(shake -> ticks - shake.startedAt() >= shake.ticks());
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || SHAKES.isEmpty()) return;

        float partialTick = (float) event.getPartialTick();
        float amount = 0.0F;
        for (Shake shake : SHAKES) {
            amount += getShake((float) (ticks - shake.startedAt()) + partialTick, shake.strength(), shake.ticks());
        }
        amount = Math.min(amount, MAX_STRENGTH) * minecraft.options.screenEffectScale().get().floatValue();
        if (amount <= 0.0F) return;
        float time = player.tickCount + partialTick;
        event.setPitch(event.getPitch() + amount * Mth.sin(time * PITCH_RATE));
        event.setYaw(event.getYaw() + amount * Mth.cos(time * YAW_RATE));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) { SHAKES.clear(); }

    static float getShake(float age, float strength, int ticks) {
        if (age < 0.0F || age >= ticks) return 0.0F;
        float left = 1.0F - age / ticks;
        return strength * left * left;
    }

    private static void start(float strength, int duration) {
        float clamped = Math.min(strength, MAX_STRENGTH);
        int length = Math.min(duration, MAX_TICKS);
        if (clamped > 0.0F && length > 0) SHAKES.add(new Shake(ticks, clamped, length));
    }

    private record Shake(long startedAt, float strength, int ticks) {}
}
