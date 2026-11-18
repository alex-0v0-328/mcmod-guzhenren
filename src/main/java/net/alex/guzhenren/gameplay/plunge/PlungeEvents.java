package net.alex.guzhenren.gameplay.plunge;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 坠击机制的事件订阅入口（核心规则位于 {@link PlungeService}）。
 * <ul>
 *   <li>坠击激活期间取消原版 {@link LivingFallEvent} 摔落结算（改由坠击落地时统一结算 3 倍摔伤），并在跨维度或触发
 *       {@link EntityTeleportEvent} 时提前终止坠击。</li>
 *   <li>⚠ 其他模组若直接调用底层 {@code teleportTo} 而不发布 {@link EntityTeleportEvent}，坠击不会中断且仍沿用传送前的
 *       起跳顶点计算落差（已知限制；原版指令、末影珍珠、紫颂果与末影人传送均会正常触发事件并终止坠击）。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class PlungeEvents {

    private PlungeEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            PlungeService.tick(serverPlayer);
        } else if (player.isLocalPlayer()) {
            PlungeService.tickLocal(player);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && PlungeService.isActive(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlungeService.stop(player);
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlungeService.stop(player);
    }
}
