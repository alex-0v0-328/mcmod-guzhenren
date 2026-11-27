package net.alex.guzhenren.client.screen.immortalaperture.map.render;

import org.jetbrains.annotations.Nullable;

/**
 * 地图投影[Map Projection]：方块坐标与高度 ↔ 全局地图像素，等距 2:1 菱形与俯视两套、各四个 90° 旋转；光栅、拾取与叠加层
 * 共用这一份换算。
 * <ul>
 *   <li>先把世界列 (x, z) 转进旋转格 (U, V)：旋转 0 时 U = x、V = z，每加 1 地图顺时针转 90°；
 *       {@link #columnU}/{@link #columnV} 与 {@link #columnX}/{@link #columnZ} 互逆，连续坐标用 {@link #u}/{@link #v}
 *       与 {@link #x}/{@link #z}。</li>
 *   <li>等距：q = {@link #quarter} = 菱形宽 / 4，格宽 {@link #cell} = 2q；点 (u, v, y) 投到 sx = (u − v)·2q、
 *       sy = (u + v)·q − y·2q，一格高 2q 像素（与菱形高相等）。列 (U, V) 顶面在平面 P = 顶层方块 y + 1，北角是锚点
 *       （{@link #anchorX}, {@link #anchorY}）。</li>
 *   <li>像素按中心归属，中心永不落在边线上，没有并列：顶面第 j 行占锚点相对列 [−m, m)，m = {@link #topHalf}；左崖面
 *       （朝 +V）第 i 列（i ∈ [−2q, 0)）从 {@link #leftStart} 行起、右崖面（朝 +U，i ∈ [0, 2q)）从 {@link #rightStart}
 *       行起，每露出 1 格往下 2q 行；{@link #face} 是两边共用的判定。</li>
 *   <li>俯视：一格 2q × 2q 像素，sx = u·2q、sy = v·2q，与高度无关，{@link #face} 只返回顶面。</li>
 *   <li>全局像素原点固定在世界原点，视口与贴图各记自己左上角的全局像素。</li>
 * </ul>
 */

public record MapProjection(int quarter, int rotation, boolean plan) {

    public static final int ZOOM_LEVELS = 3;

    public enum Face { TOP, LEFT, RIGHT }

    public static MapProjection of(int zoom, int rotation, boolean plan) {
        return new MapProjection(blockWidth(Math.clamp(zoom, 0, ZOOM_LEVELS - 1)) / 4, Math.floorMod(rotation, 4),
                plan);
    }

    /** 缩放档 0 / 1 / 2 的菱形宽：4 / 8 / 16 地图像素。 */
    public static int blockWidth(int zoom) { return 4 << zoom; }

    public int blockWidth() { return quarter * 4; }

    public int cell() { return quarter * 2; }

    //region Rotation [旋转]
    public int columnU(int x, int z) {
        return switch (rotation) {
            case 1 -> -z - 1;
            case 2 -> -x - 1;
            case 3 -> z;
            default -> x;
        };
    }

    public int columnV(int x, int z) {
        return switch (rotation) {
            case 1 -> x;
            case 2 -> -z - 1;
            case 3 -> -x - 1;
            default -> z;
        };
    }

    public int columnX(int u, int v) {
        return switch (rotation) {
            case 1 -> v;
            case 2 -> -u - 1;
            case 3 -> -v - 1;
            default -> u;
        };
    }

    public int columnZ(int u, int v) {
        return switch (rotation) {
            case 1 -> -u - 1;
            case 2 -> -v - 1;
            case 3 -> u;
            default -> v;
        };
    }

    public double u(double x, double z) {
        return switch (rotation) {
            case 1 -> -z;
            case 2 -> -x;
            case 3 -> z;
            default -> x;
        };
    }

    public double v(double x, double z) {
        return switch (rotation) {
            case 1 -> x;
            case 2 -> -z;
            case 3 -> -x;
            default -> z;
        };
    }

    public double x(double u, double v) {
        return switch (rotation) {
            case 1 -> v;
            case 2 -> -u;
            case 3 -> -v;
            default -> u;
        };
    }

    public double z(double u, double v) {
        return switch (rotation) {
            case 1 -> -u;
            case 2 -> -v;
            case 3 -> u;
            default -> v;
        };
    }
    //endregion

    //region Projection [投影]
    public double screenX(double x, double y, double z) {
        double u = u(x, z);
        return plan ? u * cell() : (u - v(x, z)) * cell();
    }

    public double screenY(double x, double y, double z) {
        double v = v(x, z);
        return plan ? v * cell() : (u(x, z) + v) * quarter - y * cell();
    }

    public double worldX(double screenX, double screenY, double y) {
        return x(planeU(screenX, screenY, y), planeV(screenX, screenY, y));
    }

    public double worldZ(double screenX, double screenY, double y) {
        return z(planeU(screenX, screenY, y), planeV(screenX, screenY, y));
    }

    private double planeU(double screenX, double screenY, double y) {
        if (plan) return screenX / cell();
        return (screenX / cell() + (screenY + y * cell()) / quarter) / 2.0;
    }

    private double planeV(double screenX, double screenY, double y) {
        if (plan) return screenY / cell();
        return ((screenY + y * cell()) / quarter - screenX / cell()) / 2.0;
    }

    public int anchorX(int u, int v) { return plan ? u * cell() : (u - v) * cell(); }

    public int anchorY(int u, int v, int plane) { return plan ? v * cell() : (u + v) * quarter - plane * cell(); }
    //endregion

    //region Pixel ownership [像素归属]
    public int topHalf(int row) { return cell() - Math.abs(2 * row + 1 - cell()); }

    public int leftStart(int column) { return cell() + Math.floorDiv(column + 1, 2); }

    public int rightStart(int column) { return cell() - Math.floorDiv(column + 1, 2); }

    public @Nullable Face face(int column, int row, int leftDepth, int rightDepth) {
        int cell = cell();
        if (plan) return column >= 0 && column < cell && row >= 0 && row < cell ? Face.TOP : null;
        if (row >= 0 && row < cell) {
            int half = topHalf(row);
            if (column >= -half && column < half) return Face.TOP;
        }
        if (column >= -cell && column < 0) {
            int start = leftStart(column);
            return row >= start && row < start + leftDepth * cell ? Face.LEFT : null;
        }
        if (column >= 0 && column < cell) {
            int start = rightStart(column);
            return row >= start && row < start + rightDepth * cell ? Face.RIGHT : null;
        }
        return null;
    }
    //endregion
}
