package net.alex.guzhenren.client;

/**
 * 客户端唯一调色板：界面色 token（暗炭金，Alex 定）、数据色以及多界面与 HUD 共用的文字色；仅有两个及以上界面或 HUD 共
 * 用的颜色才放入本类，单界面专属配色留在各自本地常量中；本类仅限客户端使用，通用代码（如物品蓄力条颜色）不得引用。
 * <ul>
 *   <li>界面底色按明度分档：{@link #SURFACE_0} 最暗（凹槽、行底），{@link #SURFACE_2} 标题带、分隔线与行悬停，
 *       {@link #SURFACE_3} 选中行；金色 {@link #ACCENT} 只表状态（选中、焦点、开、当前对象名）。</li>
 *   <li>数据色（{@link #APERTURE}、{@link #SOUL}、{@link #PLANT} 等）只画数据条与读数，不画框，也不随数值好坏变
 *       色。</li>
 *   <li>文字三档：{@link #TEXT} 米白正文、{@link #LEGEND_TEXT} 标签与次要读数、{@link #TEXT_DEAD} 失效；
 *       {@link #SHORT} 朱砂只给不足与危险。</li>
 * </ul>
 */

public final class ModPalette {

    //region Surfaces [界面色]
    public static final int SURFACE_0 = 0xFF120E10;
    public static final int SURFACE_2 = 0xFF2A2123;
    public static final int SURFACE_3 = 0xFF3A2D2F;
    public static final int ACCENT = 0xFFE4B750;
    //endregion

    //region Data [数据色]
    public static final int APERTURE = 0xFF4FC3F7;
    public static final int SOUL = 0xFFD388FF;
    public static final int PLANT = 0xFF86C46E;
    public static final int DISTILLED_FILL = 0xFF1565C0;
    //endregion

    //region Text [文字]
    public static final int TEXT = 0xFFFFF7E8;
    public static final int LEGEND_TEXT = 0xFFBCAC9A;
    public static final int TEXT_DEAD = 0xFF76655F;
    public static final int SHORT = 0xFFE8736A;
    //endregion

    private ModPalette() {}
}
