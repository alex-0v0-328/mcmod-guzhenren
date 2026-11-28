package net.alex.guzhenren.client.screen.immortalaperture.map;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapPick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 地图右键菜单：不拿工具时右键（不拖动）地图上已探明的一列，在光标旁弹一行键「传送至 x, y, z」（Alex 定删去「回到地灵
 * 身边」），点它传送并关菜单，点菜单外、右键别处或 Esc 即关；菜单开着时落点那一列画一面金色小旗（{@link #hit}）。
 * <ul>
 *   <li>坐标写该列地面上一格；服务端另找落脚处（流体、站不下时就近挪），以服务端提示为准。</li>
 *   <li>宽度按两行文字取较宽者；贴光标右下，越出地图区时翻到左边或上边。</li>
 * </ul>
 */

final class MapTeleportMenu {

    private static final int PAD = 4;
    private static final int ROW_H = Widgets.GHOST_H;
    private static final int HEIGHT = PAD * 2 + ROW_H;
    private static final String UI = EarthSpiritScreen.UI + "map.teleport.";
    private MapPick.@Nullable Hit hit;
    private int left;
    private int top;
    private int width;

    boolean isOpen() { return hit != null; }

    void open(MapPick.Hit target, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        hit = target;
        Font font = Minecraft.getInstance().font;
        width = font.width(here()) + PAD * 2 + 12;
        left = mouseX + 4 + width > screenWidth ? mouseX - 4 - width : mouseX + 4;
        top = mouseY + 4 + HEIGHT > screenHeight ? mouseY - 4 - HEIGHT : mouseY + 4;
        top = Math.max(EarthSpiritScreen.BAR_H + 2, top);
    }

    void close() { hit = null; }

    boolean contains(double mouseX, double mouseY) {
        return hit != null && Widgets.isOver(mouseX, mouseY, left, top, width, HEIGHT);
    }

    private int rowTop() { return top + PAD; }

    /** 菜单开着时的落点，地图层据此画小旗。 */
    MapPick.@Nullable Hit hit() { return hit; }

    private Component here() {
        MapPick.Hit shown = hit;
        return shown == null ? Component.empty()
                : Component.translatable(UI + "here", shown.x(), shown.ground() + 1, shown.z());
    }

    void sprites(SpriteBatch sprites, double mouseX, double mouseY) {
        if (hit == null) return;
        sprites.blit(Widgets.POPUP, left, top, width, HEIGHT);
        Widgets.button(sprites, left + PAD, rowTop(), width - PAD * 2, ROW_H, Widgets.Look.GHOST, true, mouseX,
                mouseY);
    }

    void texts(GuiGraphics graphics, Font font) {
        if (hit == null) return;
        Widgets.label(graphics, font, here(), left + PAD, rowTop(), width - PAD * 2, ROW_H, Widgets.Look.GHOST,
                true);
    }

    void click(MapTools.Land land, double mouseX, double mouseY) {
        MapPick.Hit shown = hit;
        if (shown == null) return;
        if (Widgets.isOver(mouseX, mouseY, left + PAD, rowTop(), width - PAD * 2, ROW_H)) {
            land.teleport(shown.x(), shown.z());
            close();
        }
    }
}
