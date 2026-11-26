package net.alex.guzhenren.client.input;

import net.alex.guzhenren.client.camera.CombatCameraRig;
import net.alex.guzhenren.client.particle.RingParticle;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.dimension.TreasureYellowHeavenGuard;
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
 * 横冲系蛊（横冲蛊[Horizontal Crash Gu]与直撞蛊[Vertical Crash Gu]）冲撞动作的客户端输入处理器。
 * <ul>
 *   <li>按住 Alt 键的同时按下移动方向键即可发起冲撞，由方向键决定前后或左右各跨出一步；客户端通过
 *       {@link ClientIntents#canDash} 预判身上横冲系效果是否允许冲撞（服务端按同一规则重查），发起冲撞时让 Epic
 *       Fight 切入战斗模式、将镜头前向偏航对齐方向并发送 {@link DashPayload}，同时由 {@link CombatCameraRig#kick} 放宽
 *       视野。</li>
 *   <li>服务端会拒绝的情形（旁观者、身处宝黄天[Treasure Yellow Heaven]、温养中、开窍仪式定身中）客户端同样不发起
 *       （{@link #canStart}），免得先放宽视野、藏起冲刺环再被服务端静默拒绝。</li>
 *   <li>{@link #shouldStartDash} 仅在刚按下 Alt 或按住 Alt 期间刚按下方向键的按下沿触发，避免长按每 tick 重复冲撞；
 *       登出时调用 {@link #reset} 清空已记录的按键状态，防止进入下个世界时把仍按住的按键误判为新的按下沿。</li>
 * </ul>
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
        if (minecraft.screen == null && shouldStartDash(alt, STATE.previousAlt, pressed) && canStart(player)) {
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

    private static boolean canStart(LocalPlayer player) {
        return !player.isSpectator() && !TreasureYellowHeavenGuard.isInside(player)
                && !ApertureNourishService.isCultivating(player) && !AwakeningRitualService.holdsStill(player);
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
                    CombatCameraRig.kick();
                });
    }
}
