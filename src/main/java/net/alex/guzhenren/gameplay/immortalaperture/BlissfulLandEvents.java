package net.alex.guzhenren.gameplay.immortalaperture;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureWork;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.LandMaps;
import net.alex.guzhenren.gameplay.immortalaperture.state.EarthSpiritUseEvent;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandCensus;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandNoticeEvent;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandOwnerChangeEvent;
import net.alex.guzhenren.gameplay.immortalaperture.territory.TerritoryWork;
import net.alex.guzhenren.gameplay.secretrealm.SoulsunderShakeEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 福地[Blissful Land]的事件入口，只做转交：服务端刻、玩家刻、玩家死亡、区块加载、荡魂山震荡、地灵右键、易主、福地提示、关面板、
 * 牧场栏内的自然刷新、田区工单登记、开服核对边界、停服清理。
 * <ul>
 *   <li>⚠ 自然刷新在 {@code MobSpawnEvent.PositionCheck} 判失败（{@link PastureWork#blocksSpawn}）：此时生物还没生成、没做
 *       最终化，原版只当这个落点不合格。</li>
 *   <li>⚠ 死亡监听取最低优先级且不收已取消事件：别的模组取消死亡、不死图腾救下都不算死，不失主；开服核对边界也取最低
 *       优先级，因 Gu World 在同一开服事件里才施加福地维度的边界。</li>
 *   <li>玩家每秒的认主判定不走这里，挂在 {@code Heartbeat}。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BlissfulLandEvents {

    private BlissfulLandEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        BlissfulLands.tickServer(event.getServer());
        PastureWork.tickExpulsions(event.getServer());
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (PastureWork.blocksSpawn(event.getLevel(), event.getSpawnType(), event.getX(), event.getZ())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) BlissfulLands.tickPlayerFast(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) BlissfulLands.onDeath(player);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            BlissfulLands.onChunkLoad(level, chunk);
        }
    }

    @SubscribeEvent
    public static void onSpiritUse(EarthSpiritUseEvent event) {
        EarthSpiritPanel.open(event.getPlayer(), event.getSpirit());
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getContainer() instanceof EarthSpiritMenu) LandMaps.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onNotice(LandNoticeEvent event) { EarthSpiritPanel.onNotice(event); }

    @SubscribeEvent
    public static void onOwnerChanged(LandOwnerChangeEvent event) { BlissfulLands.onOwnerChanged(event); }

    @SubscribeEvent
    public static void onShake(SoulsunderShakeEvent event) {
        if (BlissfulLands.isExempt(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRegisterTickets(RegisterTicketControllersEvent event) {
        event.register(FarmWork.TICKETS);
        event.register(PastureWork.TICKETS);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerStarted(ServerStartedEvent event) { BlissfulLands.checkBorders(event.getServer()); }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TerritoryWork.clear();
        PastureWork.clearExpulsions();
        LandCensus.clear();
        EarthSpiritPanel.clear();
    }
}
