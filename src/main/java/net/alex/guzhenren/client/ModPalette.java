package net.alex.guzhenren.client;

/**
 * 客户端唯一调色板：七个领域强调色，及多界面与 HUD 同名共用的框体色；两个及以上界面共用的色才放这里，单界面配色留本地常
 * 量；仅客户端，通用代码（物品蓄力色）不得引用。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModPalette {

    public static final int APERTURE = 0xFF4FC3F7;
    public static final int BODY = 0xFFFFAB91;
    public static final int SOUL = 0xFFD388FF;
    public static final int PATH = 0xFFFFD54F;
    public static final int MIND = 0xFF4DD0E1;
    public static final int REFINEMENT = 0xFF81C784;
    public static final int TREASURE_YELLOW_HEAVEN = 0xFFF4D35E;
    public static final int LEGEND_TEXT = 0xFFA0A0A0;
    public static final int SHORT = 0xFFFF5555;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DEAD = 0xFF6A6A6A;
    public static final int DISTILLED_FILL = 0xFF1565C0;

    private ModPalette() {}
}
