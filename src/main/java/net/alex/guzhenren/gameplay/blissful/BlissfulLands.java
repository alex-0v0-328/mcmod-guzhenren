package net.alex.guzhenren.gameplay.blissful;

import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.blissful.member.MemberService;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.blissful.territory.TerritoryWork;
import net.alex.guzhenren.gameplay.dimension.DimensionTravelService;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.gameplay.secretrealm.SecretRealms;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地[Blissful Land]的调度入口：认主与失主时机、地灵补生成、托管轮、割弃区块清空、虚空拉回、免震荡判定。
 * <ul>
 *   <li>⚠ 福地维度不存在（没装 Gu World）时 {@link #level} 为空，以下全部不做；存档照存，装回后恢复。</li>
 *   <li>认主：{@link #tickPlayer} 挂在 {@code Heartbeat}（每秒），玩家在福地维度、福地无主且秘境山心已解出时判定。</li>
 *   <li>失主：{@link #onDeath} 在任何维度死亡时（「死亡不掉落」开着就不算），{@link #onReset} 在角色重置时（非保留死亡
 *       的克隆也会走重置，重复失主无副作用）。</li>
 *   <li>{@link #tickServer} 每刻：清 1 个割弃区块；每 {@value #SPIRIT_CHECK_TICKS} 刻：行宫区块与其实体都已加载时，地灵
 *       不在就先认领附近现有的、没有才补生成（多余的移除）；每 {@value #HOSTING_TICKS} 刻：有主时按
 *       {@link LandModules#HOSTED} 的顺序跑各模块的托管，合计至多 {@value #HOSTING_LIMIT} 个对象；每秒给开着面板的主人
 *       刷新快照（只算他正在看的页）。</li>
 *   <li>{@link #tickPlayerFast} 每刻：福地里跌出最低建造高度就拉回入口（割弃侧没有边界墙）。</li>
 * </ul>
 */

public final class BlissfulLands {

    private BlissfulLands() {}

    public static final int SPIRIT_CHECK_TICKS = 10 * Ticks.SECOND;
    public static final int HOSTING_TICKS = 60 * Ticks.SECOND;
    public static final int HOSTING_LIMIT = 64;
    public static final int SPIRIT_SEARCH = 16;

    public static @Nullable ServerLevel level(@NotNull MinecraftServer server, @NotNull BlissfulLand land) {
        return server.getLevel(land.dimension());
    }

    public static @Nullable BlissfulLand landOf(@NotNull Level level) {
        for (BlissfulLand land : BlissfulLand.ALL) {
            if (land.dimension() == level.dimension()) return land;
        }
        return null;
    }

    public static @NotNull EntityType<EarthSpiritEntity> spiritType(@NotNull BlissfulLand land) {
        return ModEntityTypes.EARTH_SPIRIT_FOX_IMMORTAL_BLISSFUL_LAND.get();
    }

    //region Ownership [认主与失主]
    public static void tickPlayer(@NotNull ServerPlayer player) {
        BlissfulLand land = landOf(player.level());
        if (land == null) return;

        BlissfulLandData data = BlissfulLandData.get(player.server, land);
        RealmSite site = SecretRealms.siteOf(player.server, land.realm());
        if (site != null) MemberService.tryClaim(player.serverLevel(), data, site, land.summitY(), player);
    }

    public static void onDeath(@NotNull ServerPlayer player) {
        if (player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        loseAll(player);
    }

    public static void onReset(@NotNull ServerPlayer player) { loseAll(player); }

    private static void loseAll(ServerPlayer player) {
        for (BlissfulLand land : BlissfulLand.ALL) {
            BlissfulLandData data = BlissfulLandData.get(player.server, land);
            if (!data.isOwner(player.getUUID())) continue;
            MemberService.lose(player.server, level(player.server, land), data);
        }
    }

    public static boolean isExempt(@NotNull ServerPlayer player) {
        BlissfulLand land = landOf(player.level());
        return land != null && MemberService.isExempt(BlissfulLandData.get(player.server, land), player);
    }
    //endregion

    //region Ticks [调度]
    public static void tickServer(@NotNull MinecraftServer server) {
        int tick = server.getTickCount();
        for (BlissfulLand land : BlissfulLand.ALL) {
            ServerLevel level = level(server, land);
            if (level == null) continue;

            TerritoryWork.tick(level);
            BlissfulLandData data = BlissfulLandData.get(server, land);
            if (tick % SPIRIT_CHECK_TICKS == 0) keepSpirit(level, land, data);
            if (tick % HOSTING_TICKS == 0 && data.hasOwner()) host(server, level, data);
        }
        if (tick % Ticks.SECOND == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.containerMenu instanceof EarthSpiritMenu)) continue;
                EarthSpiritPanel.refresh(player, Component.empty());
            }
        }
    }

    public static int host(@NotNull MinecraftServer server, @NotNull ServerLevel level,
                           @NotNull BlissfulLandData data) {
        UUID owner = data.owner();
        ServerPlayer online = owner == null ? null : server.getPlayerList().getPlayer(owner);
        int used = 0;
        for (LandModule<?> module : LandModules.HOSTED) {
            if (used >= HOSTING_LIMIT) break;
            used += module.host(level, data, online, HOSTING_LIMIT - used);
        }
        LandCensus.invalidate(level);
        return used;
    }

    public static int secondsToHosting(@NotNull MinecraftServer server) {
        return EarthSpiritPanel.seconds(HOSTING_TICKS - server.getTickCount() % HOSTING_TICKS);
    }

    public static void keepSpirit(@NotNull ServerLevel level, @NotNull BlissfulLand land,
                                  @NotNull BlissfulLandData data) {
        BlockPos palace = land.palace();
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
        EarthSpiritEntity spirit = spiritType(land).create(level);
        if (spirit == null) return;
        spirit.moveTo(palace.getX() + 0.5D, palace.getY(), palace.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(spirit);
        data.setSpirit(spirit.getUUID());
    }

    public static void onChunkLoad(@NotNull ServerLevel level, @NotNull LevelChunk chunk) {
        BlissfulLand land = landOf(level);
        if (land == null) return;
        TerritoryWork.onChunkLoad(level, BlissfulLandData.get(level.getServer(), land).territory(), chunk);
    }

    public static void tickPlayerFast(@NotNull ServerPlayer player) {
        if (landOf(player.level()) == null || player.getY() >= player.level().getMinBuildHeight()) return;

        ModDimensions.AnchoredDimension anchor = ModDimensions.ANCHORED_DIMENSIONS.get(player.level().dimension());
        if (anchor == null) return;
        DimensionTravelService.rescueIfBelowVoid(player,
                DimensionTravelService.getArrival(player.serverLevel(), anchor));
    }
    //endregion
}
