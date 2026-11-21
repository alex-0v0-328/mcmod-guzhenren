package net.alex.guzhenren.client.screen.blissful;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.gameplay.blissful.PanelSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 地灵经营面板的一页：按「精灵图 → 填充 → 文字 → 物品图标 → 悬停提示」五步绘制，坐标都相对面板左上角
 * {@code (x, y)}，鼠标坐标是屏幕坐标。
 * <ul>
 *   <li>{@link #sprites} 在 {@link EarthSpiritScreen} 的同一个 {@link SpriteBatch} 里画；{@link #fills}、{@link #texts}
 *       在同一个 {@code GuiBatch} 里，先填充后文字；物品图标不能进批处理，单独在 {@link #items} 画。</li>
 *   <li>操作只经 {@link EarthSpiritScreen#send} 发意图，不在客户端改快照；服务端算好的新快照随后下发。</li>
 *   <li>{@link #overlay} 与 {@link #overlayClick} 供带弹窗的页面（疆域页的二次确认）盖在一切之上并独占点击。</li>
 * </ul>
 */

abstract class PanelPage {

    protected final EarthSpiritScreen screen;

    PanelPage(EarthSpiritScreen screen) { this.screen = screen; }

    abstract Component name();

    PanelSnapshot snapshot() { return screen.getMenu().getSnapshot(); }

    Font font() { return screen.font(); }

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
