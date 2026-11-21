package net.alex.guzhenren.client.screen.blissful;

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
 * 地灵经营面板的共用绘制部件：粉色面板套件的精灵图、按钮与开关的状态选图、列表行、玩家头像与右对齐文字。
 * <ul>
 *   <li>精灵图都在 {@code textures/gui/sprites/blissful_land/}，九宫格的随 {@code .mcmeta}；物品格、滚动条轨道、进度条
 *       复用模组中性色的 {@code slot}、{@code scroller_background}、{@code bar_background}/{@code bar_fill}。</li>
 *   <li>按钮分「画图」与「写字」两步，调用方先在一个 {@link SpriteBatch} 里画完全部精灵图再统一写字；写字与命中检测复用
 *       {@link Widgets}。</li>
 *   <li>颜色：{@link #ACCENT} 为主题粉（= 荡魂山石），只有本面板用，不进 {@link ModPalette}；其余文字色引用
 *       {@link ModPalette}。</li>
 * </ul>
 */

final class PanelKit {

    private PanelKit() {}

    static final int ACCENT = 0xFFF8ACC4;
    static final int TEXT = ModPalette.TEXT;
    static final int LEGEND = ModPalette.LEGEND_TEXT;
    static final int DEAD = ModPalette.TEXT_DEAD;
    static final int WARN = ModPalette.SHORT;
    static final ResourceLocation PANEL = sprite("panel");
    static final ResourceLocation POPUP = sprite("popup");
    static final ResourceLocation WELL = sprite("well");
    static final ResourceLocation ROW = sprite("row");
    static final ResourceLocation ROW_HIGHLIGHTED = sprite("row_highlighted");
    static final ResourceLocation TEXT_FIELD = sprite("text_field");
    static final ResourceLocation TEXT_FIELD_FOCUSED = sprite("text_field_focused");
    static final ResourceLocation SCROLLER = sprite("scroller");
    static final ResourceLocation SCROLLER_HIGHLIGHTED = sprite("scroller_highlighted");
    static final ResourceLocation SCROLLER_TRACK = Guzhenren.id("scroller_background");
    static final ResourceLocation SLOT = Guzhenren.id("slot");
    static final ResourceLocation BAR_BACKGROUND = Guzhenren.id("bar_background");
    static final ResourceLocation BAR_FILL = Guzhenren.id("bar_fill");
    static final int SWITCH_W = 18;
    static final int SWITCH_H = 10;
    static final int SQUARE = 14;

    static ResourceLocation sprite(String name) { return Guzhenren.id("blissful_land/" + name); }

    //region Buttons [按钮]
    static void button(SpriteBatch sprites, int x, int y, int width, int height, boolean live, boolean hover,
                       boolean pressed, boolean danger) {
        String name = !live ? "button_disabled" : (danger ? "button_danger" : "button")
                + (pressed ? "_pressed" : hover ? "_highlighted" : "");
        sprites.blit(sprite(name), x, y, width, height);
    }

    static void button(SpriteBatch sprites, int x, int y, int width, int height, boolean live, double mouseX,
                       double mouseY) {
        boolean hover = live && Widgets.isOver(mouseX, mouseY, x, y, width, height);
        button(sprites, x, y, width, height, live, hover, false, false);
    }

    static void label(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int height,
                      boolean live) {
        Widgets.drawButtonLabel(graphics, font, text, x, y, width, height, live);
    }

    static void toggle(SpriteBatch sprites, int x, int y, boolean on, boolean live, double mouseX, double mouseY) {
        boolean hover = live && Widgets.isOver(mouseX, mouseY, x, y, SWITCH_W, SWITCH_H);
        String state = !live ? "_disabled" : hover ? "_highlighted" : "";
        sprites.blit(sprite((on ? "switch_on" : "switch_off") + state), x, y, SWITCH_W, SWITCH_H);
    }
    //endregion

    //region Rows and text [行与文字]
    static void row(SpriteBatch sprites, int x, int y, int width, int height, boolean hover) {
        sprites.blit(hover ? ROW_HIGHLIGHTED : ROW, x, y, width, height);
    }

    static void right(GuiGraphics graphics, Font font, Component text, int rightX, int y, int colour) {
        graphics.drawString(font, text, rightX - font.width(text), y, colour, false);
    }

    static void right(GuiGraphics graphics, Font font, String text, int rightX, int y, int colour) {
        graphics.drawString(font, text, rightX - font.width(text), y, colour, false);
    }

    static void centered(GuiGraphics graphics, Font font, Component text, int left, int right, int y, int colour) {
        graphics.drawString(font, text, left + (right - left - font.width(text)) / 2, y, colour, false);
    }

    static void head(GuiGraphics graphics, UUID player, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerInfo info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(player);
        PlayerFaceRenderer.draw(graphics, info != null ? info.getSkin() : DefaultPlayerSkin.get(player), x, y, 8);
    }

    static String count(long value) {
        if (value < 10_000L) return Long.toString(value);
        if (value < 1_000_000L) return value / 1_000L + "k";
        return value / 1_000_000L + "m";
    }
    //endregion

    static void scrollbar(SpriteBatch sprites, int x, int y, int height, int offset, int visible, int total,
                          boolean hover) {
        sprites.blit(SCROLLER_TRACK, x, y, 6, height);
        if (total <= visible) return;
        int thumb = Math.max(8, height * visible / total);
        int top = y + (height - thumb) * offset / (total - visible);
        sprites.blit(hover ? SCROLLER_HIGHLIGHTED : SCROLLER, x, top, 6, thumb);
    }

    static int clampScroll(int offset, int visible, int total) {
        return Math.max(0, Math.min(offset, total - visible));
    }
}
