package net.alex.guzhenren.client.screen.immortalaperture.map;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapMark;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapTile;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapView;
import net.alex.guzhenren.network.ModPayloads;
import net.alex.guzhenren.network.payload.EarthSpiritMapMarksPayload;
import net.alex.guzhenren.network.payload.EarthSpiritMapTilesPayload;
import net.alex.guzhenren.network.payload.EarthSpiritMapViewPayload;
import net.minecraft.Util;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地地图的客户端仓库：按区块存服务端下发的 {@link MapTile} 与最新一份活物标记，并把地图页的视口订阅发上去；
 * 地图渲染只读它。
 * <ul>
 *   <li>{@link #tile} 没有即迷雾；{@link #revision} 每次地图片增删自增（含 {@link #clear} 与换视口时的裁剪），
 *       渲染方比对它决定重建缓存；{@link #marks} 每秒整份替换。</li>
 *   <li>{@link #requestView}：地图页每帧或每刻把当前视口交来即可，相同的不重发，距上次发送不足
 *       {@value #THROTTLE_MS} 毫秒的先记下、到点由客户端刻补发；边长超过 {@value MapView#MAX_SIDE} 区块的以中心裁到
 *       上限。发出时按 {@link MapView#keeps} 丢掉远处的地图片（服务端同规则忘掉已发记录，回来时重发）。</li>
 *   <li>⚠ 服务端每人每秒只收 4 个视口，抖动时最后一包可能被丢：订阅中每 {@value #RESEND_MS} 毫秒没发过就重发一次当前
 *       视口（服务端同视口直接返回），丢包至多停留这么久。</li>
 *   <li>{@link #stopView}：离开地图页时立即发空视口退订，已收的地图片留着；{@link #clear}：关面板时清空一切并
 *       退订。</li>
 *   <li>⚠ 下行处理器在 {@link FMLClientSetupEvent} 里经 {@link ModPayloads#handleOnClient} 登记；断线时清空。只在
 *       客户端主线程读写。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class LandMapClient {

    private LandMapClient() {}

    public static final long THROTTLE_MS = 250L;
    public static final long RESEND_MS = 1000L;
    private static final Long2ObjectOpenHashMap<MapTile> TILES = new Long2ObjectOpenHashMap<>();
    private static List<MapMark> marks = List.of();
    private static int revision;
    private static MapView sent = MapView.NONE;
    private static MapView wanted = MapView.NONE;
    private static long sentAt = Long.MIN_VALUE;

    //region Read [读取]
    public static @Nullable MapTile tile(int chunkX, int chunkZ) { return TILES.get(ChunkPos.asLong(chunkX, chunkZ)); }

    public static int revision() { return revision; }

    public static @NotNull List<MapMark> marks() { return marks; }

    public static @NotNull MapView view() { return sent; }
    //endregion

    //region View [视口]
    public static void requestView(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        wanted = clamp(new MapView(minChunkX, minChunkZ, maxChunkX, maxChunkZ));
        flush();
    }

    public static void stopView() {
        wanted = MapView.NONE;
        if (sent.isEmpty()) return;
        sent = MapView.NONE;
        sentAt = Util.getMillis();
        PacketDistributor.sendToServer(payload(MapView.NONE));
    }

    public static void clear() {
        stopView();
        TILES.clear();
        marks = List.of();
        sentAt = Long.MIN_VALUE;
        revision++;
    }

    private static void flush() {
        if (wanted.isEmpty()) return;
        long now = Util.getMillis();
        boolean stale = sentAt != Long.MIN_VALUE && now - sentAt >= RESEND_MS;
        if (wanted.equals(sent) && !stale) return;
        if (sentAt != Long.MIN_VALUE && now - sentAt < THROTTLE_MS) return;

        MapView view = wanted;
        sent = view;
        sentAt = now;
        if (TILES.keySet().removeIf((long chunk) -> !view.keeps(chunk))) revision++;
        PacketDistributor.sendToServer(payload(view));
    }

    private static MapView clamp(MapView view) {
        if (view.isEmpty()) return MapView.NONE;
        int minX = view.minChunkX();
        int minZ = view.minChunkZ();
        if (view.width() > MapView.MAX_SIDE) minX += (view.width() - MapView.MAX_SIDE) / 2;
        if (view.depth() > MapView.MAX_SIDE) minZ += (view.depth() - MapView.MAX_SIDE) / 2;
        return new MapView(minX, minZ, Math.min(view.maxChunkX(), minX + MapView.MAX_SIDE - 1),
                Math.min(view.maxChunkZ(), minZ + MapView.MAX_SIDE - 1));
    }

    private static EarthSpiritMapViewPayload payload(MapView view) {
        return new EarthSpiritMapViewPayload(view);
    }
    //endregion

    //region Notices [下行]
    public static void onTiles(@NotNull EarthSpiritMapTilesPayload payload) {
        if (payload.tiles().isEmpty()) return;
        for (MapTile tile : payload.tiles()) TILES.put(tile.chunk(), tile);
        revision++;
    }

    public static void onMarks(@NotNull EarthSpiritMapMarksPayload payload) { marks = payload.marks(); }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModPayloads.handleOnClient(EarthSpiritMapTilesPayload.class, LandMapClient::onTiles);
            ModPayloads.handleOnClient(EarthSpiritMapMarksPayload.class, LandMapClient::onMarks);
        });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) { flush(); }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        TILES.clear();
        marks = List.of();
        sent = MapView.NONE;
        wanted = MapView.NONE;
        sentAt = Long.MIN_VALUE;
        revision++;
    }
    //endregion
}
