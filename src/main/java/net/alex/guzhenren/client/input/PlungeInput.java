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
 * 坠击的客户端输入：空中按潜行加 Alt 发送 {@link PlungePayload}。
 * <ul>
 *   <li>组合键不分先后（{@link #isChord}）：按住潜行时按 Alt，或按住 Alt 时按潜行；只认按下，同时按住只起一次，不会每
 *       tick 一次；界面打开时不算；Alt 固定、左右均可，同冲撞（{@link DashInput}）；潜行取潜行键当前绑定，
 *       读原始键鼠事件，切换模式下开启的潜行算按住；客户端自身满足 {@link PlungeService#canStart} 才发，
 *       服务端再判。</li>
 *   <li>两键空中不撞其他动作：整合包里 Epic Fight 闪避在左 Alt，空中拒绝；坠击在地面拒绝。</li>
 *   <li>{@link #onMovementInput} 坠击中先于原版丢弃跳跃输入：否则新跳跃会开鞘翅，或双击在
 *       {@link PlungeService#tickLocal} 关掉前重开飞行一 tick。</li>
 * </ul>
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
