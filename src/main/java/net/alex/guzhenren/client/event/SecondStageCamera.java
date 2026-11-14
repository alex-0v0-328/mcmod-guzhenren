package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 二段飞行[Second-Stage Flight]中本地玩家镜头的侧倾与抖动；仅客户端。
 *
 * <ul>
 *   <li>只转镜头角度，不动画面，光影包看到普通镜头；效果随原版“屏幕扭曲效果”滑条缩放，为 0 时镜头不动。</li>
 *   <li>转弯侧倾：偏航转速经约 0.1 秒平滑（{@link #TURN_EASE}），乘 {@link #ROLL_PER_TURN} 与油门，上限
 *       {@link #MAX_ROLL}；侧倾按 {@link #ROLL_EASE} 平滑，二段结束后以更快的 {@link #RELEASE_EASE} 回正。</li>
 *   <li>巡航抖动：油门过 {@link #SHAKE_THROTTLE} 起抖，满油门 {@link #CRUISE_SHAKE} 度；俯仰、偏航双频近 10 Hz。</li>
 *   <li>撞击震屏归 {@link ImpactShakes}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageEvents
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class SecondStageCamera {

    private SecondStageCamera() {}

    static final float ROLL_PER_TURN = 0.025F;
    static final float MAX_ROLL = 8.0F;
    static final float SHAKE_THROTTLE = 0.6F;
    static final float CRUISE_SHAKE = 0.15F;
    private static final float TURN_EASE = 10.0F;
    private static final float ROLL_EASE = 7.0F;
    private static final float RELEASE_EASE = 15.0F;
    private static final float PITCH_RATE = 3.0F;
    private static final float YAW_RATE = 3.75F;
    private static final float MAX_FRAME_SECONDS = 0.1F;
    private static final float MIN_ROLL = 0.01F;

    private static final class State {

        long lastFrameNanos;
        float lastYaw = Float.NaN;
        float turnRate;
        float roll;
    }

    private static final State STATE = new State();

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        float partialTick = (float) event.getPartialTick();
        float seconds = getFrameSeconds();
        boolean active = player.getAbilities().flying && SecondStageService.isActive(player);
        float throttle = active ? (float) PlayerData.get(player).getSecondStageThrottle() : 0.0F;
        float yaw = player.getViewYRot(partialTick);
        float turned = Float.isNaN(STATE.lastYaw) || seconds <= 0.0F ? 0.0F
                : Mth.wrapDegrees(yaw - STATE.lastYaw) / seconds;
        STATE.lastYaw = yaw;
        STATE.turnRate = Mth.lerp(getEase(TURN_EASE, seconds), STATE.turnRate, turned);
        float target = active ? getRollTarget(STATE.turnRate, throttle) : 0.0F;
        STATE.roll = Mth.lerp(getEase(active ? ROLL_EASE : RELEASE_EASE, seconds), STATE.roll, target);

        float shake = getCruiseShake(throttle);
        if (Math.abs(STATE.roll) < MIN_ROLL && shake <= 0.0F) return;
        float scale = minecraft.options.screenEffectScale().get().floatValue();
        float time = player.tickCount + partialTick;
        event.setRoll(event.getRoll() + STATE.roll * scale);
        event.setPitch(event.getPitch() + shake * scale * Mth.sin(time * PITCH_RATE));
        event.setYaw(event.getYaw() + shake * scale * Mth.cos(time * YAW_RATE));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STATE.lastYaw = Float.NaN;
        STATE.turnRate = 0.0F;
        STATE.roll = 0.0F;
    }

    static float getRollTarget(float turnRate, float throttle) {
        return Mth.clamp(turnRate * ROLL_PER_TURN * throttle, -MAX_ROLL, MAX_ROLL);
    }

    static float getCruiseShake(float throttle) {
        return throttle <= SHAKE_THROTTLE ? 0.0F : CRUISE_SHAKE * (throttle - SHAKE_THROTTLE) / (1.0F - SHAKE_THROTTLE);
    }

    private static float getEase(float rate, float seconds) { return 1.0F - (float) Math.exp(-rate * seconds); }

    private static float getFrameSeconds() {
        long now = System.nanoTime();
        float seconds = STATE.lastFrameNanos == 0L ? 0.0F : (now - STATE.lastFrameNanos) / 1.0E9F;
        STATE.lastFrameNanos = now;
        return Math.min(seconds, MAX_FRAME_SECONDS);
    }
}
