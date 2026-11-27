package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandLookup;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 福地地图的服务端入口：视口订阅、每刻驱动扫描/探索/下发、每秒活物标记，以及区块加载、下线、停服的清理。订阅资格
 * （{@link MapViewerGate}）与标记来源（{@link MapMarkSource}）由组装层传入，本类不认识面板与功能。
 * <ul>
 *   <li>{@link #view}（地图视口载荷，经组装层转来）复判：订阅门给出的福地；
 *       空视口（{@code min > max}）即退订，非空但越界（每边 &gt; {@value MapView#MAX_SIDE} 区块）整包忽略。不看面板
 *       停在哪页：客户端离开地图页时自己发空视口退订。</li>
 *   <li>换视口限流：每人每秒至多接受 {@value ViewLimiter#LIMIT} 个非空视口（{@link ViewLimiter}），超出的整包忽略；
 *       退订不限。{@link #reveal}（探明，经面板操作转来，一次至多 {@value #REVEAL_SIDE}×{@value #REVEAL_SIDE} 区块）另有
 *       一份同样的限流，按区块计。限流记录按人存到下线或停服。</li>
 *   <li>订阅即排扫视口内还没发过的区块；之后每 {@value #RESCAN_TICKS} 刻把视口内已加载的区块重排一遍，地图片哈希
 *       没变的不发；区块加载事件里，有订阅覆盖的插队扫描。扫描与探索的预算见 {@link LandMap}。</li>
 *   <li>下发：每人每刻至多 {@value #PACKETS_PER_TICK} 包、每包至多 {@value #TILES_PER_PACKET} 片，近的先发；
 *       标记每 {@value #MARK_TICKS} 刻一份（{@link MapMarks}）；都经 {@link MapTilesEvent}、{@link MapMarksEvent}
 *       交网络层。</li>
 *   <li>每刻（组装层调 {@link #tick}）先复判全部订阅者，失效的（下线、关面板、走远、失主）直接退订；下线另有事件即时
 *       清，关面板由组装层调 {@link #forget}。</li>
 *   <li>⚠ 状态只在内存：订阅与发送记录按人，缓存与在途表按福地（停服清空）。福地维度不存在时不做事。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class LandMaps {

    private LandMaps() {}

    public static final int RESCAN_TICKS = 10 * Ticks.SECOND;
    public static final int MARK_TICKS = Ticks.SECOND;
    public static final int TILES_PER_PACKET = 32;
    public static final int PACKETS_PER_TICK = 2;
    private static final Map<UUID, MapViewer> VIEWERS = new LinkedHashMap<>();
    private static final Map<BlissfulLand, LandMap> MAPS = new HashMap<>();
    public static final int REVEAL_SIDE = 2;
    private static final Map<UUID, ViewLimiter> LIMITERS = new HashMap<>();
    private static final Map<UUID, ViewLimiter> REVEAL_LIMITERS = new HashMap<>();

    //region Subscribe [订阅]
    public static void view(@NotNull ServerPlayer player, @NotNull MapView view, @NotNull MapViewerGate gate) {
        if (view.isEmpty()) {
            forget(player.getUUID());
            return;
        }
        if (!view.isValid() || !LIMITERS.computeIfAbsent(player.getUUID(), key -> new ViewLimiter())
                .tryAccept(player.server.getTickCount())) {
            return;
        }
        BlissfulLand land = gate.viewing(player);
        if (land == null) return;

        MapViewer viewer = VIEWERS.get(player.getUUID());
        if (viewer == null || viewer.land != land) {
            viewer = new MapViewer(player.getUUID(), land);
            VIEWERS.put(player.getUUID(), viewer);
        }
        if (viewer.view.equals(view)) return;

        LandMap map = MAPS.computeIfAbsent(land, key -> new LandMap());
        for (long chunk : viewer.setView(view)) map.enqueue(chunk);
        viewer.nextRescan = player.server.getTickCount() + RESCAN_TICKS;
        viewer.nextMarks = player.server.getTickCount();
    }

    public static boolean isViewing(@NotNull UUID player) { return VIEWERS.containsKey(player); }

    public static boolean reveal(@NotNull ServerPlayer player, int chunkX, int chunkZ, int side,
                                 @NotNull MapViewerGate gate) {
        BlissfulLand land = gate.viewing(player);
        ServerLevel level = land == null ? null : BlissfulLandLookup.level(player.server, land);
        if (level == null || side < 1 || side > REVEAL_SIDE || !REVEAL_LIMITERS.computeIfAbsent(player.getUUID(),
                key -> new ViewLimiter()).tryAccept(player.server.getTickCount(), side * side)) {
            return false;
        }
        Territory territory = BlissfulLandData.get(player.server, land).territory();
        LandMap map = MAPS.computeIfAbsent(land, key -> new LandMap());
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                map.reveal(level, territory, ChunkPos.asLong(chunkX + dx, chunkZ + dz), player.server.getTickCount());
            }
        }
        return true;
    }

    /** 这个区块在地图上已不是迷雾（扫过地图片或已知生成过）；传送只许落在这样的区块。 */
    public static boolean isKnown(@NotNull BlissfulLand land, int chunkX, int chunkZ) {
        LandMap map = MAPS.get(land);
        return map != null && map.isKnown(ChunkPos.asLong(chunkX, chunkZ));
    }

    public static void forget(@NotNull UUID player) { VIEWERS.remove(player); }
    //endregion

    //region Tick [每刻]
    public static void tick(@NotNull MinecraftServer server, @NotNull MapViewerGate gate,
                            @NotNull List<MapMarkSource> sources) {
        if (VIEWERS.isEmpty()) return;
        long now = server.getTickCount();
        Map<BlissfulLand, List<MapViewer>> byLand = new HashMap<>();
        Iterator<MapViewer> iterator = VIEWERS.values().iterator();
        while (iterator.hasNext()) {
            MapViewer viewer = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(viewer.player);
            if (player == null || gate.viewing(player) != viewer.land) {
                iterator.remove();
                continue;
            }
            byLand.computeIfAbsent(viewer.land, land -> new ArrayList<>()).add(viewer);
        }
        for (Map.Entry<BlissfulLand, List<MapViewer>> entry : byLand.entrySet()) {
            ServerLevel level = BlissfulLandLookup.level(server, entry.getKey());
            if (level != null) tickLand(server, level, entry.getKey(), entry.getValue(), now, sources);
        }
    }

    private static void tickLand(MinecraftServer server, ServerLevel level, BlissfulLand land,
                                 List<MapViewer> viewers, long now, List<MapMarkSource> sources) {
        BlissfulLandData data = BlissfulLandData.get(server, land);
        LandMap map = MAPS.computeIfAbsent(land, key -> new LandMap());
        for (MapViewer viewer : viewers) {
            if (now < viewer.nextRescan) continue;
            for (long chunk : viewer.order) {
                if (level.getChunkSource().getChunkNow(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)) != null) {
                    map.enqueue(chunk);
                }
            }
            viewer.exploreCursor = 0;
            viewer.nextRescan = now + RESCAN_TICKS;
        }
        map.scan(level, data.territory(), viewers);
        map.explore(level, data.territory(), viewers, now);
        for (MapViewer viewer : viewers) {
            ServerPlayer player = server.getPlayerList().getPlayer(viewer.player);
            if (player == null) continue;
            for (int i = 0; i < PACKETS_PER_TICK && viewer.hasOutbox(); i++) {
                ModEvents.post(new MapTilesEvent(player, viewer.drain(TILES_PER_PACKET)));
            }
            if (now >= viewer.nextMarks) {
                viewer.nextMarks = now + MARK_TICKS;
                ModEvents.post(new MapMarksEvent(player, MapMarks.collect(level, data, viewer.view, sources)));
            }
        }
    }
    //endregion

    //region Events [事件]
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        BlissfulLand land = BlissfulLandLookup.landOf(level);
        LandMap map = land == null ? null : MAPS.get(land);
        if (map == null) return;

        long pos = chunk.getPos().toLong();
        map.markGenerated(pos);
        for (MapViewer viewer : VIEWERS.values()) {
            if (viewer.land == land && viewer.wants(pos)) {
                map.onLoaded(pos);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        forget(event.getEntity().getUUID());
        LIMITERS.remove(event.getEntity().getUUID());
        REVEAL_LIMITERS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        VIEWERS.clear();
        MAPS.clear();
        LIMITERS.clear();
        REVEAL_LIMITERS.clear();
    }
    //endregion
}
