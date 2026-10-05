package net.alex.guzhenren.client.input;

import net.alex.guzhenren.client.particle.RingParticle;
import net.alex.guzhenren.network.payload.DashPayload;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * The Crash Gu dash [横冲 / 直撞] on the client: holding Alt and pressing a movement key starts one. The
 * direction keys give a vertical and a horizontal step; {@link DashPayload#canDash} decides whether the worn
 * Crash Gu effects allow it, the same rule the server checks again. A dash switches Epic Fight into its own
 * mode, turns the camera's forward yaw by the direction, and sends {@link DashPayload}.
 *
 * <p>{@link #shouldStartDash}: a fresh Alt press starts a dash, and so does a fresh direction press while
 * Alt is still held -- the edges, never the held state, or every tick would dash again.
 *
 * @author Alex
 * @version 1.0.0
 * @see DashPayload
 * @since 1.0.0
 */

public final class DashInput {

    private DashInput() {}

    private static final float DASH_YAW_CROSS = 90.0F;
    private static final float DASH_YAW_DIAGONAL = 45.0F;
    private static boolean previousUp;
    private static boolean previousDown;
    private static boolean previousLeft;
    private static boolean previousRight;
    private static boolean previousAlt;

    public static void tick(Minecraft minecraft, LocalPlayer player, ItemStack mainHand, boolean canDash) {
        boolean up = minecraft.options.keyUp.isDown();
        boolean down = minecraft.options.keyDown.isDown();
        boolean left = minecraft.options.keyLeft.isDown();
        boolean right = minecraft.options.keyRight.isDown();
        boolean pressed = up && !previousUp || down && !previousDown
                || left && !previousLeft || right && !previousRight;
        boolean alt = Screen.hasAltDown();
        if (canDash && minecraft.screen == null && shouldStartDash(alt, previousAlt, pressed)) {
            int vertical = up == down ? 0 : up ? 1 : -1;
            int horizontal = left == right ? 0 : left ? 1 : -1;
            if (DashPayload.canDash(mainHand, vertical, horizontal,
                    player.hasEffect(ModEffects.HORIZONTAL_CRASH_GU),
                    player.hasEffect(ModEffects.VERTICAL_CRASH_GU),
                    player.hasEffect(ModEffects.CHARGING_CRASH_GU))) {
                send(player, vertical, horizontal);
            }
        }
        previousUp = up;
        previousDown = down;
        previousLeft = left;
        previousRight = right;
        previousAlt = alt;
    }

    public static boolean shouldStartDash(boolean alt, boolean previousAlt, boolean directionPressed) {
        return alt && (!previousAlt || directionPressed);
    }

    private static void send(LocalPlayer player, int vertical, int horizontal) {
        EpicFightCapabilities.getLocalPlayerPatchAsOptional(player)
                .ifPresent(patch -> {
                    patch.toEpicFightMode(true);
                    float cameraYRot = EpicFightCameraAPI.getInstance().getForwardYRot();
                    float yRot = Mth.wrapDegrees(cameraYRot
                            - (DASH_YAW_CROSS * horizontal * (1 - Math.abs(vertical))
                            + DASH_YAW_DIAGONAL * vertical * horizontal));
                    PacketDistributor.sendToServer(new DashPayload(vertical, horizontal, yRot));
                    RingParticle.noteLocalDash(player.tickCount);
                });
    }
}
