package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 地灵经营面板的共用绘制部件：暗炭底加金色的全屏套件（卡片、数据块、凹槽、列表行、按钮、开关、键帽、地图叠加底）、玩家头像
 * 与对齐文字。
 * <ul>
 *   <li>精灵图都在 {@code textures/gui/sprites/blissful_land/screen/}，九宫格的随 {@code .mcmeta}；物品格与进度条复用模组
 *       中性色的 {@code slot}、{@code bar_background}/{@code bar_fill}。</li>
 *   <li>层级（Alex 定）：金 = 状态（选中、焦点、开、当前对象名），悬停只变亮一档不用金；破坏性操作用 {@link Look#DANGER}；
 *       多选一的当前项用 {@link Look#SELECTED}；列表行内 14 高的只用 {@link Look#GHOST}，实体键至少 16 高。</li>
 *   <li>按钮分「画图」与「写字」两步，调用方先在一个 {@link SpriteBatch} 里画完全部精灵图再统一写字；字的 y 随样式与是否
 *       下沉（按下、选中、失效）而定，见 {@link #label}。按下 = 左键按住且光标在键上；画图时记下按下的那个键的矩形，同一帧
 *       写字按矩形认出它。</li>
 *   <li>卡片标题带 {@value #TITLE_H} 高：标题白字、右侧读数灰字（{@link #title}）；卡片里不放提示小字，说明走悬停提示。</li>
 *   <li>颜色：{@link #ACCENT} 与底色 {@link #BACKDROP} 只有本面板用，不进 {@link ModPalette}；其余文字色引用
 *       {@link ModPalette}。</li>
 * </ul>
 */

public final class PanelDrawing {

    private PanelDrawing() {}

    public static final int ACCENT = 0xFFE4B750;
    public static final int BACKDROP = 0xFF120E10;
    public static final int TEXT = ModPalette.TEXT;
    public static final int LEGEND = ModPalette.LEGEND_TEXT;
    public static final int DEAD = ModPalette.TEXT_DEAD;
    public static final int WARN = ModPalette.SHORT;
    public static final int OUTSIDER = 0xFFE8736A;
    public static final int SELECTED_TEXT = 0xFFFFF1C8;
    public static final int DIM_ICON = 0xFFB3B3B3;
    public static final ResourceLocation CARD = sprite("card");
    public static final ResourceLocation STAT = sprite("stat");
    public static final ResourceLocation STAT_HIGHLIGHTED = sprite("stat_highlighted");
    public static final ResourceLocation INSET = sprite("inset");
    public static final ResourceLocation POPUP = sprite("popup");
    public static final ResourceLocation OVERLAY = sprite("overlay");
    public static final ResourceLocation CHIP = sprite("chip");
    public static final ResourceLocation KEYCAP = sprite("keycap");
    public static final ResourceLocation ROW = sprite("row");
    public static final ResourceLocation ROW_HIGHLIGHTED = sprite("row_highlighted");
    public static final ResourceLocation TEXT_FIELD = sprite("text_field");
    public static final ResourceLocation TEXT_FIELD_FOCUSED = sprite("text_field_focused");
    public static final ResourceLocation SCROLLER = sprite("scroller");
    public static final ResourceLocation SCROLLER_HIGHLIGHTED = sprite("scroller_highlighted");
    public static final ResourceLocation SCROLLER_TRACK = sprite("scroller_track");
    public static final ResourceLocation SLOT = Guzhenren.id("slot");
    public static final ResourceLocation BAR_BACKGROUND = Guzhenren.id("bar_background");
    public static final ResourceLocation BAR_FILL = Guzhenren.id("bar_fill");
    public static final int TITLE_H = 15;
    public static final int MARGIN = 9;
    public static final int GAP = 10;
    public static final int PAD = 6;
    public static final int BUTTON_H = 16;
    public static final int GHOST_H = 14;
    public static final int ROW_H = 16;
    public static final int SWITCH_W = 18;
    public static final int SWITCH_H = 10;
    public static final int KEYCAP_H = 11;
    public static final int STACK_BELOW = 370;
    public static final int MAX_WIDTH = 622;
    private static final long NO_BUTTON = -1L;
    private static long pressedButton = NO_BUTTON;

    /** 按钮样式：普通实体键、危险键、幽灵键（细框）、多选一的当前项。 */
    public enum Look { PLAIN, DANGER, GHOST, SELECTED }

    /**
     * 两列卡片的位置：内容宽至多 {@value #MAX_WIDTH}、居中；窗口不足 {@value #STACK_BELOW} 宽时右卡排到左卡下面，两张都占满
     * 内容宽。
     */
    public record Columns(int leftX, int leftW, int rightX, int rightY, int rightW, boolean stacked) {

        public static Columns of(int screenWidth, int top, int leftW, int leftH) {
            int width = contentWidth(screenWidth);
            int x = contentX(screenWidth);
            if (screenWidth < STACK_BELOW) return new Columns(x, width, x, top + leftH + GAP, width, true);
            return new Columns(x, leftW, x + leftW + GAP, top, width - leftW - GAP, false);
        }
    }

    public static int contentWidth(int screenWidth) { return Math.min(screenWidth - 2 * MARGIN, MAX_WIDTH); }

    public static int contentX(int screenWidth) { return (screenWidth - contentWidth(screenWidth)) / 2; }

    public static ResourceLocation sprite(String name) { return Guzhenren.id("blissful_land/screen/" + name); }

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
        sprites.blit(sprite(name + state), x, y, width, height);
    }

    public static boolean pressed(int x, int y, int width, int height) {
        return pressedButton == rect(x, y, width, height);
    }

    private static long rect(int x, int y, int width, int height) {
        return ((long) (x & 0xFFFF) << 48) | ((long) (y & 0xFFFF) << 32) | ((long) (width & 0xFFFF) << 16)
                | (height & 0xFFFF);
    }

    public static void button(SpriteBatch sprites, int x, int y, int width, int height, Look look, boolean live,
                              double mouseX, double mouseY) {
        button(sprites, x, y, width, height, look, live, live && Widgets.isOver(mouseX, mouseY, x, y, width, height));
    }

    public static void button(SpriteBatch sprites, int x, int y, int width, int height, boolean live, double mouseX,
                              double mouseY) {
        button(sprites, x, y, width, height, Look.PLAIN, live, mouseX, mouseY);
    }

    public static void label(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int height,
                             Look look, boolean live) {
        boolean pressed = pressed(x, y, width, height);
        boolean sunk = !live || look == Look.SELECTED || pressed;
        int top = look == Look.GHOST ? y + (height - 8) / 2 + (pressed ? 1 : 0)
                : y + 1 + (height - 12) / 2 + (sunk ? 1 : 0);
        int colour = !live ? DEAD : look == Look.SELECTED ? SELECTED_TEXT : TEXT;
        centered(graphics, font, text, x, x + width, top, colour);
    }

    public static void label(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int height,
                             boolean live) {
        label(graphics, font, text, x, y, width, height, Look.PLAIN, live);
    }

    public static void toggle(SpriteBatch sprites, int x, int y, boolean on, boolean live, double mouseX,
                              double mouseY) {
        boolean hover = live && Widgets.isOver(mouseX, mouseY, x, y, SWITCH_W, SWITCH_H);
        String state = !live ? "_disabled" : hover ? "_highlighted" : "";
        sprites.blit(sprite((on ? "switch_on" : "switch_off") + state), x, y, SWITCH_W, SWITCH_H);
    }
    //endregion

    //region Cards and rows [卡片与行]
    public static void card(SpriteBatch sprites, int x, int y, int width, int height) {
        sprites.blit(CARD, x, y, width, height);
    }

    public static void title(GuiGraphics graphics, Font font, Component title, Component readout, int x, int y,
                             int width) {
        graphics.drawString(font, title, x + PAD, y + 4, TEXT, false);
        right(graphics, font, readout, x + width - PAD, y + 4, LEGEND);
    }

    public static void stat(SpriteBatch sprites, ResourceLocation icon, int x, int y, int width, int height,
                            boolean hover) {
        sprites.blit(hover ? STAT_HIGHLIGHTED : STAT, x, y, width, height);
        sprites.blit(icon, x + 5, y + (height - 16) / 2, 16, 16);
    }

    public static void statText(GuiGraphics graphics, Font font, Component label, String value, int x, int y,
                                int height) {
        int top = y + (height - 20) / 2;
        graphics.drawString(font, label, x + 26, top + 1, LEGEND, false);
        graphics.drawString(font, value, x + 26, top + 11, TEXT, false);
    }

    public static void row(SpriteBatch sprites, int x, int y, int width, int height, boolean hover) {
        sprites.blit(hover ? ROW_HIGHLIGHTED : ROW, x, y, width, height);
    }

    public static int keycapWidth(Font font, Component text) { return Math.max(12, font.width(text) + 5); }

    public static void keycap(SpriteBatch sprites, int x, int y, int width) {
        sprites.blit(KEYCAP, x, y, width, KEYCAP_H);
    }

    public static void keycapText(GuiGraphics graphics, Font font, Component text, int x, int y, int width) {
        centered(graphics, font, text, x, x + width, y + 1, TEXT);
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

    public static void head(GuiGraphics graphics, UUID player, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerInfo info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(player);
        PlayerFaceRenderer.draw(graphics, info != null ? info.getSkin() : DefaultPlayerSkin.get(player), x, y, 8);
    }

    public static String count(long value) {
        if (value < 10_000L) return Long.toString(value);
        if (value < 1_000_000L) return value / 1_000L + "k";
        return value / 1_000_000L + "m";
    }
    //endregion

    //region Scrolling [滚动]
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
}
