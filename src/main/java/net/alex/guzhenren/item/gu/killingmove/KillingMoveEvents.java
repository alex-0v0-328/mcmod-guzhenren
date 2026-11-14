package net.alex.guzhenren.item.gu.killingmove;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 杀招[Killing Move]存储辅蛊的刷新入口。每 {@link #REFRESH_TICKS} tick 及任一菜单关闭（存储菜单是玩家改存储的途径）时，
 * 服务端重算已学杀招在空窍[Aperture]存储有无辅蛊（{@link KillingMoveService#refreshStoreBacked}），不变不重发同步。
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMoveService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class KillingMoveEvents {

    public static final int REFRESH_TICKS = 10;

    private KillingMoveEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % REFRESH_TICKS == 0) {
            KillingMoveService.refreshStoreBacked(player);
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) KillingMoveService.refreshStoreBacked(player);
    }
}
