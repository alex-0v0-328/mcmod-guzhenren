package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

/**
 * 地图贴图面[Map Surface]：渲染线程每帧 {@link #update} 判是否重画，要画就拍 {@link MapGrid} 快照交给后台单线程
 * {@link MapRaster}，画好的 {@link NativeImage} 下一帧在渲染线程上传成 {@link DynamicTexture}；{@link #draw} 按相机
 * 把贴图贴进视口并乘昼夜色调。
 * <ul>
 *   <li>重画时机全由 {@link MapRedraw#check} 定；同一时刻至多一个光栅任务在跑，跑完再判下一次，旧贴图一直显示到
 *       新的到位。</li>
 *   <li>只换了缩放时，旧贴图按两档格宽之比放大或缩小先顶着；旋转、视角或迷雾平面变了则旧贴图不画，等新的。</li>
 *   <li>快照按贴图矩形与参照平面下 {@value #CAPTURE_BELOW} 格、上 {@value #CAPTURE_ABOVE} 格反投取区块，每边至多
 *       {@value MapGrid#MAX_SIDE} 区块；快照外的一律当迷雾。</li>
 *   <li>⚠ {@link #close} 必须在关界面时调用：停线程、丢弃未上传的图、释放贴图；之后后台再交来的图当场关闭，不漏
 *       原生内存。</li>
 *   <li>⚠ {@link #update} 与 {@link #draw} 只在渲染线程调；贴图坐标经位姿缩放到 1 地图像素 = 2 真实像素，调用方
 *       负责裁剪框。</li>
 * </ul>
 */

public final class MapSurface implements AutoCloseable {

