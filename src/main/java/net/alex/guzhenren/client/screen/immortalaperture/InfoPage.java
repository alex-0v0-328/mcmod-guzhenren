package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 「信息」页：顶栏下一条二级标签（{@code subtab}，常态只有底线，悬停变亮，选中金底线）切换四个小节（总览、养殖、种植、
 * 蛊虫），小节正文由各自的 {@link PanelPage} 从 {@link #CONTENT_TOP} 起排卡片，绘制、输入与弹窗都转给当前小节。
 * <ul>
 *   <li>四个小节的数据同属信息页，服务端每秒一起算；切小节只在客户端，不发 {@code View}。</li>
 *   <li>界面实例在库藏页与其他页间互换时本页对象随之带过去，停在哪个小节保留；重开面板回到总览。</li>
 * </ul>
 */

final class InfoPage extends PanelPage {

    static final int OVERVIEW = 0;
    static final int HERD = 1;
    static final int FARM = 2;
    static final int GU = 3;
    static final int CONTENT_TOP = EarthSpiritScreen.BAR_H + 29;
    private static final String UI = EarthSpiritScreen.UI;
    private static final int SUBTAB_Y = EarthSpiritScreen.BAR_H + 6;
    private static final int SUBTAB_W = 44;
    private static final int SUBTAB_H = 16;
    private static final int SUBTAB_PITCH = 46;
    private static final ResourceLocation SUBTAB = PanelDrawing.sprite("subtab");
    private static final ResourceLocation SUBTAB_HIGHLIGHTED = PanelDrawing.sprite("subtab_highlighted");
    private static final ResourceLocation SUBTAB_SELECTED = PanelDrawing.sprite("subtab_selected");
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

    private int subtabLeft(int index) { return PanelDrawing.contentX(width()) + index * SUBTAB_PITCH; }

    private boolean isOverSubtab(int index, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, subtabLeft(index), SUBTAB_Y, SUBTAB_W, SUBTAB_H);
    }

    @Override
    protected Component name() { return Component.translatable(UI + "tab.info"); }

    //region Drawing [绘制]
    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < this.parts.size(); i++) {
            ResourceLocation tab = i == this.part ? SUBTAB_SELECTED
                    : isOverSubtab(i, mouseX, mouseY) ? SUBTAB_HIGHLIGHTED : SUBTAB;
            sprites.blit(tab, subtabLeft(i), SUBTAB_Y, SUBTAB_W, SUBTAB_H);
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
            int colour = i == this.part ? PanelDrawing.ACCENT
                    : isOverSubtab(i, mouseX, mouseY) ? PanelDrawing.TEXT : PanelDrawing.LEGEND;
            PanelDrawing.centered(graphics, font(), this.parts.get(i).name(), subtabLeft(i), subtabLeft(i) + SUBTAB_W,
                    SUBTAB_Y + 4, colour);
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
            if (isOverSubtab(i, mouseX, mouseY)) {
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
