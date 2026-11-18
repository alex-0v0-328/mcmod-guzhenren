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
 * 二段飞行开关的客户端输入处理器：双击疾跑键发送 {@link ToggleSecondStagePayload}。
 * <ul>
 *   <li>按键监听直接取原始键鼠事件并与疾跑键当前绑定比对（原版按键映射仅反映是否按住，切换疾跑模式下甚至连按下状态都无
 *       法区分），打开界面时忽略输入；{@link #DOUBLE_TAP_TICKS} 与原版双击（前进疾跑、跳跃飞行）窗口一致，按本类客户端
 *       tick 时钟（{@link #ticks}）计。</li>
 *   <li>处于二段飞行时双击直接发送退出请求；否则先由客户端校验 {@link SecondStageService#canEnter}（飞行中且已在疾
 *       跑，按住前进时第一下即起跑）才发送进入请求并交服务端再判，同时通过 {@link SonicBoomHud#noteRequested} 登记待
 *       播放的第一人称音爆环。</li>
 *   <li>唯一可变静态字段 {@code ticks} 是本客户端用于双击计时的单调时钟，登出时清除待配对的首次按下记录。</li>
 * </ul>
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
