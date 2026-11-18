package net.alex.guzhenren.client.radial;

/**
 * 扇形选择器的纯几何布局计算器。
 * <ul>
 *   <li>不引用任何游戏类，可直接通过纯 JUnit 单元测试钉住几何分布；第 0 号扇区在正上方居中，其余顺时针排列，采用 GUI
 *       坐标系（x 向右、y 向下）并以菜单中心为原点。</li>
 *   <li>{@link #sectorAt} 返回当前指向的扇区索引（位于中心死区或无扇区时返回 {@link #NONE}），{@link #centerAngle} 返
 *       回各扇区中心线自正上方顺时针旋转的弧度。</li>
 *   <li>扇环径向跨越 {@link #INNER_RADIUS} 至 {@link #OUTER_RADIUS} GUI 像素（内圈以内为死区），图标放置在
 *       {@link #ICON_RADIUS} 半径处。</li>
 * </ul>
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
