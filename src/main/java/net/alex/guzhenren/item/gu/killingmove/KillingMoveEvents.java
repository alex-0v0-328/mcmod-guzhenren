package net.alex.guzhenren.item.gu.killingmove;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 杀招[Killing Move]空窍存储辅蛊可用性的服务端刷新入口：每 {@link #REFRESH_TICKS} tick 以及任意容器菜单关闭时（空窍存储
 * 界面是玩家调整存储内蛊虫的途径），调用 {@link KillingMoveService#refreshStoreBacked} 重算已学杀招在空窍存储中是否有可
 * 用辅蛊，结果未变时不重复发送同步包。
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
