package net.alex.guzhenren.client.screen.blissful.map;

import net.alex.guzhenren.gameplay.blissful.map.MapTerrain;

/**
 * 福地地图调色板（色阶像素，Alex 定）：每种 {@link MapTerrain} 一条三阶色带（顶面 / 左崖面 / 右崖面，光从左上来），亮面
 * 偏暖、暗面偏冷，顶面平均饱和约 28%；另有四阶水色、波光、崖面分层色、迷雾三色与底色，只有地图光栅与图例用，不进
 * {@code ModPalette}。
 * <ul>
 *   <li>顶面颗粒：约 1/4 的列（按坐标散列，平移不闪）压暗到 {@value #GRAIN}；崖顶亮边：顶色往暖白混
 *       {@value #RIM_MIX}，高差 ≥ {@value #RIM_DEPTH} 格才描。</li>
 *   <li>水：顶面按离岸 1–{@value #SHORE_STEPS} 格由浅到深（{@link #water}）；离岸满 {@value #SHORE_STEPS} 格的深水约
 *       1/{@value #GLINT_EVERY} 的列点一道波光。服务端不发水深，离岸距离是近似。</li>
 *   <li>崖面分层：草土类顶下 1 格、沙与树冠顶下 2 格用本色带，再往下换土层或树冠暗影（{@link #cap}、{@link #strata}）；
 *       石、木、栅栏、水与秘境石不分层。</li>
 *   <li>荡魂山石保留主题粉一系但降饱和，胆石用灰肉红一系；{@link MapTerrain#UNKNOWN} 与 {@link MapTerrain#OTHER}
 *       同用石色带，{@link MapTerrain#VOID} 不画（透明）。</li>
 *   <li>昼夜色调不烘进这里，贴图时由 {@link MapTint} 统一相乘。</li>
 * </ul>
 */

public final class MapPalette {

    public static final int FOG = 0xFF2A2735;
    public static final int FOG_ALT = 0xFF2F2B3B;
    public static final int FOG_SIDE = 0xFF201E29;
    public static final int BACKDROP = 0xFF17161F;
    public static final int GLINT = 0xFFC9DDD8;
    public static final int SHORE_STEPS = 4;
    public static final int RIM_DEPTH = 2;
    public static final int NO_CAP = Integer.MAX_VALUE;
    static final float GRAIN = 0.93F;
    static final float RIM_MIX = 0.32F;
    static final int GLINT_EVERY = 17;
    private static final int RIM_LIGHT = 0xFFFFF2D0;
    private static final int[] WATER = { 0xFF7FAAB4, 0xFF6E9CAE, 0xFF5F8CA6, 0xFF557E99 };
    private static final int GRASS_TOP = 0xFF9DB06A;
    private static final int GRASS_LEFT = 0xFF6E8A52;
    private static final int GRASS_RIGHT = 0xFF4B6649;
    private static final int STONE_TOP = 0xFFB2ADA2;
    private static final int STONE_LEFT = 0xFF888785;
    private static final int STONE_RIGHT = 0xFF62636D;
    private static final int EARTH_LEFT = 0xFF7E6552;
    private static final int EARTH_RIGHT = 0xFF5E4A42;
    private static final int STRATA_STONE_LEFT = 0xFF85848A;
    private static final int STRATA_STONE_RIGHT = 0xFF5F6070;
    private static final int SHADE_LEFT = 0xFF2E4643;
    private static final int SHADE_RIGHT = 0xFF23353A;
    private static final int[][] RAMPS = new int[MapTerrain.values().length][];
    private static final int[] GRAINS = new int[MapTerrain.values().length];
    private static final int[] RIMS = new int[MapTerrain.values().length];

    static {
        for (MapTerrain terrain : MapTerrain.values()) {
            int[] ramp = ramp(terrain);
            RAMPS[terrain.ordinal()] = ramp;
            GRAINS[terrain.ordinal()] = darken(ramp[0], GRAIN);
            RIMS[terrain.ordinal()] = mix(ramp[0], RIM_LIGHT, RIM_MIX);
        }
    }

    private MapPalette() {}

