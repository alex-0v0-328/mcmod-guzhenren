package net.alex.guzhenren.gameplay.immortalaperture.mappipeline;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * 一块福地的地图服务端状态（只在内存）：地图片缓存、扫描队列、探索在途表、区块是否已生成；由 {@link LandMaps} 每刻驱动。
 * <ul>
 *   <li>{@link #enqueue} 排到队尾，{@link #onLoaded} 把刚加载的区块插到队首；{@link #scan} 每刻至多出队
 *       {@value #SCAN_BUDGET} 个：疆域外给整片虚空，已加载的扫，没加载但缓存里有的发缓存，在途的放回队尾，其余丢弃
 *       （归探索管）。结果写缓存，交给想要它的订阅者（哈希没变的不发）。</li>
 *   <li>{@link #explore}：清掉过期的在途项，再沿各订阅者的 {@link MapViewer#order} 往下找「疆域内、没加载、缓存里
 *       没有、不在途」的区块，只加载已生成的（Alex 定，没生成的留作迷雾）：生成与否先异步读区域文件判（同时至多
 *       {@value #READS_IN_FLIGHT} 个，结果记到停服），已生成的用 {@link #TICKET}（半径 0 即 FULL 状态、
 *       {@value #TICKET_TICKS} 刻自动过期）请求加载；同时在途至多 {@value #IN_FLIGHT}，每刻至多看
 *       {@value #EXPLORE_LOOKS} 个候选。扫到后立即撤票。</li>
 *   <li>{@link #reveal}（探明，主人在地图上点迷雾）：不论生成与否直接挂票，没生成的就此生成；限流在 {@link LandMaps}。
 *       区块加载事件（{@link #onLoaded}、{@link #markGenerated}）把它记成已生成。</li>
 *   <li>缓存按访问顺序至多 {@value #CACHE_TILES} 片（约 12 MB），只服务「没加载的区块」与「哈希比对」；淘汰掉的区块
 *       下次要时重新探索。</li>
 *   <li>⚠ 一刻之内：扫描 ≤ {@value #SCAN_BUDGET} 片、探索 ≤ {@value #EXPLORE_LOOKS} 次查看与
 *       ≤ {@value #IN_FLIGHT} 张票；队列长度受订阅视口（≤ 64 × 64）约束，入队按区块去重。</li>
 * </ul>
 */

final class LandMap {

    static final int SCAN_BUDGET = 128;
    static final int IN_FLIGHT = 16;
    static final int EXPLORE_LOOKS = 512;
    static final int TICKET_TICKS = 5 * Ticks.SECOND;
    static final int CACHE_TILES = 8192;
    static final int READS_IN_FLIGHT = 32;
    private static final byte GENERATED = 1;
    private static final byte ABSENT = -1;
    private static final byte READING = 2;

    private record Read(long chunk, boolean generated) {}
    static final TicketType<ChunkPos> TICKET = TicketType.create("guzhenren_land_map",
            Comparator.comparingLong(ChunkPos::toLong), TICKET_TICKS);
    private final LongLinkedOpenHashSet queue = new LongLinkedOpenHashSet();
    private final Long2LongOpenHashMap inFlight = new Long2LongOpenHashMap();
    private final Long2ByteOpenHashMap generated = new Long2ByteOpenHashMap();
    private final Queue<Read> readResults = new ConcurrentLinkedQueue<>();
    private final LongArrayFIFOQueue ready = new LongArrayFIFOQueue();
    private int reads;
    private final Map<Long, MapTile> cache = new LinkedHashMap<>(16, 0.75F, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, MapTile> eldest) { return size() > CACHE_TILES; }
    };

    void enqueue(long chunk) { this.queue.add(chunk); }

    void onLoaded(long chunk) {
        markGenerated(chunk);
        this.queue.addAndMoveToFirst(chunk);
    }

    void markGenerated(long chunk) { this.generated.put(chunk, GENERATED); }

    boolean isIdle() { return this.queue.isEmpty() && this.inFlight.isEmpty(); }

    //region Scan [扫描]
    void scan(@NotNull ServerLevel level, @NotNull Territory territory, @NotNull List<MapViewer> viewers) {
        for (int budget = SCAN_BUDGET; budget > 0 && !this.queue.isEmpty(); budget--) {
            long chunk = this.queue.removeFirstLong();
            if (!wanted(viewers, chunk)) continue;

            int chunkX = ChunkPos.getX(chunk);
            int chunkZ = ChunkPos.getZ(chunk);
            MapTile tile;
            if (!territory.contains(chunkX, chunkZ)) {
                tile = MapTile.empty(chunkX, chunkZ);
            } else if (level.getChunkSource().getChunkNow(chunkX, chunkZ) instanceof LevelChunk loaded) {
                tile = MapScanner.scan(loaded);
                if (this.inFlight.containsKey(chunk)) {
                    this.inFlight.remove(chunk);
                    level.getChunkSource().removeRegionTicket(TICKET, loaded.getPos(), 0, loaded.getPos());
                }
            } else {
                MapTile cached = this.cache.get(chunk);
                if (cached != null) {
                    for (MapViewer viewer : viewers) viewer.offer(cached);
                } else if (this.inFlight.containsKey(chunk)) {
                    this.queue.add(chunk);
                }
                continue;
            }
            this.cache.put(chunk, tile);
            for (MapViewer viewer : viewers) viewer.offer(tile);
        }
    }

    private static boolean wanted(List<MapViewer> viewers, long chunk) {
        for (MapViewer viewer : viewers) {
            if (viewer.wants(chunk)) return true;
        }
        return false;
    }
    //endregion

    //region Explore [探索]
    void explore(@NotNull ServerLevel level, @NotNull Territory territory, @NotNull List<MapViewer> viewers,
                 long now) {
        this.inFlight.values().removeIf((long expiry) -> expiry <= now);
        for (Read read = this.readResults.poll(); read != null; read = this.readResults.poll()) {
            this.reads--;
            this.generated.put(read.chunk(), read.generated() ? GENERATED : ABSENT);
            if (read.generated()) this.ready.enqueue(read.chunk());
        }
        while (!this.ready.isEmpty() && this.inFlight.size() < IN_FLIGHT) {
            long chunk = this.ready.dequeueLong();
            if (wanted(viewers, chunk) && needsLoad(level, territory, chunk)) load(level, chunk, now);
        }
        int looks = EXPLORE_LOOKS;
        for (MapViewer viewer : viewers) {
            while (this.inFlight.size() < IN_FLIGHT && looks > 0 && viewer.exploreCursor < viewer.order.length) {
                long chunk = viewer.order[viewer.exploreCursor++];
                looks--;
                if (!needsLoad(level, territory, chunk)) continue;
                byte known = this.generated.get(chunk);
                if (known == GENERATED) {
                    load(level, chunk, now);
                } else if (known == 0 && this.reads < READS_IN_FLIGHT) {
                    this.generated.put(chunk, READING);
                    this.reads++;
                    level.getChunkSource().chunkMap.read(new ChunkPos(chunk)).whenComplete((tag, failure) ->
                            this.readResults.add(new Read(chunk, failure == null && tag.isPresent()
                                    && ChunkSerializer.getChunkTypeFromTag(tag.get()) == ChunkType.LEVELCHUNK)));
                }
            }
        }
    }

    void reveal(@NotNull ServerLevel level, @NotNull Territory territory, long chunk, long now) {
        markGenerated(chunk);
        if (needsLoad(level, territory, chunk)) load(level, chunk, now);
    }

    private boolean needsLoad(ServerLevel level, Territory territory, long chunk) {
        int chunkX = ChunkPos.getX(chunk);
        int chunkZ = ChunkPos.getZ(chunk);
        return territory.contains(chunkX, chunkZ) && !this.cache.containsKey(chunk) && !this.inFlight.containsKey(chunk)
                && level.getChunkSource().getChunkNow(chunkX, chunkZ) == null;
    }

    private void load(ServerLevel level, long chunk, long now) {
        ChunkPos pos = new ChunkPos(chunk);
        level.getChunkSource().addRegionTicket(TICKET, pos, 0, pos);
        this.inFlight.put(chunk, now + TICKET_TICKS);
        this.queue.add(chunk);
    }
    //endregion
}
