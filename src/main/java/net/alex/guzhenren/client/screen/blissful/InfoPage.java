package net.alex.guzhenren.client.screen.blissful;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 「信息」页：面板顶行一排小按钮切换四个小节（总览、养殖、种植、蛊虫），小节正文由各自的 {@link PanelPage} 绘制，
 * 绘制、输入与弹窗都转给当前小节。
 * <ul>
 *   <li>四个小节的数据同属信息页，服务端每秒一起算；切小节只在客户端，不发 {@code View}。</li>
 *   <li>顶行按钮占掉原来的页眉位置，本页不画「福地名 · 页名」页眉（{@link #drawsHeading}）。</li>
 *   <li>界面实例在库藏页与其他页间互换时本页对象随之带过去，停在哪个小节保留；重开面板回到总览。</li>
 * </ul>
 */

final class InfoPage extends PanelPage {

    static final int OVERVIEW = 0;
    static final int HERD = 1;
    static final int FARM = 2;
    static final int GU = 3;
    private static final String UI = EarthSpiritScreen.UI;
    private static final int BUTTON_X = 8;
    private static final int BUTTON_Y = 3;
    private static final int BUTTON_W = 30;
    private static final int BUTTON_H = 12;
    private static final int BUTTON_PITCH = 32;
    private final List<PanelPage> parts;
    private int part = OVERVIEW;

    InfoPage(EarthSpiritScreen screen) {
        super(screen);
        this.parts = List.of(new OverviewPage(screen), new HerdPage(screen), new FarmPage(screen), new GuPage(screen));
    }

    void open(int next) {
        if (next == this.part) return;
        current().closeOverlay();
        current().hidden();
        this.part = next;
        current().resized(width(), height());
    }

    int part() { return this.part; }

    private PanelPage current() { return this.parts.get(this.part); }

    @Override
    void attach(EarthSpiritScreen screen) {
        super.attach(screen);
        for (PanelPage each : this.parts) each.attach(screen);
    }

    private boolean isOverButton(int x, int y, int index, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, x + BUTTON_X + index * BUTTON_PITCH, y + BUTTON_Y, BUTTON_W, BUTTON_H);
    }

    @Override
    protected Component name() { return Component.translatable(UI + "tab.info"); }

    @Override
    protected boolean drawsHeading() { return true; }

    //region Drawing [绘制]
    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < this.parts.size(); i++) {
            boolean selected = i == this.part;
            PanelKit.button(sprites, x + BUTTON_X + i * BUTTON_PITCH, y + BUTTON_Y, BUTTON_W, BUTTON_H, true,
                    !selected && isOverButton(x, y, i, mouseX, mouseY), selected, false);
        }
        current().sprites(sprites, x, y, mouseX, mouseY);
    }

    @Override
    protected void fills(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        current().fills(graphics, x, y, mouseX, mouseY);
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < this.parts.size(); i++) {
            int left = x + BUTTON_X + i * BUTTON_PITCH;
            PanelKit.centered(graphics, font(), this.parts.get(i).name(), left, left + BUTTON_W, y + BUTTON_Y + 2,
                    i == this.part ? PanelKit.ACCENT : PanelKit.TEXT);
        }
        current().texts(graphics, x, y, mouseX, mouseY);
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        current().items(graphics, x, y, mouseX, mouseY);
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        current().tooltip(graphics, x, y, mouseX, mouseY);
    }
    //endregion

    //region Input [输入]
    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        for (int i = 0; i < this.parts.size(); i++) {
            if (isOverButton(x, y, i, mouseX, mouseY)) {
                open(i);
                return true;
            }
        }
        return current().click(x, y, mouseX, mouseY, button, shift);
    }

    @Override
    protected boolean drag(int x, int y, double mouseX, double mouseY, int button, double dragX, double dragY) {
        return current().drag(x, y, mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean release(int x, int y, double mouseX, double mouseY, int button) {
        return current().release(x, y, mouseX, mouseY, button);
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        return current().scroll(x, y, mouseX, mouseY, delta);
    }

    @Override
    protected boolean key(int keyCode, int scanCode, int modifiers) {
        return current().key(keyCode, scanCode, modifiers);
    }
    //endregion

    //region Lifecycle [生命周期]
    @Override
    protected void resized(int width, int height) { current().resized(width, height); }

    @Override
    protected void tick() { current().tick(); }

    @Override
    protected void hidden() { current().hidden(); }

    @Override
    protected void closed() {
        for (PanelPage each : this.parts) each.closed();
    }
    //endregion

    //region Overlay [弹窗]
    @Override
    protected boolean hasOverlay() { return current().hasOverlay(); }

    @Override
    protected void overlay(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        current().overlay(graphics, x, y, mouseX, mouseY);
    }

    @Override
    protected void overlayClick(int x, int y, double mouseX, double mouseY) {
        current().overlayClick(x, y, mouseX, mouseY);
    }

    @Override
    protected void closeOverlay() { current().closeOverlay(); }
    //endregion
}
