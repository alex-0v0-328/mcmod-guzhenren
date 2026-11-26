package net.alex.guzhenren.client.screen.blissful.map;

import net.alex.guzhenren.gameplay.blissful.map.MapTerrain;

/**
 * 福地地图调色板：每种 {@link MapTerrain} 一条三阶色带（顶面 / 左崖面 / 右崖面，光从左上来），色带共用 38 色的高饱和
 * 色表，另有迷雾三色；只有地图光栅与图例用，不进 {@code ModPalette}。
 * <ul>
 *   <li>荡魂山石用面板主题粉 {@code #F8ACC4} 一系，胆石用肉红一系；{@link MapTerrain#UNKNOWN} 与
 *       {@link MapTerrain#OTHER} 同用石灰色带，{@link MapTerrain#VOID} 不画（透明）。</li>
 *   <li>昼夜色调不烘进这里，贴图时由 {@link MapTint} 统一相乘。</li>
 * </ul>
 */

public final class MapPalette {

    public static final int FOG = 0xFF2E2840;
    public static final int FOG_ALT = 0xFF342D48;
    public static final int FOG_SIDE = 0xFF221D30;
    private static final int GRASS_TOP = 0xFF8BD650;
    private static final int GRASS_MID = 0xFF55A83E;
    private static final int GRASS_DARK = 0xFF2F7434;
    private static final int LEAF_TOP = 0xFF3E9E4A;
    private static final int LEAF_DARK = 0xFF14432B;
    private static final int WATER_TOP = 0xFF4CB8F5;
    private static final int WATER_MID = 0xFF2D86D6;
    private static final int WATER_DARK = 0xFF1C56A8;
    private static final int ICE_TOP = 0xFFBDEBFF;
    private static final int SNOW_TOP = 0xFFF5F9FF;
    private static final int SNOW_MID = 0xFFC5D3EA;
    private static final int STONE_TOP = 0xFFA3A9B8;
    private static final int STONE_MID = 0xFF737A8E;
    private static final int STONE_DARK = 0xFF4A4F63;
    private static final int GRAVEL_TOP = 0xFFBFB2A2;
    private static final int SAND_TOP = 0xFFF5DA8C;
    private static final int SAND_MID = 0xFFDDAA5C;
    private static final int SAND_DARK = 0xFFA8763C;
    private static final int DIRT_TOP = 0xFFB97C49;
    private static final int DIRT_MID = 0xFF8C5733;
    private static final int DIRT_DARK = 0xFF5C3620;
    private static final int MUD_TOP = 0xFF6E4A35;
    private static final int MUD_DARK = 0xFF3A2619;
    private static final int SPROUT_TOP = 0xFFB2E055;
    private static final int RIPE_TOP = 0xFFF7CE4A;
    private static final int FENCE_TOP = 0xFFE2B26E;
    private static final int PATH_TOP = 0xFFD9BC80;
    private static final int FLOWER_TOP = 0xFFFF77B0;
    private static final int LAVA_TOP = 0xFFFF9A2E;
    private static final int LAVA_MID = 0xFFE5501E;
    private static final int LAVA_DARK = 0xFFA32E16;
    private static final int SOULSUNDER_TOP = 0xFFF8ACC4;
    private static final int SOULSUNDER_MID = 0xFFDC82A6;
    private static final int SOULSUNDER_DARK = 0xFFA9567E;
    private static final int GUTS_TOP = 0xFFE85A70;
    private static final int GUTS_MID = 0xFFB63D55;
    private static final int GUTS_DARK = 0xFF7C263D;
    private static final int[][] RAMPS = new int[MapTerrain.values().length][];

    static {
        for (MapTerrain terrain : MapTerrain.values()) RAMPS[terrain.ordinal()] = ramp(terrain);
    }

    private MapPalette() {}

    private static int[] ramp(MapTerrain terrain) {
        return switch (terrain) {
            case GRASS -> new int[] { GRASS_TOP, GRASS_MID, GRASS_DARK };
            case FOLIAGE -> new int[] { LEAF_TOP, GRASS_DARK, LEAF_DARK };
            case WATER -> new int[] { WATER_TOP, WATER_MID, WATER_DARK };
            case ICE -> new int[] { ICE_TOP, WATER_TOP, WATER_MID };
            case SNOW -> new int[] { SNOW_TOP, SNOW_MID, STONE_MID };
            case SAND -> new int[] { SAND_TOP, SAND_MID, SAND_DARK };
            case GRAVEL -> new int[] { GRAVEL_TOP, STONE_MID, STONE_DARK };
            case DIRT, WOOD -> new int[] { DIRT_TOP, DIRT_MID, DIRT_DARK };
            case MUD -> new int[] { MUD_TOP, DIRT_DARK, MUD_DARK };
            case FARMLAND -> new int[] { DIRT_MID, DIRT_DARK, MUD_DARK };
            case CROP_GROWING -> new int[] { SPROUT_TOP, GRASS_MID, GRASS_DARK };
            case CROP_MATURE -> new int[] { RIPE_TOP, SAND_MID, SAND_DARK };
            case FENCE -> new int[] { FENCE_TOP, DIRT_TOP, DIRT_MID };
            case PATH -> new int[] { PATH_TOP, DIRT_TOP, DIRT_MID };
            case FLOWER -> new int[] { FLOWER_TOP, GRASS_MID, GRASS_DARK };
            case LAVA -> new int[] { LAVA_TOP, LAVA_MID, LAVA_DARK };
            case SOULSUNDER_STONE -> new int[] { SOULSUNDER_TOP, SOULSUNDER_MID, SOULSUNDER_DARK };
            case GUTS_STONE -> new int[] { GUTS_TOP, GUTS_MID, GUTS_DARK };
            default -> new int[] { STONE_TOP, STONE_MID, STONE_DARK };
        };
    }

    public static int top(MapTerrain terrain) { return RAMPS[terrain.ordinal()][0]; }

    public static int left(MapTerrain terrain) { return RAMPS[terrain.ordinal()][1]; }

    public static int right(MapTerrain terrain) { return RAMPS[terrain.ordinal()][2]; }

    public static int colour(MapTerrain terrain, MapProjection.Face face) {
        return RAMPS[terrain.ordinal()][face.ordinal()];
    }

    public static int darken(int argb, float factor) {
        int red = (int) ((argb >> 16 & 0xFF) * factor);
        int green = (int) ((argb >> 8 & 0xFF) * factor);
        int blue = (int) ((argb & 0xFF) * factor);
        return argb & 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
