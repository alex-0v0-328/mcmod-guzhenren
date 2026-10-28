package net.alex.guzhenren.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.plunge.PlungeService;
import net.alex.guzhenren.network.payload.PlungePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The plunge [坠击] on the client: sneak plus Alt in the air sends {@link PlungePayload}.
 *
 * <p>The chord counts in either order ({@link #isChord}): an Alt press while sneak is held, or a sneak press while
 * Alt is held -- the press, never the held state, so holding both starts one plunge, not one a tick. Alt is fixed,
 * either side, as for the dash ({@link DashInput}); sneak is whatever the sneak key is bound to, read from the raw key
 * and mouse events, and with Sneak set to Toggle a toggled-on sneak counts as held. Presses with a screen open do not
 * count. The chord is sent only when {@link PlungeService#canStart} holds on the client's own state; the server
 * decides again.
 *
 * <p>The two keys never meet another action in the air: Epic Fight's dodge, on Left Alt in the pack, refuses in the
 * air, and the plunge refuses on the ground.
 *
 * <p>{@link #onMovementInput} drops the jump input while the plunge runs, before vanilla reads it: a fresh jump
 * press would otherwise open the elytra or, double-tapped, turn flight back on for a tick before
 * {@link PlungeService#tickLocal} shuts them again.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlungeService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class PlungeInput {

    private PlungeInput() {}

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        boolean alt = event.getKey() == GLFW.GLFW_KEY_LEFT_ALT || event.getKey() == GLFW.GLFW_KEY_RIGHT_ALT;
        onPress(alt, InputConstants.getKey(event.getKey(), event.getScanCode()));
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            onPress(false, InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (PlungeService.isActive(event.getEntity())) event.getInput().jumping = false;
    }

    public static boolean isChord(boolean altPressed, boolean sneakPressed, boolean altDown, boolean sneakDown) {
        return altPressed && sneakDown || sneakPressed && altDown;
    }

    private static void onPress(boolean altPressed, InputConstants.Key key) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) return;

        boolean sneakPressed = minecraft.options.keyShift.isActiveAndMatches(key);
        if (isChord(altPressed, sneakPressed, Screen.hasAltDown(), minecraft.options.keyShift.isDown())
                && PlungeService.canStart(player)) {
            PacketDistributor.sendToServer(PlungePayload.INSTANCE);
        }
    }
}
