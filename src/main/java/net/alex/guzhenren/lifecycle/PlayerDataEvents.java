package net.alex.guzhenren.lifecycle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.gu.tending.VitalGuLostEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

/**
 * 玩家生命周期（登录、登出、死亡、克隆、重生、睡眠）事件的转发入口。
 * <ul>
 *   <li>各时刻转 {@link PlayerDataService}，本命蛊[Vital Gu]之死（{@code VitalGuLostEvent}）转
 *       {@link VitalLossService}；本类不做决定，免得处理器对重生保留什么起分歧。</li>
 *   <li>{@code keepInventory} 取自 {@link MinecraftServer} 游戏规则而非 {@code level()}，传给唯一结算死亡重置的
 *       {@code onClone}；它用 {@code LOW} 优先级，在 NeoForge 复制继承附件后跑，重置不被覆盖。</li>
 *   <li>⚠ {@code onRespawn} 跳过末地返回（{@code isEndConquered}）：原版对它也发重生事件，换维度从不重置身体。</li>
 *   <li>⚠ {@code onWakeUp} 以 {@code wakeImmediately} / {@code updateLevel} / {@code isSleepingLongEnough} 把关：醒来是
 *       跳变非持续状态，盯 tick 会重复触发。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerDataService
 * @since 1.0.0
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
}
