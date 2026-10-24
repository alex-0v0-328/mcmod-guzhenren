package net.alex.guzhenren.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.flight.FlightService;
import net.alex.guzhenren.network.ModNetwork;
import net.alex.guzhenren.network.payload.ToggleBurstPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * The burst [疾飞] toggle on the client: double-tapping the sprint key sends {@link ToggleBurstPayload}.
 *
 * <p>Presses come from the raw key and mouse events, matched against whatever the sprint key is bound to, because
 * the key mapping itself only tells whether the key is down and, with Sprint set to Toggle, not even that.
 * {@link #DOUBLE_TAP_TICKS} is vanilla's window for its own double taps (forward to sprint, jump to fly), counted in
 * client ticks ({@link #ticks}). Presses with a screen open do not count. A double tap is sent only to end a burst
 * or when {@link FlightService#getGrant} allows one on the synced state, so a player with no flying Gu sends
 * nothing; the server decides again. Logging out forgets a first tap that was waiting for its second.
 *
 * @author Alex
 * @version 1.0.0
 * @see DoubleTap
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class BurstInput {

    private BurstInput() {}

    private static final int DOUBLE_TAP_TICKS = 7;
    private static final DoubleTap DOUBLE_TAP = new DoubleTap(DOUBLE_TAP_TICKS);
    private static long ticks;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ticks++;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) { DOUBLE_TAP.reset(); }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) onPress(InputConstants.getKey(event.getKey(), event.getScanCode()));
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        if (event.getAction() == GLFW.GLFW_PRESS) onPress(InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
    }

    private static void onPress(InputConstants.Key key) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !minecraft.options.keySprint.isActiveAndMatches(key)) return;

        if (DOUBLE_TAP.press(ticks)
                && (FlightService.isBursting(player) || FlightService.getGrant(player).canBurst())) {
            ModNetwork.sendToServer(ToggleBurstPayload.INSTANCE);
        }
    }
}
