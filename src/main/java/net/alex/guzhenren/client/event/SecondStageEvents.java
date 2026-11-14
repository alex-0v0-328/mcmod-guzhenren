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
 * 本地玩家端的二段飞行[Second-Stage Flight]：驱动它的油门，及读油门的反馈。
 *
 * <ul>
 *   <li>{@code net.alex.guzhenren.mixin.PlayerMixin} 移动玩家；{@link SecondStageCamera} 管镜头侧倾与抖动。</li>
 *   <li>{@link #onClientTick} 在玩家 tick 前、本 tick 数据包后运行；暂停时玩家不 tick，一并跳过。</li>
 *   <li>同步标记刚开启或出现在新本地玩家上（换维度会重建玩家并重新同步标记）时：油门设为
 *       {@link SecondStageCruise#ENTRY_THROTTLE}（同服务端），启动风声（{@link SecondStageWindSound}），播第一人称音爆
 *       （{@link SonicBoomHud#onSecondStageStarted}，只对本客户端请求的进入保留）。</li>
 *   <li>带标记每 tick 按本地疾跑推油门（{@link SecondStageService#stepThrottle}），巡航响应不等往返；无标记清零。</li>
 *   <li>{@link #onComputeFovModifier}：飞二段时随油门放宽视野，把原版修正值（已含飞行与疾跑的放宽）乘
 *       {@link #getFovFactor}；满油门最多再放宽 {@link #FOV_WIDENING}。</li>
 *   <li>按原版“视场角效果”滑条向零缓和（同 NeoForge 缓和原版修正值），关掉该项的玩家看不到放宽；原版用几 tick 把视野缓
 *       向新修正值，Minecraft 对总放宽设上限，疾跑与飞行也计入。</li>
 *   <li>{@link #onLoggingOut} 清 {@code STATE.wasInSecondStage}、所记玩家与音爆，新世界首进才不像延续。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SecondStageService
 * @since 1.0.0
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
