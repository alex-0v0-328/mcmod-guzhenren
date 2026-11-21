package net.alex.guzhenren.gameplay.secretrealm;

import net.alex.guzhenren.Guzhenren;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 天地秘境[Secret Realm]的世界级事件入口：服务端 tick 末尾交给 {@link SecretRealms#tickAreas}（自行按秒门控），生物死亡
 * 交给 {@link SecretRealms#onDeath}。
 * <ul>
 *   <li>⚠ 死亡监听取最低优先级且不收已取消事件：别的模组取消死亡时不长胆石；不死图腾在事件前结算，救下的不算。</li>
 *   <li>停服时清空 {@link SecretRealms} 的范围缓存，不让旧存档的生成状态留在内存里。</li>
 *   <li>玩家的每秒结算不走这里，挂在 {@code Heartbeat}，与魂魄致死检查同拍。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class SecretRealmEvents {

    private SecretRealmEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) { SecretRealms.tickAreas(event.getServer()); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) { SecretRealms.onDeath(event.getEntity()); }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) { SecretRealms.clear(); }
}
