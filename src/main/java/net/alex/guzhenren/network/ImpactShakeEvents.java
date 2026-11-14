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
 * 把重击转发上线为震屏。
 * <ul>
 *   <li>玩法侧发事件，本类向附近玩家发按距离衰减的 {@link ImpactShakePayload}（{@link #getFalloff}：撞点满、半径处
 *       零）；{@link #onPlungeLanded}：坠击者及落点 {@link #getPlungeRadius} 内所有人震 {@link #getPlungeStrength} 度，
 *       持续 {@link #PLUNGE_TICKS}；均随落地冲击力（2 到 6）增长：16 到 32 格、0.25° 到 0.65°。</li>
 *   <li>{@link #onSecondStageCrash}：二段飞行[Second-Stage Flight]撞毁处 {@link #CRASH_RADIUS} 内除飞行者外最多震
 *       {@link #CRASH_STRENGTH}，持续 {@link #CRASH_TICKS}；飞行者本机在物理撞毁时已震
 *       （{@code net.alex.guzhenren.client.event.ImpactShakes}），早一个往返，数值相同。</li>
 * </ul>
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
