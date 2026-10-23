package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.flight.FlightService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The local player's burst [疾飞] beyond its speed, which {@code net.alex.guzhenren.mixin.PlayerMixin} and
 * {@code LocalPlayerMixin} scale.
 *
 * <p>{@link #onClientTick} jumps the way vanilla's creative double jump does when a burst starts on the ground.
 * It runs before the player ticks and after the tick's packets, so it sees the synced burst mark and the abilities
 * that turned flying on together, and the player leaves the ground before the landing check at the end of
 * {@code LocalPlayer#aiStep} could end the flight.
 *
 * <p>{@link #onComputeFovModifier} widens the view while the local player flies a burst, multiplying the vanilla
 * modifier (already widened by flying and sprinting) by {@link #getFovFactor}: {@link #FOV_WIDENING} more, eased
 * toward none by the vanilla FOV Effects slider the same way Forge eases the vanilla modifier, so a player who
 * turned FOV effects off sees no widening here either. Vanilla eases the view toward each new modifier over a few
 * ticks, so it widens and narrows smoothly; Minecraft caps the total widening, sprint and flight included.
 *
 * <p>{@link #onLoggingOut} clears {@link #wasBursting}, so a burst that was running when the player left does not
 * make the next world's first burst look like a continuation.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.flight.FlightService
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class BurstEvents {

    private BurstEvents() {}

    private static final double FOV_WIDENING = 0.15D;
    private static boolean wasBursting;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        LocalPlayer player = Minecraft.getInstance().player;
        boolean bursting = player != null && FlightService.isBursting(player);
        if (bursting && !wasBursting && player.onGround()) player.jumpFromGround();
        wasBursting = bursting;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) { wasBursting = false; }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = event.getPlayer();
        if (player == minecraft.player && player.getAbilities().flying && FlightService.isBursting(player)) {
            event.setNewFovModifier(event.getNewFovModifier()
                    * getFovFactor(minecraft.options.fovEffectScale().get()));
        }
    }

    public static float getFovFactor(double effectScale) {
        return (float) (1.0D + FOV_WIDENING * effectScale);
    }
}
