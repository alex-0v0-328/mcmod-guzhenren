package net.alex.guzhenren.client.radial;

/**
 * The geometry of the radial menu [扇形选择器], kept free of any game class so plain JUnit pins it: sector 0 is
 * centred straight up and the rest follow clockwise, in GUI coordinates (x right, y down) around the menu's centre.
 * {@link #sectorAt} names the sector under a cursor, or {@link #NONE} inside the dead zone or with no sectors;
 * {@link #centerAngle} is a sector's centre as a clockwise angle from straight up, in radians.
 *
 * @author Alex
 * @version 1.0.0
 * @see RadialMenu
 * @since 1.0.0
 */

public final class RadialLayout {

    public static final int NONE = -1;
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
