package net.alex.guzhenren.client.screen.immortalaperture;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelAction;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSection;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSectionSource;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 地灵经营面板的一页（或信息页里的一个小节）：按「精灵图 → 填充 → 文字 → 物品图标 → 悬停提示」五步绘制，鼠标坐标是
 * 屏幕坐标；四页都全屏，{@code (x, y)} 在库藏页是两张卡片的左上角（背包格按它摆），其余页恒为 (0, 0)，排版按
 * {@link #resized} 给的整窗尺寸算。
 * <ul>
 *   <li>{@link #sprites} 在 {@link EarthSpiritScreen} 的同一个 {@link SpriteBatch} 里画；{@link #fills}、{@link #texts}
 *       在同一个 {@code GuiBatch} 里，先填充后文字；物品图标不能进批处理，单独在 {@link #items} 画。顶栏在这五步之后另画，
 *       盖在页面之上。</li>
 *   <li>数据经 {@link #section} 取所属面板段最近一份快照段；切到还没收到过的页时先拿面板段给的空段，下一刻服务端回传后就有。
 *       </li>
 *   <li>操作只经 {@link #send} 发意图，不在客户端改快照；服务端算好的新快照随后下发。</li>
 *   <li>按钮的说明走 {@link #tip}：鼠标停在区域上就画一段自动换行的悬停提示。</li>
 *   <li>{@link #overlay} 与 {@link #overlayClick} 供带弹窗的页面（疆域页的二次确认）盖在一切之上并独占点击。</li>
 *   <li>输入钩子返回真 = 吃掉事件：{@link #click} 在地图页收所有鼠标键、其余页只收左键；{@link #drag}、{@link #release}、
 *       {@link #key}（输入框聚焦时不转来，数字键切页在它之后）照转当前页。</li>
 *   <li>生命周期：{@link #resized} 在界面每次布局时给当前页整窗尺寸；{@link #tick} 每客户端刻调当前页；{@link #hidden}
 *       是切走本页；{@link #closed} 是整个面板关掉（库藏页与其他页互换界面实例不算，页面对象随之带过去，状态保留）。</li>
 * </ul>
 */

public abstract class PanelPage {

    private static final int TIP_WIDTH = 170;
    protected EarthSpiritScreen screen;

    protected PanelPage(EarthSpiritScreen screen) { this.screen = screen; }

    void attach(EarthSpiritScreen screen) { this.screen = screen; }

    protected abstract Component name();

    protected <S extends PanelSection> S section(PanelSectionSource<S> module, S empty) {
        S section = menu().section(module);
        return section == null ? empty : section;
    }

    protected EarthSpiritMenu menu() { return screen.getMenu(); }

    protected void send(PanelAction action) { screen.send(action); }

    protected Font font() { return screen.font(); }

    protected int width() { return screen.width; }

    protected int height() { return screen.height; }

    protected boolean tip(GuiGraphics graphics, int left, int top, int width, int height, int mouseX, int mouseY,
                          Component text) {
        if (!Widgets.isOver(mouseX, mouseY, left, top, width, height)) return false;
        graphics.renderTooltip(font(), font().split(text, TIP_WIDTH), mouseX, mouseY);
        return true;
    }

    //region Drawing [绘制]
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {}

    protected void fills(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {}

    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {}

    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {}

    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {}
    //endregion

    //region Input [输入]
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) { return false; }

    protected boolean drag(int x, int y, double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    protected boolean release(int x, int y, double mouseX, double mouseY, int button) { return false; }

    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) { return false; }

    protected boolean key(int keyCode, int scanCode, int modifiers) { return false; }
    //endregion

    //region Lifecycle [生命周期]
    protected void resized(int width, int height) {}

    protected void tick() {}

    protected void hidden() {}

    protected void closed() {}
    //endregion

    //region Overlay [弹窗]
    protected boolean hasOverlay() { return false; }

    protected void overlay(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {}

    protected void overlayClick(int x, int y, double mouseX, double mouseY) {}

    protected void closeOverlay() {}
    //endregion
}
