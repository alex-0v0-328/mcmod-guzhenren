package net.alex.guzhenren.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.hud.SonicBoomHud;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.network.ModNetwork;
import net.alex.guzhenren.network.payload.ToggleSecondStagePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The second-stage flight [二段飞行] toggle on the client: double-tapping the sprint key sends
 * {@link ToggleSecondStagePayload}.
 *
 * <p>Presses come from the raw key and mouse events, matched against whatever the sprint key is bound to, because
 * the key mapping itself only tells whether the key is down and, with Sprint set to Toggle, not even that.
 * {@link #DOUBLE_TAP_TICKS} is vanilla's window for its own double taps (forward to sprint, jump to fly), counted in
 * client ticks ({@link #ticks}). Presses with a screen open do not count. A double tap is sent to leave the stage,
 * or to enter it when {@link SecondStageService#canEnter} holds on the client's own state: flying and already
 * sprinting, which the first tap starts while forward is held. The server decides again. A tap sent to enter also
 * arms the first-person sonic boom ({@link SonicBoomHud#noteRequested}). Logging out forgets a first tap that was
 * waiting for its second.
 *
 * @author Alex
 * @version 1.0.0
 * @see DoubleTap
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class SecondStageInput {

    private SecondStageInput() {}

    private static final int DOUBLE_TAP_TICKS = 7;
    private static final DoubleTap DOUBLE_TAP = new DoubleTap(DOUBLE_TAP_TICKS);
    private static long ticks;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ticks++;
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

        if (!DOUBLE_TAP.press(ticks)) return;
        if (SecondStageService.isActive(player)) {
            ModNetwork.sendToServer(ToggleSecondStagePayload.INSTANCE);
        } else if (SecondStageService.canEnter(player)) {
            SonicBoomHud.INSTANCE.noteRequested(player.tickCount);
            ModNetwork.sendToServer(ToggleSecondStagePayload.INSTANCE);
        }
    }
}
