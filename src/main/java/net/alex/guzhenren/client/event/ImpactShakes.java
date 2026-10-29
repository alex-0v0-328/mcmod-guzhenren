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
 * The impact shakes [震屏] this client's camera is playing; client only. Like {@link SecondStageCamera} it turns the
 * camera's angles, never the rendered image, and scales with the vanilla Distortion Effects slider.
 *
 * <p>A shake starts from an {@link ImpactShakePayload} (a plunge's landing, someone else's second-stage crash), its
 * strength already scaled by the distance, or straight from the local physics' {@link SecondStageCrashEvent}, with
 * {@link ImpactShakeEvents#CRASH_STRENGTH} over {@link ImpactShakeEvents#CRASH_TICKS}. Each opens at its strength in
 * degrees and fades on the square of the time left ({@link #getShake}); overlapping shakes add up to
 * {@link #MAX_STRENGTH}. Ages count on this class's own unpaused client ticks, so a respawn's fresh tick count never
 * replays one, and a pause holds them. A notice's numbers are clamped to {@link #MAX_STRENGTH} and
 * {@link #MAX_TICKS}. Pitch and yaw swing on two rates near 10 Hz.
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
