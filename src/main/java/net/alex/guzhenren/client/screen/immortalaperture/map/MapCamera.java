package net.alex.guzhenren.client.screen.immortalaperture.map;

import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapView;

/**
 * 地图相机[Map Camera]：视口中心（方块坐标）、缩放三档、旋转 0–3、等距或俯视，以及视口大小（地图像素）；只在渲染
 * 线程用。
 * <ul>
 *   <li>1 地图像素 = {@value #REAL_PER_MAP_PIXEL} 真实像素，与 GUI 缩放无关；GUI 坐标 = 视口左上角 + 地图像素 ×
 *       {@link #guiPerMapPixel}。</li>
 *   <li>中心的 y 是参照平面：平移、缩放锚点、迷雾高度（{@link #fogPlane}）都按这一平面换算；它不随拖动变，由地图页
 *       设定。</li>
 *   <li>{@link #originX}/{@link #originY} 是视口左上角的全局地图像素（{@link MapProjection}），
 *       {@link #viewX}/{@link #viewY} 与 {@link #worldX}/{@link #worldZ} 在视口像素与世界之间来回换。</li>
 *   <li>{@link #viewChunks}：视口覆盖的区块矩形，供 {@code LandMapClient.requestView}；等距视角按参照平面下
 *       {@value #VIEW_BELOW} 格到上 {@value #VIEW_ABOVE} 格反投四角取外包，超过 {@value MapView#MAX_SIDE} 区块时围绕
 *       中心区块裁到 {@value MapView#MAX_SIDE}；{@link #chunksOf} 同一算法给贴图用。</li>
 * </ul>
 */

public final class MapCamera {

    public static final int REAL_PER_MAP_PIXEL = 2;
    public static final int VIEW_BELOW = 32;
    public static final int VIEW_ABOVE = 96;
    private double x;
    private double y;
    private double z;
    private int zoom = 1;
    private int rotation;
    private boolean plan;
    private int width = 1;
    private int height = 1;

    public MapCamera(double x, double y, double z) { setCenter(x, y, z); }

    public static double guiPerMapPixel(double guiScale) { return REAL_PER_MAP_PIXEL / guiScale; }

    //region State [状态]
    public double x() { return x; }

    public double y() { return y; }

    public double z() { return z; }

    public int zoom() { return zoom; }

    public int rotation() { return rotation; }

    public boolean plan() { return plan; }

    public int width() { return width; }

    public int height() { return height; }

    public int fogPlane() { return (int) Math.floor(y) + 1; }

    public void setCenter(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setViewport(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    public void setZoom(int zoom) { this.zoom = Math.clamp(zoom, 0, MapProjection.ZOOM_LEVELS - 1); }

    public void setPlan(boolean plan) { this.plan = plan; }

    public void rotate(int steps) { rotation = Math.floorMod(rotation + steps, 4); }

    public MapProjection projection() { return MapProjection.of(zoom, rotation, plan); }
    //endregion

    //region Navigation [导航]
    public void pan(double dx, double dy) {
        MapProjection projection = projection();
        moveTo(projection, projection.screenX(x, y, z) - dx, projection.screenY(x, y, z) - dy);
    }

    public boolean zoom(int steps, double anchorX, double anchorY) {
        int next = Math.clamp(zoom + steps, 0, MapProjection.ZOOM_LEVELS - 1);
        if (next == zoom) return false;

        double pointX = worldX(anchorX, anchorY);
        double pointZ = worldZ(anchorX, anchorY);
        zoom = next;
        MapProjection projection = projection();
        moveTo(projection, projection.screenX(pointX, y, pointZ) - (anchorX - width / 2.0),
                projection.screenY(pointX, y, pointZ) - (anchorY - height / 2.0));
        return true;
    }

    private void moveTo(MapProjection projection, double screenX, double screenY) {
        x = projection.worldX(screenX, screenY, y);
        z = projection.worldZ(screenX, screenY, y);
    }
    //endregion

    //region Viewport [视口]
    public int originX() { return (int) Math.floor(projection().screenX(x, y, z) - width / 2.0); }

    public int originY() { return (int) Math.floor(projection().screenY(x, y, z) - height / 2.0); }

    public double viewX(double x, double y, double z) { return projection().screenX(x, y, z) - originX(); }

    public double viewY(double x, double y, double z) { return projection().screenY(x, y, z) - originY(); }

    public double worldX(double viewX, double viewY) {
        return projection().worldX(originX() + viewX, originY() + viewY, y);
    }

    public double worldZ(double viewX, double viewY) {
        return projection().worldZ(originX() + viewX, originY() + viewY, y);
    }

    public MapView viewChunks() {
        return chunksOf(projection(), y, originX(), originY(), width, height, VIEW_BELOW, VIEW_ABOVE, MapView.MAX_SIDE);
    }

    public static MapView chunksOf(MapProjection projection, double y, int originX, int originY, int width,
                                   int height, int below, int above, int maxSide) {
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double[] planes = projection.plan() ? new double[] { y } : new double[] { y - below, y + above };
        for (double plane : planes) {
            for (int corner = 0; corner < 4; corner++) {
                double screenX = originX + (corner & 1) * width;
                double screenY = originY + (corner >> 1) * height;
                double worldX = projection.worldX(screenX, screenY, plane);
                double worldZ = projection.worldZ(screenX, screenY, plane);
                minX = Math.min(minX, worldX);
                minZ = Math.min(minZ, worldZ);
                maxX = Math.max(maxX, worldX);
                maxZ = Math.max(maxZ, worldZ);
            }
        }
        double centerX = projection.worldX(originX + width / 2.0, originY + height / 2.0, y);
        double centerZ = projection.worldZ(originX + width / 2.0, originY + height / 2.0, y);
        int lowX = lowEnd(chunk(minX), chunk(Math.ceil(maxX) - 1), chunk(centerX), maxSide);
        int lowZ = lowEnd(chunk(minZ), chunk(Math.ceil(maxZ) - 1), chunk(centerZ), maxSide);
        int highX = Math.min(chunk(Math.ceil(maxX) - 1), lowX + maxSide - 1);
        int highZ = Math.min(chunk(Math.ceil(maxZ) - 1), lowZ + maxSide - 1);
        return new MapView(lowX, lowZ, highX, highZ);
    }

    private static int chunk(double block) { return Math.floorDiv((int) Math.floor(block), 16); }

    private static int lowEnd(int min, int max, int center, int maxSide) {
        if (max - min < maxSide) return min;
        int low = Math.max(min, center - maxSide / 2);
        return Math.min(low, max - maxSide + 1);
    }
    //endregion
}
