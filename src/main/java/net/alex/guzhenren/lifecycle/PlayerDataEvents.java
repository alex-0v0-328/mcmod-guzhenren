package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.gu.tending.VitalGuLostEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

/**
 * 玩家生命周期事件（登录、登出、克隆、重生、睡眠唤醒、本命蛊损毁与回血养本命蛊）的统一转发入口，除门控外全部业务交由
 * {@link PlayerDataService}、{@link VitalLossService} 与 {@link VitalMendService} 处理。
 * <ul>
 *   <li>死亡克隆处理（{@link #onClone}）以 {@code EventPriority.LOW} 在 NeoForge 复制完继承附件后执行，确保清空型重置不
 *       被覆盖；是否保留物品栏取自 {@link MinecraftServer} 游戏规则而非维度实例。</li>
 *   <li>⚠ {@link #onRespawn} 必须跳过末地返回传送门触发的重生事件（{@code isEndConquered}），防止跨维返回错误触发死亡
 *       恢复逻辑。</li>
 *   <li>⚠ {@link #onWakeUp} 必须同时校验 {@code wakeImmediately}、{@code updateLevel} 与
 *       {@code isSleepingLongEnough}，确保仅在完整睡醒那一刻触发一次回复。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class PlayerDataEvents {

    private PlayerDataEvents() {}

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataService.onJoin(player);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerDataService.onLogout(player);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onClone(PlayerEvent.Clone event) {
        MinecraftServer server = event.getOriginal().getServer();
        boolean keepInventory = server != null
                && server.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        PlayerDataService.onClone(event.getOriginal(), event.getEntity(), event.isWasDeath(), keepInventory);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataService.onRespawn(player);
        }
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.wakeImmediately() || event.updateLevel()) return;
        if (!player.isSleepingLongEnough()) return;

        PlayerDataService.onSleepComplete(player);
    }

    @SubscribeEvent
    public static void onVitalGuLost(VitalGuLostEvent event) { VitalLossService.onVitalGuLost(event); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHealed(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) VitalMendService.onHealed(player, event.getAmount());
    }
}
