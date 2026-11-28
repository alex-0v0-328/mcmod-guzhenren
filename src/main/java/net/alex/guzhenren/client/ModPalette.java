package net.alex.guzhenren.client;

/**
 * 客户端唯一调色板：七个信息页签的数据色以及多界面与 HUD 共用的颜色；仅有两个及以上界面或 HUD 共用的颜色才放入本类，单
 * 界面专属配色留在各自本地常量中；本类仅限客户端使用，通用代码（如物品蓄力条颜色）不得引用。
 * <ul>
 *   <li>数据色（{@link #APERTURE}、{@link #SOUL}、{@link #PLANT} 等）只画数据条与读数，不画框，也不随数值好坏变色。</li>
 * </ul>
 */

public final class ModPalette {

    //region Data [数据色]
    public static final int APERTURE = 0xFF4FC3F7;
    public static final int BODY = 0xFFFFAB91;
    public static final int SOUL = 0xFFD388FF;
    public static final int PATH = 0xFFFFD54F;
    public static final int MIND = 0xFF4DD0E1;
    public static final int REFINEMENT = 0xFF81C784;
    public static final int PLANT = 0xFF86C46E;
    public static final int TREASURE_YELLOW_HEAVEN = 0xFFF4D35E;
    public static final int DISTILLED_FILL = 0xFF1565C0;
    //endregion

    //region Text [文字]
    public static final int LEGEND_TEXT = 0xFFA0A0A0;
    public static final int SHORT = 0xFFFF5555;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DEAD = 0xFF6A6A6A;
    //endregion

    private ModPalette() {}
}