    //region Ramps [色带]
    private static int[] ramp(MapTerrain terrain) {
        return switch (terrain) {
            case GRASS -> new int[] { GRASS_TOP, GRASS_LEFT, GRASS_RIGHT };
            case FOLIAGE -> new int[] { 0xFF5E8250, 0xFF42634A, 0xFF2E4643 };
            case WATER -> new int[] { 0xFF6E9CAE, 0xFF557E99, 0xFF415E7E };
            case ICE -> new int[] { 0xFFB4D0CF, 0xFF8FB2BC, 0xFF6E90A6 };
            case SNOW -> new int[] { 0xFFECEEE6, 0xFFC3C8CF, 0xFF9198AA };
            case SAND -> new int[] { 0xFFE0D0A2, 0xFFBFA87C, 0xFF8F785E };
            case GRAVEL -> new int[] { 0xFFABA59A, 0xFF86847F, 0xFF61626C };
            case DIRT -> new int[] { 0xFF957C5E, 0xFF725B4C, 0xFF4E3E3E };
            case WOOD -> new int[] { 0xFF8D6E56, 0xFF6A5248, 0xFF4A3A3C };
            case MUD -> new int[] { 0xFF64514A, 0xFF4A3D3D, 0xFF332B32 };
            case FARMLAND -> new int[] { 0xFF7A6150, 0xFF5A4842, 0xFF3D3236 };
            case CROP_GROWING -> new int[] { 0xFFAFBF72, GRASS_LEFT, GRASS_RIGHT };
            case CROP_MATURE -> new int[] { 0xFFD6BF7A, 0xFFAC9160, 0xFF7C6650 };
            case FENCE -> new int[] { 0xFFAE9170, 0xFF836A55, 0xFF5C4A44 };
            case PATH -> new int[] { 0xFFC9B488, 0xFFA08A68, 0xFF736250 };
            case FLOWER -> new int[] { 0xFFCFA0AE, GRASS_LEFT, GRASS_RIGHT };
            case LAVA -> new int[] { 0xFFE09656, 0xFFBB6446, 0xFF7E4040 };
            case SOULSUNDER_STONE -> new int[] { 0xFFD4A8AF, 0xFFAA8192, 0xFF7A5E74 };
            case GUTS_STONE -> new int[] { 0xFFB87A72, 0xFF8E5A5C, 0xFF5E3E4A };
            default -> new int[] { STONE_TOP, STONE_LEFT, STONE_RIGHT };
        };
    }

    public static int top(MapTerrain terrain) { return RAMPS[terrain.ordinal()][0]; }

    /** 带颗粒的顶面色：约 1/4 的列压暗。 */
    public static int top(MapTerrain terrain, int x, int z) {
        return (hash(x, z) & 3) == 0 ? GRAINS[terrain.ordinal()] : top(terrain);
    }

    public static int left(MapTerrain terrain) { return RAMPS[terrain.ordinal()][1]; }

    public static int right(MapTerrain terrain) { return RAMPS[terrain.ordinal()][2]; }

    public static int colour(MapTerrain terrain, MapProjection.Face face) {
        return RAMPS[terrain.ordinal()][face.ordinal()];
    }

    public static int rim(MapTerrain terrain) { return RIMS[terrain.ordinal()]; }
    //endregion

    //region Water [水面]
    /** 离岸 {@code shore} 格（1 = 贴岸）的水面色，超出范围按两端取。 */
    public static int water(int shore) { return WATER[Math.clamp(shore, 1, SHORE_STEPS) - 1]; }

    public static boolean glint(int x, int z) { return Math.floorMod(hash(z, x) >>> 4, GLINT_EVERY) == 0; }
    //endregion

    //region Strata [崖面分层]
    /** 崖面顶下用本色带的格数，{@link #NO_CAP} 为整面本色。 */
    public static int cap(MapTerrain terrain) {
        return switch (terrain) {
            case GRASS, FLOWER, CROP_GROWING, CROP_MATURE, FARMLAND, PATH, DIRT, MUD, SNOW -> 1;
            case SAND, FOLIAGE -> 2;
            default -> NO_CAP;
        };
    }

    /** {@link #cap} 以下的崖面色；不分层的地貌返回 0。 */
    public static int strata(MapTerrain terrain, MapProjection.Face face) {
        boolean left = face == MapProjection.Face.LEFT;
        return switch (terrain) {
            case GRASS, FLOWER, CROP_GROWING, CROP_MATURE, FARMLAND, PATH, DIRT, MUD, SAND ->
                    left ? EARTH_LEFT : EARTH_RIGHT;
            case SNOW -> left ? STRATA_STONE_LEFT : STRATA_STONE_RIGHT;
            case FOLIAGE -> left ? SHADE_LEFT : SHADE_RIGHT;
            default -> 0;
        };
    }
    //endregion

    //region Colour math [颜色运算]
    public static int darken(int argb, float factor) {
        int red = (int) ((argb >> 16 & 0xFF) * factor);
        int green = (int) ((argb >> 8 & 0xFF) * factor);
        int blue = (int) ((argb & 0xFF) * factor);
        return argb & 0xFF000000 | red << 16 | green << 8 | blue;
    }

    public static int mix(int argb, int other, float t) {
        int red = Math.round((argb >> 16 & 0xFF) + ((other >> 16 & 0xFF) - (argb >> 16 & 0xFF)) * t);
        int green = Math.round((argb >> 8 & 0xFF) + ((other >> 8 & 0xFF) - (argb >> 8 & 0xFF)) * t);
        int blue = Math.round((argb & 0xFF) + ((other & 0xFF) - (argb & 0xFF)) * t);
        return argb & 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int hash(int x, int z) {
        int hash = x * 0x27D4EB2D ^ z * 0x165667B1;
        hash ^= hash >>> 15;
        hash *= 0x2C1B3C6D;
        return hash ^ hash >>> 13;
    }
    //endregion
}
