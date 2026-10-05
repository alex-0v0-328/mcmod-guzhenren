package net.alex.guzhenren.client;

/**
 * The one palette of the client surfaces: the seven domain accent colors and the chrome that several
 * screens and HUDs mean by the same name. A color belongs here only when two or more surfaces share
 * it; a single surface's own look stays a local constant.
 *
 * <p>Client only -- common code (the item charge color) must not reference this class.
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
    public static final int PANEL_FILL = 0xBF000000;
    public static final int BORDER = 0x66FFFFFF;
    public static final int SLOT_FILL = 0x33FFFFFF;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int BUTTON_IDLE = 0x33FFFFFF;
    public static final int BUTTON_HOVER = 0x66FFFFFF;
    public static final int BUTTON_DEAD = 0x14FFFFFF;
    public static final int BAR_TRACK = 0xB0202020;
    public static final int BAR_BORDER = 0xC0000000;
    public static final int DISTILLED_FILL = 0xFF1565C0;

    private ModPalette() {}
}
