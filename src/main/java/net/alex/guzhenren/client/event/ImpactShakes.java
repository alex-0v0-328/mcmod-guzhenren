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
 * 本客户端镜头正在播的震屏；仅客户端。
 *
 * <ul>
 *   <li>同 {@link SecondStageCamera}：只转镜头角度，不动画面，随原版“屏幕扭曲效果”滑条缩放。</li>
 *   <li>来源：{@link ImpactShakePayload}（俯冲落地、他人的二段飞行[Second-Stage Flight]撞击，强度已按距离缩放），或本地
 *       物理的 {@link SecondStageCrashEvent}（强度 {@link ImpactShakeEvents#CRASH_STRENGTH}，持续
 *       {@link ImpactShakeEvents#CRASH_TICKS}）。</li>
 *   <li>以强度（度）开场，按剩余时间平方衰减（{@link #getShake}）；重叠相加，上限 {@link #MAX_STRENGTH}；通知数值钳到
 *       {@link #MAX_STRENGTH}、{@link #MAX_TICKS}；俯仰、偏航以两个近 10 Hz 的频率摆动。</li>
 *   <li>时长按本类自己的未暂停客户端 tick 计：重生重置的 tick 计数不重播震屏，暂停即冻结。</li>
 *   <li>唯一可变静态字段 {@code ticks} 是本客户端的 tick 时钟：只增不减，每次震屏在其上记起点，登出无需清零。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ImpactShakeEvents
 * @since 1.0.0
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
