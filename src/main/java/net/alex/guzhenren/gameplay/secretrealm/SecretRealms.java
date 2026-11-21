package net.alex.guzhenren.gameplay.secretrealm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 天地秘境[Secret Realm]的定位与调度：解出每处秘境的 {@link RealmSite}，按秒分发玩家、区域与死亡钩子。
 * <ul>
 *   <li>山心 = 结构集同心环第一个区块的中心方块 {@code (cx * 16 + 8, cz * 16 + 8)}（与 Gu World 结构片约定），取自
 *       {@link ChunkGeneratorStructureState#getRingPositionsFor}；结构集未注册、不是同心环或环为空（未装 Gu World）时
 *       {@link #siteOf} 恒为 {@code null}，所有钩子不触发。</li>
 *   <li>环位还须经 {@code StructureManager#checkStructurePresence} 确认那个区块真有结构起点才算数：已生成且没有、或尚未
 *       生成但群系不容许（环位落在不生成山的群系）都定为关闭；尚未生成又可能生成时视为暂未开启，每秒至多重查一次，确认
 *       后才缓存。</li>
 *   <li>⚠ 性能：解出的范围按秘境缓存，以所在维度的 {@link ChunkGeneratorStructureState} 实例为键，换存档自动重算；首次解
 *       析可能阻塞等待原版异步算环（世界加载时已开始算）。之后每次查询只是一次映射查找加一次平方距离比较。</li>
 *   <li>⚠ 性能：{@link #tickAreas} 每秒一次，只有范围外扩 {@link #PLAYER_MARGIN} 格内有玩家才调 {@code tickArea}，否则不
 *       查实体；玩家钩子挂在 {@code Heartbeat}，不另起逐刻循环；范围内外各调一个钩子，秘境未解出时两个都不调。</li>
 *   <li>{@link #pin} 只供 GameTest 固定或清除范围；钉住的范围不随生成状态重算，测试结束须传 {@code null} 清除。</li>
 * </ul>
 */

public final class SecretRealms {

    private SecretRealms() {}

    public static final int PLAYER_MARGIN = 32;
    public static final List<SecretRealm> ALL = List.of(SoulsunderMountain.INSTANCE);
    private static final Map<SecretRealm, Resolved> SITES = new HashMap<>();

    private record Resolved(@Nullable ChunkGeneratorStructureState state, @Nullable RealmSite site, boolean settled,
                            int checkedAt) {

        static Resolved settled(@Nullable ChunkGeneratorStructureState state, @Nullable RealmSite site) {
            return new Resolved(state, site, true, 0);
        }
    }

    //region Lookup [定位]
    public static @Nullable RealmSite siteOf(@NotNull MinecraftServer server, @NotNull SecretRealm realm) {
        ServerLevel level = server.getLevel(realm.dimension());
        if (level == null) return null;

        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        Resolved resolved = SITES.get(realm);
        boolean current = resolved != null && (resolved.state() == null || resolved.state() == state);
        if (current && resolved.settled()) return resolved.site();
        if (current && server.getTickCount() - resolved.checkedAt() < Ticks.SECOND) return null;

        resolved = resolve(server, level, realm, state);
        SITES.put(realm, resolved);
        return resolved.settled() ? resolved.site() : null;
    }

    public static @Nullable RealmSite siteAround(@NotNull ServerLevel level, @NotNull SecretRealm realm,
                                                 double x, double z) {
        if (level.dimension() != realm.dimension()) return null;
        RealmSite site = siteOf(level.getServer(), realm);
        return site != null && site.contains(x, z) ? site : null;
    }

    public static void pin(@NotNull SecretRealm realm, @Nullable RealmSite site) {
        if (site == null) {
            SITES.remove(realm);
        } else {
            SITES.put(realm, Resolved.settled(null, site));
        }
    }

    public static void clear() { SITES.clear(); }

    private static Resolved resolve(MinecraftServer server, ServerLevel level, SecretRealm realm,
                                    ChunkGeneratorStructureState state) {
        StructureSet set = server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(realm.structureSet());
        if (set == null || !(set.placement() instanceof ConcentricRingsStructurePlacement rings)) {
            return Resolved.settled(state, null);
        }

        List<ChunkPos> ring = state.getRingPositionsFor(rings);
        if (ring == null || ring.isEmpty()) return Resolved.settled(state, null);

        ChunkPos chunk = ring.getFirst();
        RealmSite site = new RealmSite(new BlockPos(chunk.getMiddleBlockX(), 0, chunk.getMiddleBlockZ()),
                realm.radius());
        boolean unknown = false;
        for (StructureSet.StructureSelectionEntry entry : set.structures()) {
            StructureCheckResult start = level.structureManager()
                    .checkStructurePresence(chunk, entry.structure().value(), rings, false);
            if (start == StructureCheckResult.START_PRESENT) return Resolved.settled(state, site);
            if (start == StructureCheckResult.CHUNK_LOAD_NEEDED) unknown = true;
        }
        return unknown ? new Resolved(state, null, false, server.getTickCount()) : Resolved.settled(state, null);
    }
    //endregion

    //region Dispatch [分发]
    public static void tickPlayer(@NotNull ServerPlayer player) {
        for (SecretRealm realm : ALL) {
            RealmSite site = siteOf(player.server, realm);
            if (site == null) continue;

            if (player.level().dimension() == realm.dimension() && site.contains(player)) realm.tickPlayer(player, site);
            else realm.tickPlayerAway(player, site);
        }
    }

    public static void tickAreas(@NotNull MinecraftServer server) {
        if (server.getTickCount() % Ticks.SECOND != 0) return;

        for (SecretRealm realm : ALL) {
            RealmSite site = siteOf(server, realm);
            if (site == null) continue;

            ServerLevel level = server.getLevel(realm.dimension());
            if (level != null && hasPlayerNear(level, site)) realm.tickArea(level, site);
        }
    }

    public static void onDeath(@NotNull LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;

        for (SecretRealm realm : ALL) {
            RealmSite site = siteAround(level, realm, entity.getX(), entity.getZ());
            if (site != null) realm.onDeath(level, site, entity);
        }
    }

    public static boolean hasPlayerNear(@NotNull ServerLevel level, @NotNull RealmSite site) {
        int distance = site.radius() + PLAYER_MARGIN;
        for (ServerPlayer player : level.players()) {
            if (site.isWithin(player.getX(), player.getZ(), distance)) return true;
        }
        return false;
    }
    //endregion
}
