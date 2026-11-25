package net.alex.guzhenren.client.screen.blissful;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelSection;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 地灵经营面板的一页：按「精灵图 → 填充 → 文字 → 物品图标 → 悬停提示」五步绘制，坐标都相对面板左上角
 * {@code (x, y)}，鼠标坐标是屏幕坐标。
 * <ul>
 *   <li>{@link #sprites} 在 {@link EarthSpiritScreen} 的同一个 {@link SpriteBatch} 里画；{@link #fills}、{@link #texts}
 *       在同一个 {@code GuiBatch} 里，先填充后文字；物品图标不能进批处理，单独在 {@link #items} 画。</li>
 *   <li>数据经 {@link #section} 取本页最近一份快照段；切到还没收到过的页时先拿模块给的空段，下一刻服务端回传后就有。
 *       </li>
 *   <li>操作只经 {@link #send} 发意图，不在客户端改快照；服务端算好的新快照随后下发。</li>
 *   <li>按钮的说明走 {@link #tip}：鼠标停在区域上就画一段自动换行的悬停提示。</li>
 *   <li>{@link #overlay} 与 {@link #overlayClick} 供带弹窗的页面（疆域页的二次确认）盖在一切之上并独占点击。</li>
 * </ul>
 */

abstract class PanelPage {

    private static final int TIP_WIDTH = 170;
    protected final EarthSpiritScreen screen;

    PanelPage(EarthSpiritScreen screen) { this.screen = screen; }

    abstract Component name();

    @SuppressWarnings("unchecked")
    <S extends PanelSection> S section(S empty) {
        PanelSection section = screen.getMenu().section(empty.page());
        return section == null ? empty : (S) section;
    }

    void send(PanelAction action) { screen.send(action); }

    Font font() { return screen.font(); }

    boolean tip(GuiGraphics graphics, int left, int top, int width, int height, int mouseX, int mouseY,
                Component text) {
        if (!Widgets.isOver(mouseX, mouseY, left, top, width, height)) return false;
        graphics.renderTooltip(font(), font().split(text, TIP_WIDTH), mouseX, mouseY);
        return true;
    }

    void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {}

    void fills(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {}

    void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {}

    void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {}

    void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {}

    boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) { return false; }

    boolean scroll(int x, int y, double mouseX, double mouseY, double delta) { return false; }

    boolean hasOverlay() { return false; }

    void overlay(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {}

    void overlayClick(int x, int y, double mouseX, double mouseY) {}

    void closeOverlay() {}
}
