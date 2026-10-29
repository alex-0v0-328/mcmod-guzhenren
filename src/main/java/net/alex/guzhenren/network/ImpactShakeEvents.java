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
 * Forwards hard impacts to the wire as camera shakes [震屏]: the gameplay posts, this class sends each player near
 * the impact an {@link ImpactShakePayload} already scaled by distance ({@link #getFalloff}: full at the impact,
 * nothing at the radius).
 *
 * <p>{@link #onPlungeLanded}: the plunger and everyone within {@link #getPlungeRadius} of the landing shake
 * {@link #getPlungeStrength} degrees over {@link #PLUNGE_TICKS}, both growing with the landing's impact power (2 to
 * 6): 16 to 32 blocks, 0.25° to 0.65°. {@link #onSecondStageCrash}: everyone within {@link #CRASH_RADIUS} of a
 * second-stage crash but the flier shakes up to {@link #CRASH_STRENGTH} over {@link #CRASH_TICKS}; the flier's own
 * client shook the moment its physics crashed ({@code net.alex.guzhenren.client.event.ImpactShakes}), a round trip
 * sooner, with the same numbers.
 *
 * @author Alex
 * @version 1.0.0
 * @see ModPayloads
 * @since 1.0.0
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
