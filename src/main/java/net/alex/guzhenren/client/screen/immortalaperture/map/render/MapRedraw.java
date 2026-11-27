package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import org.jetbrains.annotations.Nullable;

/**
 * 地图贴图的一帧参数与重画判定：{@link MapSurface} 每帧拿「已显示的帧」对照相机现状问 {@link #check}，纯函数便于单测。
 * <ul>
 *   <li>贴图边长为视口的 {@value #SCALE} 倍（{@link #around} 以视口为中心）；缩放、旋转、视角、迷雾平面或贴图尺寸一变
 *       即 {@link Reason#LAYOUT}。</li>
 *   <li>小幅平移只挪贴图：视口离开贴图内框（四边各让出余量的 {@code 1/}{@value #PAN_MARGIN_DIVISOR}）才
 *       {@link Reason#PAN}。</li>
 *   <li>地图片版本（{@code LandMapClient.revision()}）变了且距上次开画 ≥ {@value #DATA_THROTTLE_MS} 毫秒才
 *       {@link Reason#DATA}，挡住地图片成批到达时的连画。</li>
 * </ul>
 */

public record MapRedraw(int zoom, int rotation, boolean plan, int fogPlane, long revision, int originX, int originY,
                        int width, int height) {

    public static final double SCALE = 1.5;
    public static final long DATA_THROTTLE_MS = 250L;
    static final int PAN_MARGIN_DIVISOR = 3;

    public enum Reason { NONE, FIRST, LAYOUT, PAN, DATA }

    public static MapRedraw around(MapCamera camera, long revision) {
        int width = (int) Math.ceil(camera.width() * SCALE);
        int height = (int) Math.ceil(camera.height() * SCALE);
        return new MapRedraw(camera.zoom(), camera.rotation(), camera.plan(), camera.fogPlane(), revision,
                camera.originX() - (width - camera.width()) / 2, camera.originY() - (height - camera.height()) / 2,
                width, height);
    }

    public boolean sameLayout(MapRedraw other) {
        return zoom == other.zoom && rotation == other.rotation && plan == other.plan && fogPlane == other.fogPlane
                && width == other.width && height == other.height;
    }

    public static Reason check(@Nullable MapRedraw shown, MapRedraw wanted, int viewX, int viewY, int viewWidth,
                               int viewHeight, long sinceLastMillis) {
        if (shown == null) return Reason.FIRST;
        if (!shown.sameLayout(wanted)) return Reason.LAYOUT;
        int marginX = (shown.width - viewWidth) / 2 / PAN_MARGIN_DIVISOR;
        int marginY = (shown.height - viewHeight) / 2 / PAN_MARGIN_DIVISOR;
        if (viewX < shown.originX + marginX || viewY < shown.originY + marginY
                || viewX + viewWidth > shown.originX + shown.width - marginX
                || viewY + viewHeight > shown.originY + shown.height - marginY) {
            return Reason.PAN;
        }
        if (shown.revision != wanted.revision && sinceLastMillis >= DATA_THROTTLE_MS) return Reason.DATA;
        return Reason.NONE;
    }
}
