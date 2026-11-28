package net.alex.guzhenren.client.screen;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 全模组界面唯一的绘制部件套件（暗炭金，Alex 定）：面板、卡片、弹窗、凹槽、物品格、进度条、四种按钮、开关、键帽、列表行、数据
 * 块、滚动条与对齐文字，外加命中检测、容器按钮发包与回 B 面板入口。
 * <ul>
 *   <li>精灵图都在 {@code textures/gui/sprites/ui/}（{@link #ui}），一概念一张图，九宫格的随 {@code .mcmeta}；地灵面板专属
 *       图标另在 {@code blissful_land/screen/}，布局常量在 {@code PanelDrawing}。</li>
 *   <li>层级：金只表状态（选中、焦点、开、当前对象名），悬停只亮一档；破坏性操作用 {@link Look#DANGER}；多选一的当前项用
 *       {@link Look#SELECTED}；列表行内 14 高的只用 {@link Look#GHOST}，实体键至少 16 高。</li>
 *   <li>按钮分「画图」与「写字」两步，调用方先在一个 {@link SpriteBatch} 里画完全部精灵图再统一写字；字的 y 随样式与是否
 *       下沉（按下、选中、失效）而定，见 {@link #label}。按下 = 左键按住且光标在键上；画图时记下按下的那个键的矩形，同一帧
 *       写字按矩形认出它。</li>
 *   <li>物品格精灵图四周比所框 16 像素槽位各宽 1 像素；面板边框宽 {@link #FRAME}，内容须避让；弹窗精灵图右下带 2 像素阴
 *       影，尺寸比逻辑弹窗多出阴影宽；弹窗打开先压暗整屏（{@link #drawScrim}）。卡片标题带 {@value #TITLE_H} 高：白字标
 *       题、灰字读数（{@link #title}）。</li>
 * </ul>
 */

public final class Widgets {

    private Widgets() {}

    public static final int CELL = 16;
    public static final int FRAME = 3;
    public static final int TITLE_H = 15;
    public static final int PAD = 6;
    public static final int BUTTON_H = 16;
    public static final int GHOST_H = 14;
    public static final int ROW_H = 16;
    public static final int SWITCH_W = 18;
    public static final int SWITCH_H = 10;
    public static final int KEYCAP_H = 11;
    public static final ResourceLocation PANEL = ui("panel");
    public static final ResourceLocation CARD = ui("card");
    public static final ResourceLocation POPUP = ui("popup");
    public static final ResourceLocation INSET = ui("inset");
    public static final ResourceLocation OVERLAY = ui("overlay");
    public static final ResourceLocation CHIP = ui("chip");
    public static final ResourceLocation KEYCAP = ui("keycap");
    public static final ResourceLocation STAT = ui("stat");
    public static final ResourceLocation STAT_HIGHLIGHTED = ui("stat_highlighted");
    public static final ResourceLocation ROW = ui("row");
    public static final ResourceLocation ROW_HIGHLIGHTED = ui("row_highlighted");
    public static final ResourceLocation TEXT_FIELD = ui("text_field");
    public static final ResourceLocation TEXT_FIELD_FOCUSED = ui("text_field_focused");
    public static final ResourceLocation SCROLLER = ui("scroller");
    public static final ResourceLocation SCROLLER_HIGHLIGHTED = ui("scroller_highlighted");
    public static final ResourceLocation SCROLLER_TRACK = ui("scroller_track");
    public static final ResourceLocation SLOT = ui("slot");
    public static final ResourceLocation SLOT_CORE = ui("slot_core");
    public static final ResourceLocation BAR_BACKGROUND = ui("bar_background");
    public static final ResourceLocation BAR_FILL = ui("bar_fill");
    private static final int POPUP_SHADOW = 2;
    private static final int SCRIM = 0x8C080607;
    private static final int BACK_W = 16;
    private static final int BACK_H = 14;
    private static final Component BACK = Component.literal("<-");
    private static final long NO_BUTTON = -1L;
    private static long pressedButton = NO_BUTTON;

    /** 按钮样式：普通实体键、危险键、幽灵键（细框）、多选一的当前项。 */
    public enum Look { PLAIN, DANGER, GHOST, SELECTED }

    public static ResourceLocation ui(String name) { return Guzhenren.id("ui/" + name); }

    //region Frames and cells [框与格]
    public static void drawPanel(SpriteBatch sprites, int x, int y, int width, int height) {
        sprites.blit(PANEL, x, y, width, height);
    }

    public static void card(SpriteBatch sprites, int x, int y, int width, int height) {
        sprites.blit(CARD, x, y, width, height);
    }

    public static void drawPopup(SpriteBatch sprites, int x, int y, int width, int height) {
        sprites.blit(POPUP, x, y, width + POPUP_SHADOW, height + POPUP_SHADOW);
    }

    public static void drawScrim(GuiGraphics graphics) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), SCRIM);
    }

    public static void drawRule(GuiGraphics graphics, int left, int y, int right, int color) {
        graphics.fill(left, y, right, y + 1, color);
    }

    public static void drawCells(SpriteBatch sprites, int x, int y, int cols, int rows, int pitch) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) drawCell(sprites, SLOT, x + col * pitch, y + row * pitch);
        }
    }

    public static void drawCell(SpriteBatch sprites, int x, int y) { drawCell(sprites, SLOT, x, y); }

    public static void drawCell(SpriteBatch sprites, ResourceLocation sprite, int x, int y) {
        sprites.blit(sprite, x - 1, y - 1, CELL + 2, CELL + 2);
    }

    /** {@code filled} 是填充覆盖的内宽，取 0 至 {@code width - 2}；填充内缩 1 像素。 */
    public static void drawBar(SpriteBatch sprites, int x, int y, int width, int height, int filled, int colour) {
        sprites.blit(BAR_BACKGROUND, x, y, width, height);
        sprites.blit(BAR_FILL, x + 1, y + 1, Math.min(filled, width - 2), height - 2, colour);
    }
    //endregion

    //region Buttons [按钮]
    public static void button(SpriteBatch sprites, int x, int y, int width, int height, Look look, boolean live,
                              boolean hover) {
        boolean pressed = live && hover && look != Look.SELECTED
                && Minecraft.getInstance().mouseHandler.isLeftPressed();
        long key = rect(x, y, width, height);
        if (pressed) pressedButton = key;
        else if (pressedButton == key) pressedButton = NO_BUTTON;
        String name = switch (look) {
            case PLAIN -> live ? "button" : "button_disabled";
            case DANGER -> live ? "button_danger" : "button_disabled";
            case GHOST -> "button_ghost";
            case SELECTED -> live ? "button_selected" : "button_disabled";
        };
        String state = pressed ? "_pressed" : live && hover ? "_highlighted" : "";
        sprites.blit(ui(name + state), x, y, width, height);
    }

    public static void button(SpriteBatch sprites, int x, int y, int width, int height, Look look, boolean live,
                              double mouseX, double mouseY) {
        button(sprites, x, y, width, height, look, live, live && isOver(mouseX, mouseY, x, y, width, height));
    }

    public static void button(SpriteBatch sprites, int x, int y, int width, int height, boolean live, double mouseX,
                              double mouseY) {
        button(sprites, x, y, width, height, Look.PLAIN, live, mouseX, mouseY);
    }

    public static void drawButton(SpriteBatch sprites, int x, int y, int width, int height, boolean live,
                                  boolean hover) {
        button(sprites, x, y, width, height, Look.PLAIN, live, hover);
    }

    public static boolean pressed(int x, int y, int width, int height) {
        return pressedButton == rect(x, y, width, height);
    }

    private static long rect(int x, int y, int width, int height) {
        return ((long) (x & 0xFFFF) << 48) | ((long) (y & 0xFFFF) << 32) | ((long) (width & 0xFFFF) << 16)
                | (height & 0xFFFF);
    }

    public static void label(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int height,
                             Look look, boolean live) {
        boolean pressed = pressed(x, y, width, height);
        boolean sunk = !live || look == Look.SELECTED || pressed;
        int top = look == Look.GHOST ? y + (height - 8) / 2 + (pressed ? 1 : 0)
                : y + 1 + (height - 12) / 2 + (sunk ? 1 : 0);
        int colour = !live ? ModPalette.TEXT_DEAD : look == Look.SELECTED ? ModPalette.ACCENT_TEXT : ModPalette.TEXT;
        centered(graphics, font, text, x, x + width, top, colour);
    }

    public static void label(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int height,
                             boolean live) {
        label(graphics, font, text, x, y, width, height, Look.PLAIN, live);
    }

    public static void drawButtonLabel(GuiGraphics graphics, Font font, Component label, int x, int y, int width,
                                       int height, boolean live) {
        label(graphics, font, label, x, y, width, height, live);
    }

    public static void drawBack(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        drawButton(sprites, x, y, BACK_W, BACK_H, true, isOverBack(mouseX, mouseY, x, y));
    }

    public static void drawBackLabel(GuiGraphics graphics, Font font, int x, int y) {
        drawButtonLabel(graphics, font, BACK, x, y, BACK_W, BACK_H, true);
    }

    public static boolean isOverBack(double mouseX, double mouseY, int x, int y) {
        return isOver(mouseX, mouseY, x, y, BACK_W, BACK_H);
    }

    public static void toggle(SpriteBatch sprites, int x, int y, boolean on, boolean live, double mouseX,
                              double mouseY) {
        boolean hover = live && isOver(mouseX, mouseY, x, y, SWITCH_W, SWITCH_H);
        String state = !live ? "_disabled" : hover ? "_highlighted" : "";
        sprites.blit(ui((on ? "switch_on" : "switch_off") + state), x, y, SWITCH_W, SWITCH_H);
    }
    //endregion

    //region Rows and blocks [行与数据块]
    public static void title(GuiGraphics graphics, Font font, Component title, Component readout, int x, int y,
                             int width) {
        graphics.drawString(font, title, x + PAD, y + 4, ModPalette.TEXT, false);
        right(graphics, font, readout, x + width - PAD, y + 4, ModPalette.LEGEND_TEXT);
    }

    public static void stat(SpriteBatch sprites, ResourceLocation icon, int x, int y, int width, int height,
                            boolean hover) {
        sprites.blit(hover ? STAT_HIGHLIGHTED : STAT, x, y, width, height);
        sprites.blit(icon, x + 5, y + (height - 16) / 2, 16, 16);
    }

    public static void statText(GuiGraphics graphics, Font font, Component label, String value, int x, int y,
                                int height) {
        int top = y + (height - 20) / 2;
        graphics.drawString(font, label, x + 26, top + 1, ModPalette.LEGEND_TEXT, false);
        graphics.drawString(font, value, x + 26, top + 11, ModPalette.TEXT, false);
    }

    public static void row(SpriteBatch sprites, int x, int y, int width, int height, boolean hover) {
        sprites.blit(hover ? ROW_HIGHLIGHTED : ROW, x, y, width, height);
    }

    public static int keycapWidth(Font font, Component text) { return Math.max(12, font.width(text) + 5); }

    public static void keycap(SpriteBatch sprites, int x, int y, int width) {
        sprites.blit(KEYCAP, x, y, width, KEYCAP_H);
    }

    public static void keycapText(GuiGraphics graphics, Font font, Component text, int x, int y, int width) {
        centered(graphics, font, text, x, x + width, y + 1, ModPalette.TEXT);
    }

    public static void scrollbar(SpriteBatch sprites, int x, int y, int height, int offset, int visible, int total,
                                 boolean hover) {
        sprites.blit(SCROLLER_TRACK, x, y, 6, height);
        if (total <= visible) return;
        int thumb = Math.max(8, height * visible / total);
        int top = y + (height - thumb) * offset / (total - visible);
        sprites.blit(hover ? SCROLLER_HIGHLIGHTED : SCROLLER, x, top, 6, thumb);
    }

    public static int clampScroll(int offset, int visible, int total) {
        return Math.max(0, Math.min(offset, total - visible));
    }
    //endregion

    //region Text [文字]
    public static void right(GuiGraphics graphics, Font font, Component text, int rightX, int y, int colour) {
        graphics.drawString(font, text, rightX - font.width(text), y, colour, false);
    }

    public static void right(GuiGraphics graphics, Font font, String text, int rightX, int y, int colour) {
        graphics.drawString(font, text, rightX - font.width(text), y, colour, false);
    }

    public static void centered(GuiGraphics graphics, Font font, Component text, int left, int right, int y,
                                int colour) {
        graphics.drawString(font, text, left + (right - left - font.width(text)) / 2, y, colour, false);
    }

    public static void drawCentered(GuiGraphics graphics, Font font, Component label, int x, int y, int width,
                                    int height, int colour) {
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + (height - font.lineHeight) / 2 + 1,
                colour, false);
    }
    //endregion

    //region Input [输入]
    public static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    static boolean openInfo(Screen from) { return openInfo(from, false); }

    /** {@code escapeHeld}：由 Esc 带到这里，面板忽略该键直到松开。 */
    static boolean openInfo(Screen from, boolean escapeHeld) {
        from.onClose();
        Minecraft.getInstance().setScreen(new PlayerInfoScreen(escapeHeld));
        return true;
    }

    static boolean clickMenuButton(int containerId, int button) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null) return false;

        minecraft.gameMode.handleInventoryButtonClick(containerId, button);
        return true;
    }
    //endregion
}
