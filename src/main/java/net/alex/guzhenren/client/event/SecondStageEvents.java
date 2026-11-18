package net.alex.guzhenren.client.event;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.hud.SonicBoomHud;
import net.alex.guzhenren.client.sound.SecondStageWindSound;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.flight.SecondStageCruise;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 本地玩家端的二段飞行客户端事件处理器：负责驱动本地油门步进以及基于油门的镜头与视听反馈。
 * <ul>
 *   <li>实际位移逻辑在 {@code net.alex.guzhenren.mixin.PlayerMixin}，镜头侧倾与抖动由 {@link SecondStageCamera} 负
 *       责。</li>
 *   <li>{@link #onClientTick} 在玩家 tick 前且本 tick 数据包处理后执行；游戏暂停时玩家不执行 tick，本逻辑一并跳
 *       过。</li>
 *   <li>同步标记刚开启或出现在新本地玩家实体上（例如换维度会重建玩家实体并重新同步标记）时：将油门初始化为
 *       {@link SecondStageCruise#ENTRY_THROTTLE}（与服务端一致），启动风声（{@link SecondStageWindSound}），并播放第
 *       一人称音爆环（{@link SonicBoomHud#onSecondStageStarted}，仅对本客户端主动请求的进入生效）。</li>
 *   <li>携带飞行标记期间每 tick 根据本地疾跑状态步进油门（{@link SecondStageService#stepThrottle}），使巡航响应无需等待
 *       网络往返；失去标记后立即清零油门。</li>
 *   <li>{@link #onComputeFovModifier} 在二段飞行期间随油门放宽视野：将已包含飞行与疾跑加成的原版修正值再乘以
 *       {@link #getFovFactor}，满油门时至多额外放宽 {@link #FOV_WIDENING}。</li>
 *   <li>视野放宽按原版「视场角效果」滑条向 1.0 缓和（与 NeoForge 缓和原版修正值一致），关闭该选项时看不到额外放宽；原版
 *       仍会用数 tick 平滑过渡并受总放宽上限约束（疾跑与飞行同样计入上限）。</li>
 *   <li>{@link #onLoggingOut} 清空 {@code STATE.wasInSecondStage}、记录的玩家实体与音爆状态，避免重进世界时残留上一会话
 *       的延续状态。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class SecondStageEvents {

    private SecondStageEvents() {}

    private static final double FOV_WIDENING = 0.15D;

    private static final class State {

        boolean wasInSecondStage;
        @Nullable LocalPlayer lastPlayer;
    }

    private static final State STATE = new State();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) return;
        LocalPlayer player = minecraft.player;
        boolean active = player != null && SecondStageService.isActive(player);
        if (active && (!STATE.wasInSecondStage || player != STATE.lastPlayer)) {
            PlayerData.get(player).setSecondStageThrottle(SecondStageCruise.ENTRY_THROTTLE);
            minecraft.getSoundManager().play(new SecondStageWindSound(player));
            SonicBoomHud.INSTANCE.onSecondStageStarted(player.tickCount);
        } else if (active) {
            SecondStageService.stepThrottle(player);
        } else if (player != null) {
            PlayerData data = PlayerData.get(player);
            if (data.getSecondStageThrottle() != 0.0D) data.setSecondStageThrottle(0.0D);
        }
        STATE.wasInSecondStage = active;
        STATE.lastPlayer = player;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STATE.wasInSecondStage = false;
        STATE.lastPlayer = null;
        SonicBoomHud.INSTANCE.reset();
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = event.getPlayer();
        if (player == minecraft.player && player.getAbilities().flying && SecondStageService.isActive(player)) {
            event.setNewFovModifier(event.getNewFovModifier() * getFovFactor(
                    minecraft.options.fovEffectScale().get(), PlayerData.get(player).getSecondStageThrottle()));
        }
    }

    public static float getFovFactor(double effectScale, double throttle) {
        return (float) (1.0D + FOV_WIDENING * throttle * effectScale);
    }
}
