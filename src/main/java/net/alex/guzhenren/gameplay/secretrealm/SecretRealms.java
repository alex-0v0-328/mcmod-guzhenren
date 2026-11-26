package net.alex.guzhenren.gameplay.secretrealm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.place.PlaceTitles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 天地秘境[Secret Realm]的定位与调度：解出每处秘境的 {@link RealmSite}，按秒分发玩家、区域与死亡钩子。
 * <ul>
 *   <li>山心 = 结构集放置器在维度原点 ±{@link #SCAN_CHUNKS} 区块内首个 {@link StructurePlacement#isStructureChunk} 命中
 *       区块的中心方块 {@code (cx * 16 + 8, cz * 16 + 8)}（与 Gu World 结构约定；Gu World 的 {@code guworld:gentle_site}
 *       在原点 ±32 区块内挑缓地，只按 ID 耦合）。结构集未注册或扫不到（未装 Gu World）时 {@link #siteOf} 恒为
 *       {@code null}，所有钩子不触发。</li>
 *   <li>山底：把命中区块同步生成到 {@code STRUCTURE_STARTS}，取结构起点各结构片边界框的最高顶（山巅最高一层荡魂山石）
 *       减 {@link SecretRealm#summitRise}，记进山心的 y；该区块没有有效起点（群系不容许等）定为关闭。⚠ 不取起点自身的
 *       边界框：地形适配不为 {@code none} 时原版把它四面外扩 12 格。</li>
 *   <li>⚠ 性能：解出的范围按秘境缓存，以所在维度的 {@link ChunkGeneratorStructureState} 实例为键，换存档自动重算；首次
 *       解析扫 67×67 次放置判定（Gu World 挑址在建维度时已异步开算，未算完则等它）并在主线程生成一个区块的结构起点。之
 *       后每次查询只是一次映射查找加一次平方距离比较。</li>
 *   <li>⚠ 性能：{@link #tickAreas} 每秒一次，只有范围外扩 {@link #PLAYER_MARGIN} 格内有玩家才调 {@code tickArea}，否则
 *       不查实体；玩家钩子挂在 {@code Heartbeat}，不另起逐刻循环；范围内外各调一个钩子，秘境未解出时两个都不调。</li>
 *   <li>{@link #tickPlayer} 同时把玩家在不在范围内报给 {@link PlaceTitles}（地点 ID 用结构集 ID），进入时弹秘境的地名标
 *       题。</li>
 *   <li>{@link #pin} 只供 GameTest 把范围钉在给定维度（通常是测试所在的主世界），{@link #pinAbsent} 钉成「没有这处秘
 *       境」；钉住的范围不随生成状态重算，{@link #dimensionOf} 随之返回钉住的维度，测试结束须调 {@link #unpin}。</li>
 * </ul>
 */

public final class SecretRealms {

    private SecretRealms() {}

    public static final int PLAYER_MARGIN = 32;
    public static final int SCAN_CHUNKS = 33;
    public static final List<SecretRealm> ALL = List.of(SoulsunderMountain.INSTANCE);
    private static final Map<SecretRealm, Resolved> SITES = new HashMap<>();
    private static final Map<SecretRealm, Pin> PINS = new HashMap<>();

    private record Resolved(@NotNull ChunkGeneratorStructureState state, @Nullable RealmSite site) {}

    private record Pin(@Nullable ResourceKey<Level> dimension, @Nullable RealmSite site) {}

    //region Lookup [定位]
    public static @Nullable RealmSite siteOf(@NotNull MinecraftServer server, @NotNull SecretRealm realm) {
        Pin pin = PINS.get(realm);
        if (pin != null) return pin.site();

        ServerLevel level = server.getLevel(realm.dimension());
        if (level == null) return null;

        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        Resolved resolved = SITES.get(realm);
        if (resolved != null && resolved.state() == state) return resolved.site();

        resolved = new Resolved(state, resolve(server, level, realm, state));
        SITES.put(realm, resolved);
        return resolved.site();
    }

    public static @NotNull ResourceKey<Level> dimensionOf(@NotNull SecretRealm realm) {
        Pin pin = PINS.get(realm);
        return pin != null && pin.dimension() != null ? pin.dimension() : realm.dimension();
    }

    public static @Nullable RealmSite siteAround(@NotNull ServerLevel level, @NotNull SecretRealm realm,
                                                 double x, double z) {
        if (level.dimension() != dimensionOf(realm)) return null;
        RealmSite site = siteOf(level.getServer(), realm);
        return site != null && site.contains(x, z) ? site : null;
    }

    public static void pin(@NotNull SecretRealm realm, @NotNull ServerLevel level, @NotNull RealmSite site) {
        PINS.put(realm, new Pin(level.dimension(), site));
    }

    public static void pinAbsent(@NotNull SecretRealm realm) { PINS.put(realm, new Pin(null, null)); }

    public static void unpin(@NotNull SecretRealm realm) { PINS.remove(realm); }

    public static void clear() {
        SITES.clear();
        PINS.clear();
    }

    private static @Nullable RealmSite resolve(MinecraftServer server, ServerLevel level, SecretRealm realm,
                                               ChunkGeneratorStructureState state) {
        StructureSet set = server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(realm.structureSet());
        if (set == null) return null;

        ChunkPos chunk = findChunk(state, set.placement());
        if (chunk == null) return null;

        ChunkAccess access = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS);
        for (StructureSet.StructureSelectionEntry entry : set.structures()) {
            StructureStart start = access.getStartForStructure(entry.structure().value());
            if (start == null || !start.isValid()) continue;

            int summit = start.getPieces().stream().mapToInt(piece -> piece.getBoundingBox().maxY()).max()
                    .orElse(start.getBoundingBox().maxY());
            int floor = summit - realm.summitRise();
            return new RealmSite(new BlockPos(chunk.getMiddleBlockX(), floor, chunk.getMiddleBlockZ()), realm.radius());
        }
        return null;
    }

    public static @Nullable ChunkPos findChunk(@NotNull ChunkGeneratorStructureState state,
                                               @NotNull StructurePlacement placement) {
        for (int z = -SCAN_CHUNKS; z <= SCAN_CHUNKS; z++) {
            for (int x = -SCAN_CHUNKS; x <= SCAN_CHUNKS; x++) {
                if (placement.isStructureChunk(state, x, z)) return new ChunkPos(x, z);
            }
        }
        return null;
    }
    //endregion

    //region Dispatch [分发]
    public static void tickPlayer(@NotNull ServerPlayer player) {
        for (SecretRealm realm : ALL) {
            RealmSite site = siteOf(player.server, realm);
            if (site == null) continue;

            boolean inside = player.level().dimension() == dimensionOf(realm) && site.contains(player);
            PlaceTitles.update(player, realm.structureSet().location(), inside, realm.title());
            if (inside) realm.tickPlayer(player, site);
            else realm.tickPlayerAway(player, site);
        }
    }

    public static void tickAreas(@NotNull MinecraftServer server) {
        if (server.getTickCount() % Ticks.SECOND != 0) return;

        for (SecretRealm realm : ALL) {
            RealmSite site = siteOf(server, realm);
            if (site == null) continue;

            ServerLevel level = server.getLevel(dimensionOf(realm));
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
