package net.alex.guzhenren.gameplay.immortalaperture;

import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.dimension.DimensionTravelService;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmState;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmWork;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldPlans;
import net.alex.guzhenren.gameplay.immortalaperture.gu.GuState;
import net.alex.guzhenren.gameplay.immortalaperture.gu.GuWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.GatherWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdState;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdWork;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureSettings;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureWork;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.MapPart;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.LandMaps;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapMarkSource;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapView;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapViewerGate;
import net.alex.guzhenren.gameplay.immortalaperture.spring.SpringWork;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandLookup;
import net.alex.guzhenren.gameplay.immortalaperture.state.EarthSpiritEntity;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandCensus;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandHostingJob;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandMembership;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandOwnerChangeEvent;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandSite;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.gameplay.immortalaperture.territory.TerritoryWork;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealms;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地[Blissful Land]的调度入口：认主与失主时机、地灵补生成、托管轮、割弃区块清空、虚空拉回、免震荡判定。
 * <ul>
 *   <li>⚠ 福地维度不存在（没装 Gu World）时 {@link BlissfulLandLookup#level} 为空，以下全部不做；秘境未解出时
 *       {@link BlissfulLandLookup#siteOf} 为空，认主与地灵补生都不做。{@link #checkBorders} 在开服时核对每块福地维度的
 *       世界边界是否正好 {@code half × 32} 格（两仓各写一份边长），不一致只记错误日志。</li>
 *   <li>认主：{@link #tickPlayer} 挂在 {@code Heartbeat}（每秒），玩家在福地维度、福地无主且秘境山心已解出时判定。</li>
 *   <li>失主：{@link #onDeath} 在任何维度死亡时（「死亡不掉落」开着就不算），{@link #onReset} 在角色重置时（非保留死亡
 *       的克隆也会走重置，重复失主无副作用）。易主后 {@link #onOwnerChanged} 替各功能收尾：认主重挂田与牧场的强加载，
 *       失主关三种托管、撤强加载。</li>
 *   <li>{@link #tickServer} 每刻：清 1 个割弃区块；每 {@value #SPIRIT_CHECK_TICKS} 刻：行宫区块与其实体都已加载时，地灵
 *       不在就先认领附近现有的、没有才补生成（多余的移除）；每 {@value LandHostingJob#INTERVAL_TICKS} 刻：有主时按
 *       {@link #HOSTING_JOBS} 的顺序（种植、自动收拢、养殖、蛊虫）跑各功能的托管任务，各用各的预算；每秒推进一步标准田规划队列
 *       （{@link FieldPlans}），并给开着面板的主人刷新快照（只算他正在看的页）；每刻最后驱动地图管线（{@link LandMaps#tick}，
 *       订阅门与标记来源由本类传入，地图视口载荷也经 {@link #mapView} 转进去）。</li>
 *   <li>元泉收集（{@link SpringWork}）也是托管任务，排在蛊虫之后，没有开关，有主就跑；区块加载时（{@link #onChunkLoad}）顺带登记
 *       区块里的元泉泉位。</li>
 *   <li>{@link #storageHere}：人在自己当主人的福地里时给出那块福地的库藏，蛊粮直供（{@code GuHunger}）用它；别处、
 *       非主人都是空。</li>
 *   <li>{@link #tickPlayerFast} 每刻：福地里跌出最低建造高度就拉回入口（割弃侧没有边界墙）。</li>
 * </ul>
 */

public final class BlissfulLands {

    private BlissfulLands() {}

    public static final int SPIRIT_CHECK_TICKS = 10 * Ticks.SECOND;
    public static final int SPIRIT_SEARCH = 16;
    public static final List<LandHostingJob> HOSTING_JOBS = List.of(FarmWork::host, GatherWork::host, HerdWork::host,
            GuWork::host, SpringWork::host);
    public static final MapViewerGate MAP_VIEWER_GATE = EarthSpiritPanel::mapViewerLand;
    public static final List<MapMarkSource> MAP_MARK_SOURCES = List.of(HerdWork::mapMarks, GuWork::mapMarks);

    //region Ownership [认主与失主]
    public static void tickPlayer(@NotNull ServerPlayer player) {
        BlissfulLand land = BlissfulLandLookup.landOf(player.level());
        if (land == null) return;

        BlissfulLandData data = BlissfulLandData.get(player.server, land);
        RealmSite realm = SecretRealms.siteOf(player.server, land.realm());
        if (realm != null) {
            LandMembership.tryClaim(player.serverLevel(), data, realm, BlissfulLandLookup.siteOf(realm, land).summitY(),
                    player);
        }
    }

    public static void onDeath(@NotNull ServerPlayer player) {
        if (player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        loseAll(player);
    }

    public static void onReset(@NotNull ServerPlayer player) { loseAll(player); }

    public static void onOwnerChanged(@NotNull LandOwnerChangeEvent event) {
        ServerLevel level = event.getLevel();
        BlissfulLandData data = event.getData();
        if (event.isClaimed()) {
            if (level == null) return;
            FarmWork.retain(level, data);
            PastureWork.retain(level, data);
            return;
        }
        HerdState.of(data).clearHosting();
        PastureSettings.of(data).clearAutoGather();
        GuState.of(data).clearHosting();
        FarmState.of(data).setHosting(false);
        if (level == null) return;
        FarmWork.release(level, data);
        PastureWork.release(level, data);
    }

    private static void loseAll(ServerPlayer player) {
        for (BlissfulLand land : BlissfulLand.ALL) {
            BlissfulLandData data = BlissfulLandData.get(player.server, land);
            if (!data.isOwner(player.getUUID())) continue;
            LandMembership.lose(player.server, BlissfulLandLookup.level(player.server, land), data);
        }
    }

    public static @Nullable BlissfulStorage storageHere(@NotNull ServerPlayer player) {
        BlissfulLand land = BlissfulLandLookup.landOf(player.level());
        if (land == null) return null;

        BlissfulLandData data = BlissfulLandData.get(player.server, land);
        return data.isOwner(player.getUUID()) ? BlissfulStorage.of(data) : null;
    }

    public static boolean isExempt(@NotNull ServerPlayer player) {
        BlissfulLand land = BlissfulLandLookup.landOf(player.level());
        return land != null && LandMembership.isExempt(BlissfulLandData.get(player.server, land), player);
    }
    //endregion

    //region Ticks [调度]
    public static void tickServer(@NotNull MinecraftServer server) {
        int tick = server.getTickCount();
        for (BlissfulLand land : BlissfulLand.ALL) {
            ServerLevel level = BlissfulLandLookup.level(server, land);
            if (level == null) continue;

            TerritoryWork.tick(level);
            BlissfulLandData data = BlissfulLandData.get(server, land);
            if (tick % SPIRIT_CHECK_TICKS == 0) keepSpirit(level, land, data);
            if (tick % LandHostingJob.INTERVAL_TICKS == 0 && data.hasOwner()) host(server, level, data);
            if (tick % Ticks.SECOND == 0) FieldPlans.tick(level, data, () -> MapPart.plot(level, data));
        }
        if (tick % Ticks.SECOND == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.containerMenu instanceof EarthSpiritMenu)) continue;
                EarthSpiritPanel.refresh(player, Component.empty());
            }
        }
        LandMaps.tick(server, MAP_VIEWER_GATE, MAP_MARK_SOURCES);
    }

    public static void checkBorders(@NotNull MinecraftServer server) {
        for (BlissfulLand land : BlissfulLand.ALL) {
            ServerLevel level = BlissfulLandLookup.level(server, land);
            if (level == null) continue;
            double expected = land.half() * 2.0D * 16.0D;
            double size = level.getWorldBorder().getSize();
            if (Math.abs(size - expected) < 0.5D) continue;
            Guzhenren.LOGGER.error("Blissful land {} has a {}-block world border but this mod expects {} blocks"
                    + " ({} chunks a side); the land size in Gu World and in BlissfulLand disagree", land.id(), size,
                    expected, land.half() * 2);
        }
    }

    public static void mapView(@NotNull ServerPlayer player, @NotNull MapView view) {
        LandMaps.view(player, view, MAP_VIEWER_GATE);
    }

    public static int host(@NotNull MinecraftServer server, @NotNull ServerLevel level,
                           @NotNull BlissfulLandData data) {
        UUID owner = data.owner();
        ServerPlayer online = owner == null ? null : server.getPlayerList().getPlayer(owner);
        int handled = 0;
        for (LandHostingJob job : HOSTING_JOBS) handled += job.host(level, data, online);
        LandCensus.invalidate(level);
        return handled;
    }

    public static void keepSpirit(@NotNull ServerLevel level, @NotNull BlissfulLand land,
                                  @NotNull BlissfulLandData data) {
        LandSite site = BlissfulLandLookup.siteOf(level.getServer(), land);
        if (site == null) return;
        BlockPos palace = site.palace();
        if (!level.isLoaded(palace) || !level.areEntitiesLoaded(ChunkPos.asLong(palace))) return;
        UUID known = data.spirit();
        if (known != null && level.getEntity(known) instanceof EarthSpiritEntity spirit && spirit.isAlive()) return;

        List<EarthSpiritEntity> found = level.getEntitiesOfClass(EarthSpiritEntity.class,
                new AABB(palace).inflate(SPIRIT_SEARCH), EarthSpiritEntity::isAlive);
        if (!found.isEmpty()) {
            data.setSpirit(found.getFirst().getUUID());
            for (int i = 1; i < found.size(); i++) found.get(i).discard();
            return;
        }
        EarthSpiritEntity spirit = land.spirit().get().create(level);
        if (spirit == null) return;
        spirit.moveTo(palace.getX() + 0.5D, palace.getY(), palace.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(spirit);
        data.setSpirit(spirit.getUUID());
    }

    public static void onChunkLoad(@NotNull ServerLevel level, @NotNull LevelChunk chunk) {
        BlissfulLand land = BlissfulLandLookup.landOf(level);
        if (land == null) return;
        BlissfulLandData data = BlissfulLandData.get(level.getServer(), land);
        TerritoryWork.onChunkLoad(level, data.territory(), chunk);
        SpringWork.onChunkLoad(data, chunk);
    }

    public static void tickPlayerFast(@NotNull ServerPlayer player) {
        if (BlissfulLandLookup.landOf(player.level()) == null
                || player.getY() >= player.level().getMinBuildHeight()) {
            return;
        }

        ModDimensions.AnchoredDimension anchor = ModDimensions.ANCHORED_DIMENSIONS.get(player.level().dimension());
        if (anchor == null) return;
        DimensionTravelService.rescueIfBelowVoid(player,
                DimensionTravelService.getArrival(player.serverLevel(), anchor));
    }
    //endregion
}
