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
 * 开窍[Awakening]仪式事件转发入口：服务端玩家每 tick 末尾推进 {@link AwakeningRitualService#tick}，死亡、切换维度、登出
 * 或重登时调 {@link AwakeningRitualService#finish} 立即结清未完仪式。
 * <ul>
 *   <li>⚠ 死亡事件先于玩家克隆触发：未开启死亡不掉落（{@code keepInventory}）时，仪式中死亡会先补完开窍，再在克隆时按
 *       已开窍玩家的统一规则重置。</li>
 *   <li>⚠ {@link #onDeath} 必须使用 {@link EventPriority#LOWEST} 最后执行，确保被不死图腾或其他模组取消的死亡不会误中
 *       断正在进行的仪式。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class AwakeningRitualEvents {

    private AwakeningRitualEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) AwakeningRitualService.tick(player);
    }

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
