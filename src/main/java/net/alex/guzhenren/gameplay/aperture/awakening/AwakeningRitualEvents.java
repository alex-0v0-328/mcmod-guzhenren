package net.alex.guzhenren.gameplay.aperture.awakening;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 开窍[Awakening]仪式事件入口：服务端玩家 tick 末尾调 {@link AwakeningRitualService#tick}；死亡、换维度、登出、登录调
 * {@link AwakeningRitualService#finish}。
 * <ul>
 *   <li>⚠ 死亡钩子先于克隆：未开 keepInventory 则死亡掉落仪式刚开的空窍[Aperture]，同其他已开窍者。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningRitualService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class AwakeningRitualEvents {

    private AwakeningRitualEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) AwakeningRitualService.tick(player);
    }

    // Last, so a handler that cancels the death (a revive) leaves the ritual running.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AwakeningRitualService.finish(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AwakeningRitualService.finish(player);
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AwakeningRitualService.finish(player);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AwakeningRitualService.finish(player);
    }
}
