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
 * Rolls and shakes the local player's camera in second-stage flight [二段飞行]; client only. Everything turns the
 * camera's angles, never the rendered image, so shader packs see an ordinary camera, and everything scales with the
 * vanilla Distortion Effects slider: at 0 the camera stays still.
 *
 * <p>The roll banks into a turn: the yaw's turn rate, eased over ≈0.1 s ({@link #TURN_EASE}), times
 * {@link #ROLL_PER_TURN} and the throttle, capped at {@link #MAX_ROLL}, the roll itself eased at {@link #ROLL_EASE}
 * and released faster ({@link #RELEASE_EASE}) once the stage ends. The cruise shake starts past
 * {@link #SHAKE_THROTTLE} and reaches {@link #CRUISE_SHAKE} degrees at full throttle, swinging pitch and yaw on two
 * rates near 10 Hz. A crash's shake is {@link ImpactShakes}'s.
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
    private static long lastFrameNanos;
    private static float lastYaw = Float.NaN;
    private static float turnRate;
    private static float roll;

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
        float turned = Float.isNaN(lastYaw) || seconds <= 0.0F ? 0.0F : Mth.wrapDegrees(yaw - lastYaw) / seconds;
        lastYaw = yaw;
        turnRate = Mth.lerp(getEase(TURN_EASE, seconds), turnRate, turned);
        float target = active ? getRollTarget(turnRate, throttle) : 0.0F;
        roll = Mth.lerp(getEase(active ? ROLL_EASE : RELEASE_EASE, seconds), roll, target);

        float shake = getCruiseShake(throttle);
        if (Math.abs(roll) < MIN_ROLL && shake <= 0.0F) return;
        float scale = minecraft.options.screenEffectScale().get().floatValue();
        float time = player.tickCount + partialTick;
        event.setRoll(event.getRoll() + roll * scale);
        event.setPitch(event.getPitch() + shake * scale * Mth.sin(time * PITCH_RATE));
        event.setYaw(event.getYaw() + shake * scale * Mth.cos(time * YAW_RATE));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        lastYaw = Float.NaN;
        turnRate = 0.0F;
        roll = 0.0F;
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
        float seconds = lastFrameNanos == 0L ? 0.0F : (now - lastFrameNanos) / 1.0E9F;
        lastFrameNanos = now;
        return Math.min(seconds, MAX_FRAME_SECONDS);
    }
}
