package net.alex.guzhenren.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.hud.SonicBoomHud;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.alex.guzhenren.network.payload.ToggleSecondStagePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 二段飞行[Second-Stage Flight]开关的客户端输入：双击疾跑键发送 {@link ToggleSecondStagePayload}。
 * <ul>
 *   <li>按键取原始键鼠事件，与疾跑键当前绑定比对：按键映射只知是否按下，疾跑设为切换模式时连这也不行；界面打开时不算；
 *       {@link #DOUBLE_TAP_TICKS} 即原版自身双击（前进疾跑、跳跃飞行）的窗口，按客户端 tick（{@link #ticks}）计。</li>
 *   <li>二段中双击即发退出；否则客户端自身满足 {@link SecondStageService#canEnter}（飞行中且已在疾跑，
 *       按住前进时第一下即开始疾跑）才发进入，服务端再判；进入请求同时备好第一人称音爆环
 *       （{@link SonicBoomHud#noteRequested}）。</li>
 *   <li>唯一可变静态字段 {@code ticks} 为本客户端双击用 tick 时钟，只增不减；登出时忘掉待第二下的第一下。</li>
 * </ul>
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
            PacketDistributor.sendToServer(ToggleSecondStagePayload.INSTANCE);
        } else if (SecondStageService.canEnter(player)) {
            SonicBoomHud.INSTANCE.noteRequested(player.tickCount);
            PacketDistributor.sendToServer(ToggleSecondStagePayload.INSTANCE);
        }
    }
}
