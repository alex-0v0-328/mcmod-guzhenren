package net.alex.guzhenren.client.radial;

/**
 * 扇形选择器的几何。
 *
 * <ul>
 *   <li>不引用游戏类，纯 JUnit 可钉住；扇区 0 正上方居中，其余顺时针；GUI 坐标（x 右，y 下），原点为菜单中心。</li>
 *   <li>{@link #sectorAt}：所指扇区，死区或无扇区为 {@link #NONE}；{@link #centerAngle}：中心自正上方顺时针弧度。</li>
 *   <li>环跨 {@link #INNER_RADIUS} 到 {@link #OUTER_RADIUS} GUI 像素，内圈即死区；图标在 {@link #ICON_RADIUS}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see RadialMenu
 * @since 1.0.0
 */

public final class RadialLayout {

    public static final int NONE = -1;
    public static final int INNER_RADIUS = 30;
    public static final int OUTER_RADIUS = 80;
    public static final int ICON_RADIUS = 55;
    private static final double TURN = 2.0D * Math.PI;

    private RadialLayout() {}

    public static int sectorAt(double x, double y, int count, double deadZone) {
        if (count <= 0 || x * x + y * y < deadZone * deadZone) return NONE;

        double angle = Math.atan2(x, -y);
        if (angle < 0.0D) angle += TURN;
        double step = TURN / count;
        return (int) Math.floor((angle + step / 2.0D) / step) % count;
    }

    public static double centerAngle(int index, int count) { return index * TURN / count; }

    public static double x(double radius, double angle) { return radius * Math.sin(angle); }

    public static double y(double radius, double angle) { return -radius * Math.cos(angle); }
}
