package net.alex.guzhenren.client.input;

import net.alex.guzhenren.client.particle.RingParticle;
import net.alex.guzhenren.network.ClientIntents;
import net.alex.guzhenren.network.payload.DashPayload;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * 横冲系蛊（横冲蛊[Horizontal Crash Gu] / 直撞蛊[Vertical Crash Gu]）冲撞的客户端输入。
 * <ul>
 *   <li>按住 Alt 再按移动键发起，方向键给纵、横各一步；{@link ClientIntents#canDash} 判身上横冲系效果是否允许，
 *       服务端同规则重查；冲撞时 Epic Fight 切入战斗模式，镜头前向偏航按方向转，发送 {@link DashPayload}。</li>
 *   <li>{@link #shouldStartDash}：新按 Alt，或按住 Alt 时新按方向键即冲撞；只认按下沿，否则每 tick 都会再冲；
 *       {@link #reset} 清空记住的按键状态，登出时调用，免得下个世界把按住的键读成按下沿。</li>
 * </ul>
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

    private static final class State {

        boolean previousUp;
        boolean previousDown;
        boolean previousLeft;
        boolean previousRight;
        boolean previousAlt;
    }

    private static final State STATE = new State();

    public static void tick(Minecraft minecraft, LocalPlayer player) {
        boolean up = minecraft.options.keyUp.isDown();
        boolean down = minecraft.options.keyDown.isDown();
        boolean left = minecraft.options.keyLeft.isDown();
        boolean right = minecraft.options.keyRight.isDown();
        boolean pressed = up && !STATE.previousUp || down && !STATE.previousDown
                || left && !STATE.previousLeft || right && !STATE.previousRight;
        boolean alt = Screen.hasAltDown();
        if (minecraft.screen == null && shouldStartDash(alt, STATE.previousAlt, pressed)) {
            int vertical = up == down ? 0 : up ? 1 : -1;
            int horizontal = left == right ? 0 : left ? 1 : -1;
            if (ClientIntents.canDash(vertical, horizontal,
                    player.hasEffect(ModEffects.HORIZONTAL_CRASH_GU),
                    player.hasEffect(ModEffects.VERTICAL_CRASH_GU),
                    player.hasEffect(ModEffects.CHARGING_CRASH_GU))) {
                send(player, vertical, horizontal);
            }
        }
        STATE.previousUp = up;
        STATE.previousDown = down;
        STATE.previousLeft = left;
        STATE.previousRight = right;
        STATE.previousAlt = alt;
    }

    public static void reset() {
        STATE.previousUp = false;
        STATE.previousDown = false;
        STATE.previousLeft = false;
        STATE.previousRight = false;
        STATE.previousAlt = false;
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
