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
 * 空中坠击动作的客户端输入处理器：在空中同时按下潜行键与 Alt 键发送 {@link PlungePayload}。
 * <ul>
 *   <li>组合键不限按下顺序（{@link #isChord}）：既支持按住潜行时按下 Alt，也支持按住 Alt 时按下潜行；仅在按下瞬间触发
 *       一次而不随长按每 tick 重复，界面打开时忽略；Alt 键固定支持左右两侧（与 {@link DashInput} 一致），潜行键读取当前
 *       按键绑定与原始键鼠事件（切换潜行模式下开启的状态视为持续按住）；客户端先通过 {@link PlungeService#canStart} 预
 *       检，服务端再做权威判定。</li>
 *   <li>该双键组合在空中不与其余动作冲突：整合包内 Epic Fight 闪避虽绑定在左 Alt 但在空中无效，而坠击在地面无效。</li>
 *   <li>{@link #onMovementInput} 在坠击期间抢在原版之前丢弃跳跃输入，否则新起跳会意外开启鞘翅，或双击跳跃会在
 *       {@link PlungeService#tickLocal} 关闭前重新开启飞行一 tick。</li>
 * </ul>
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