    public static final int NO_HEIGHT = Integer.MIN_VALUE;
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final int CAPTURE_BELOW = 64;
    private static final int CAPTURE_ABOVE = 192;
    private final ResourceLocation id = Guzhenren.id("dynamic/land_map_" + IDS.getAndIncrement());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Guzhenren land map raster");
        thread.setDaemon(true);
        return thread;
    });
    private final Object lock = new Object();
    private @Nullable Ready ready;
    private volatile boolean closed;
    private @Nullable DynamicTexture texture;
    private @Nullable MapRedraw shown;
    private @Nullable MapRedraw pending;
    private List<MapRaster.Label> labels = List.of();
    private long lastStart = -1L;
    private long lastMillis = -1L;
    private int lowest = NO_HEIGHT;
    private int highest = NO_HEIGHT;

    private record Ready(MapRedraw frame, @Nullable NativeImage image, List<MapRaster.Label> labels, long millis,
                         int lowest, int highest) {}

    //region Render thread [渲染线程]
    public void update(MapCamera camera, TileSource tiles, long revision) {
        if (closed) return;
        accept();
        if (pending != null) return;

        long now = Util.getMillis();
        MapRedraw wanted = MapRedraw.around(camera, revision);
        MapRedraw.Reason reason = MapRedraw.check(shown, wanted, camera.originX(), camera.originY(), camera.width(),
                camera.height(), lastStart < 0L ? Long.MAX_VALUE : now - lastStart);
        if (reason == MapRedraw.Reason.NONE) return;

        MapProjection projection = camera.projection();
        MapGrid grid = MapGrid.capture(tiles, MapCamera.chunksOf(projection, camera.y(), wanted.originX(),
                wanted.originY(), wanted.width(), wanted.height(), CAPTURE_BELOW, CAPTURE_ABOVE, MapGrid.MAX_SIDE));
        pending = wanted;
        lastStart = now;
        worker.execute(() -> render(wanted, projection, grid));
    }

    public void draw(GuiGraphics graphics, MapCamera camera, int guiX, int guiY, double guiScale, MapTint tint) {
        MapRedraw frame = shown;
        if (texture == null || frame == null) return;
        MapProjection now = camera.projection();
        if (frame.rotation() != now.rotation() || frame.plan() != now.plan() || frame.fogPlane() != camera.fogPlane()) {
            return;
        }
        double ratio = (double) now.blockWidth() / MapProjection.blockWidth(frame.zoom());
        double scale = MapCamera.guiPerMapPixel(guiScale);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(guiX, guiY, 0.0F);
        pose.scale((float) scale, (float) scale, 1.0F);
        pose.translate(frame.originX() * ratio - camera.originX(), frame.originY() * ratio - camera.originY(), 0.0);
        pose.scale((float) ratio, (float) ratio, 1.0F);
        graphics.setColor(tint.red(), tint.green(), tint.blue(), 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(id, 0, 0, 0.0F, 0.0F, frame.width(), frame.height(), frame.width(), frame.height());
        RenderSystem.disableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }

    /** 当前贴图的等高线标注（全局地图像素）；版式与相机不一致时为空。 */
    public List<MapRaster.Label> labels(MapCamera camera) {
        MapRedraw frame = shown;
        if (frame == null || frame.zoom() != camera.zoom() || frame.rotation() != camera.rotation()
                || frame.plan() != camera.plan()) {
            return List.of();
        }
        return labels;
    }

    /** 最近一次光栅耗时（毫秒），还没画过为 −1。 */
    public long lastMillis() { return lastMillis; }

    /** 当前贴图快照内柱顶的最低 y；没有陆地时为 {@link #NO_HEIGHT}。 */
    public int lowest() { return lowest; }

    /** 当前贴图快照内柱顶的最高 y；没有陆地时为 {@link #NO_HEIGHT}。 */
    public int highest() { return highest; }

    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
            if (ready != null && ready.image() != null) ready.image().close();
            ready = null;
        }
        worker.shutdownNow();
        if (texture != null) Minecraft.getInstance().getTextureManager().release(id);
        texture = null;
        shown = null;
        pending = null;
    }

    private void accept() {
        Ready next;
        synchronized (lock) {
            next = ready;
            ready = null;
        }
        if (next == null) return;

        pending = null;
        shown = next.frame();
        lastMillis = next.millis();
        NativeImage image = next.image();
        if (image == null) return;
        labels = next.labels();
        lowest = next.lowest();
        highest = next.highest();
        NativeImage current = texture == null ? null : texture.getPixels();
        if (texture != null && current != null && current.getWidth() == image.getWidth()
                && current.getHeight() == image.getHeight()) {
            texture.setPixels(image);
            texture.upload();
            return;
        }
        texture = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }
    //endregion

    //region Worker thread [后台线程]
    private void render(MapRedraw frame, MapProjection projection, MapGrid grid) {
        long start = System.nanoTime();
        NativeImage image = null;
        List<MapRaster.Label> found = List.of();
        int low = NO_HEIGHT;
        int high = NO_HEIGHT;
        try {
            MapRaster.Result result = MapRaster.draw(projection, grid, frame.fogPlane(), frame.originX(),
                    frame.originY(), frame.width(), frame.height());
            image = image(result);
            found = result.labels();
            if (grid.hasLand()) {
                low = grid.minPlane() - 1;
                high = grid.maxPlane() - 1;
            }
        } catch (RuntimeException exception) {
            Guzhenren.LOGGER.error("Land map raster failed", exception);
        }
        long millis = (System.nanoTime() - start) / 1_000_000L;
        synchronized (lock) {
            if (closed) {
                if (image != null) image.close();
                return;
            }
            if (ready != null && ready.image() != null) ready.image().close();
            ready = new Ready(frame, image, found, millis, low, high);
        }
    }

    private static NativeImage image(MapRaster.Result result) {
        NativeImage image = new NativeImage(result.width(), result.height(), false);
        int[] pixels = result.pixels();
        for (int y = 0; y < result.height(); y++) {
            for (int x = 0; x < result.width(); x++) {
                image.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(pixels[y * result.width() + x]));
            }
        }
        return image;
    }
    //endregion
}
