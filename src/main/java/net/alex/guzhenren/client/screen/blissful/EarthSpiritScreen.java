package net.alex.guzhenren.client.screen.blissful;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.blissful.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.blissful.PanelAction;
import net.alex.guzhenren.gameplay.blissful.PanelSnapshot;
import net.alex.guzhenren.network.payload.EarthSpiritActionPayload;
import net.alex.guzhenren.network.payload.EarthSpiritPanelPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.jetbrains.annotations.NotNull;

/**
 * 地灵[Earth Spirit]经营面板：粉色面板加左侧外挂的六个分页标签，各页由 {@link PanelPage} 子类绘制与响应。
 * <ul>
 *   <li>布局按面板美术规格：主面板 176×222，标签在面板左侧外挂（未选中先画、被面板压住右端；选中后画、盖住面板左框），
 *       整体231×222 居中，所以 {@code leftPos} 右移 {@value #TAB_SPAN} 的一半。</li>
 *   <li>绘制顺序：一个 {@code GuiBatch} 里先一个 {@link SpriteBatch} 画全部精灵图，再填充、再文字；物品图标与头像在批
 *       外；弹窗与操作提示在 {@code super.render} 之后，盖在背包格与光标物品之上。</li>
 *   <li>数字键 1–6 切页；鼠标停在背包格上时数字键仍归原版（与快捷栏交换）。</li>
 *   <li>背包格只在库藏页显示（{@link EarthSpiritMenu#setInventoryShown}）；荡魂山白名单的名字输入框只在荡魂山页显示，聚
 *       焦时吞掉除 Esc 外的按键，免得按 E 关掉面板。</li>
 *   <li>库藏页是 {@link Storage}，其余五页是 {@link Pages}，跨这条线切页就换一个界面实例（同一个菜单，页码、输入框内容与
 *       提示带过去，各页滚动位置重置）：整理类模组按界面类挂件，InventoryProfilesNext 只认 {@link IPNIgnore}，所以只有库
 *       藏页出现它的整理按钮与快捷栏箭头。客户端 {@code removed} 不动菜单，服务端菜单不受换界面影响。</li>
 *   <li>左侧标签列算面板内（{@link #hasClickedOutside}）：手上拿着库藏物品点标签，松手时原版会把它当点在面板外扔出去。</li>
 *   <li>数据只来自菜单里的 {@link PanelSnapshot}：开面板时随缓冲带来，之后每秒与每次操作后由
 *       {@link EarthSpiritPanelPayload} 替换（{@link #onPanel}）。操作结果提示在面板上方显示 {@value #NOTICE_MILLIS} 毫
 *       秒。</li>
 * </ul>
 */

public abstract class EarthSpiritScreen extends AbstractContainerScreen<EarthSpiritMenu> {

    static final String UI = "guzhenren.blissful_land.ui.";
    private static final int TAB_SPAN = 55;
    private static final int TAB_X = -53;
    private static final int TAB_SELECTED_X = -55;
    private static final int TAB_W = 58;
    private static final int TAB_H = 22;
    private static final int TAB_TOP = 6;
    private static final int TAB_PITCH = 24;
    private static final int RULE_Y = 15;
    private static final int NOTICE_MILLIS = 4000;
    private static final String[] TAB_ICONS = {"icon_overview", "icon_mountain", "icon_husbandry", "icon_farming",
            "icon_territory", "icon_storage"};
    private final Map<Tab, PanelPage> pages = new EnumMap<>(Tab.class);
    private final Inventory inventory;
    private Tab tab;
    private EditBox nameField;
    private String keptName = "";
    private Component notice = Component.empty();
    private long noticeUntil;
    private PanelSnapshot seen;

    enum Tab { OVERVIEW, MOUNTAIN, HUSBANDRY, FARMING, TERRITORY, STORAGE }

    private EarthSpiritScreen(EarthSpiritMenu menu, Inventory inventory, Component title, Tab tab) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.tab = tab;
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.pages.put(Tab.OVERVIEW, new OverviewPage(this));
        this.pages.put(Tab.MOUNTAIN, new MountainPage(this));
        this.pages.put(Tab.HUSBANDRY, new HerdPage(this));
        this.pages.put(Tab.FARMING, new FarmPage(this));
        this.pages.put(Tab.TERRITORY, new TerritoryPage(this));
        this.pages.put(Tab.STORAGE, new StoragePage(this));
        this.seen = menu.getSnapshot();
    }

    public static @NotNull EarthSpiritScreen open(EarthSpiritMenu menu, Inventory inventory, Component title) {
        return new Pages(menu, inventory, title, Tab.OVERVIEW);
    }

    public static void onPanel(EarthSpiritPanelPayload payload) {
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.containerMenu instanceof EarthSpiritMenu menu) {
            menu.setSnapshot(payload.snapshot());
        }
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth + TAB_SPAN) / 2;
        String kept = this.nameField == null ? this.keptName : this.nameField.getValue();
        this.nameField = new EditBox(this.font, this.leftPos + 12, this.topPos + 90, 120, 10,
                Component.translatable(UI + "name_hint"));
        this.nameField.setBordered(false);
        this.nameField.setMaxLength(16);
        this.nameField.setTextColor(PanelKit.TEXT);
        this.nameField.setHint(Component.translatable(UI + "name_hint").withColor(PanelKit.DEAD));
        this.nameField.setValue(kept);
        this.addRenderableWidget(this.nameField);
        this.menu.setInventoryShown(this instanceof Storage);
        applyTab();
    }

    private void show(Tab next) {
        pages.get(this.tab).closeOverlay();
        if ((next == Tab.STORAGE) != (this instanceof Storage)) {
            EarthSpiritScreen other = next == Tab.STORAGE ? new Storage(this.menu, this.inventory, this.title, next)
                    : new Pages(this.menu, this.inventory, this.title, next);
            other.keptName = this.nameField.getValue();
            other.notice = this.notice;
            other.noticeUntil = this.noticeUntil;
            other.seen = this.seen;
            Minecraft.getInstance().setScreen(other);
            return;
        }
        this.tab = next;
        applyTab();
    }

    private void applyTab() {
        this.nameField.setVisible(this.tab == Tab.MOUNTAIN);
        if (this.tab != Tab.MOUNTAIN) this.nameField.setFocused(false);
    }

    Font font() { return this.font; }

    boolean isNameFieldFocused() { return this.nameField.isFocused(); }

    String takeNameField() {
        String value = this.nameField.getValue();
        this.nameField.setValue("");
        return value;
    }

    void send(PanelAction action, int value, String text) {
        if (text.length() > EarthSpiritActionPayload.MAX_TEXT) return;
        PacketDistributor.sendToServer(new EarthSpiritActionPayload(action, value, text));
    }

    //region Rendering [绘制]
    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        PanelPage page = pages.get(this.tab);
        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> {
                for (Tab each : Tab.values()) {
                    if (each != this.tab) drawTab(sprites, each, x, y, mouseX, mouseY);
                }
                sprites.blit(PanelKit.PANEL, x, y, this.imageWidth, this.imageHeight);
                drawTab(sprites, this.tab, x, y, mouseX, mouseY);
                page.sprites(sprites, x, y, mouseX, mouseY);
            });
            graphics.fill(x + 7, y + RULE_Y, x + 168, y + RULE_Y + 1, PanelKit.ACCENT);
            page.fills(graphics, x, y, mouseX, mouseY);
            Component heading = Component.translatable(this.menu.getSnapshot().landNameKey()).append(" · ")
                    .append(page.name());
            graphics.drawString(this.font, heading, x + 8, y + 6, PanelKit.ACCENT, false);
            for (Tab each : Tab.values()) drawTabLabel(graphics, each, x, y, mouseX, mouseY);
            page.texts(graphics, x, y, mouseX, mouseY);
        });
        page.items(graphics, x, y, mouseX, mouseY);
    }

    private void drawTab(SpriteBatch sprites, Tab each, int x, int y, int mouseX, int mouseY) {
        boolean selected = each == this.tab;
        int left = x + (selected ? TAB_SELECTED_X : TAB_X);
        int top = y + TAB_TOP + each.ordinal() * TAB_PITCH;
        boolean hover = !selected && isOverTab(each, mouseX, mouseY);
        String name = selected ? "tab_selected" : hover ? "tab_highlighted" : "tab";
        sprites.blit(PanelKit.sprite(name), left, top, TAB_W, TAB_H);
        sprites.blit(PanelKit.sprite(TAB_ICONS[each.ordinal()]), left + 5, top + 3, 16, 16);
    }

    private void drawTabLabel(GuiGraphics graphics, Tab each, int x, int y, int mouseX, int mouseY) {
        boolean selected = each == this.tab;
        int left = x + (selected ? TAB_SELECTED_X : TAB_X);
        int top = y + TAB_TOP + each.ordinal() * TAB_PITCH;
        int colour = selected ? PanelKit.ACCENT : isOverTab(each, mouseX, mouseY) ? PanelKit.TEXT : PanelKit.LEGEND;
        graphics.drawString(this.font, pages.get(each).name(), left + 24, top + 7, colour, false);
    }

    private boolean isOverTab(Tab each, double mouseX, double mouseY) {
        int top = this.topPos + TAB_TOP + each.ordinal() * TAB_PITCH;
        return Widgets.isOver(mouseX, mouseY, this.leftPos + TAB_SELECTED_X, top, -TAB_SELECTED_X, TAB_H);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return super.hasClickedOutside(mouseX, mouseY, left, top, button) && !Widgets.isOver(mouseX, mouseY,
                left + TAB_SELECTED_X, top + TAB_TOP, -TAB_SELECTED_X, Tab.values().length * TAB_PITCH);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        trackNotice();
        super.render(graphics, mouseX, mouseY, partialTick);
        PanelPage page = pages.get(this.tab);
        if (!page.hasOverlay()) {
            page.tooltip(graphics, this.leftPos, this.topPos, mouseX, mouseY);
            this.renderTooltip(graphics, mouseX, mouseY);
        }
        if (Util.getMillis() < this.noticeUntil) {
            PanelKit.centered(graphics, this.font, this.notice, this.leftPos, this.leftPos + this.imageWidth,
                    this.topPos - 12, PanelKit.ACCENT);
        }
        if (page.hasOverlay()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            page.overlay(graphics, this.leftPos, this.topPos, mouseX, mouseY);
            graphics.pose().popPose();
        }
    }

    private void trackNotice() {
        PanelSnapshot snapshot = this.menu.getSnapshot();
        if (snapshot == this.seen) return;
        this.seen = snapshot;
        if (snapshot.notice().getString().isEmpty()) return;
        this.notice = snapshot.notice();
        this.noticeUntil = Util.getMillis() + NOTICE_MILLIS;
    }
    //endregion

    //region Input [输入]
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PanelPage page = pages.get(this.tab);
        if (page.hasOverlay()) {
            if (button == 0) page.overlayClick(this.leftPos, this.topPos, mouseX, mouseY);
            return true;
        }
        if (button == 0) {
            for (Tab each : Tab.values()) {
                if (isOverTab(each, mouseX, mouseY)) {
                    if (each != this.tab) show(each);
                    return true;
                }
            }
            if (page.click(this.leftPos, this.topPos, mouseX, mouseY, button, Screen.hasShiftDown())) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pages.get(this.tab).scroll(this.leftPos, this.topPos, mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PanelPage page = pages.get(this.tab);
        if (keyCode == InputConstants.KEY_ESCAPE && page.hasOverlay()) {
            page.closeOverlay();
            return true;
        }
        if (this.nameField.isFocused() && keyCode != InputConstants.KEY_ESCAPE) {
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                String name = takeNameField();
                if (!name.isBlank()) send(PanelAction.WHITELIST_ADD, 0, name.strip());
                return true;
            }
            this.nameField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        int digit = keyCode - InputConstants.KEY_1;
        if (this.hoveredSlot == null && !page.hasOverlay() && digit >= 0 && digit < Tab.values().length) {
            show(Tab.values()[digit]);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    //endregion

    /** 库藏以外的五页：背包格隐藏，InventoryProfilesNext 不挂件。 */
    @IPNIgnore
    public static final class Pages extends EarthSpiritScreen {
        private Pages(EarthSpiritMenu menu, Inventory inventory, Component title, Tab tab) {
            super(menu, inventory, title, tab);
        }
    }

    /** 库藏页：背包格可见，存取靠它们，整理类模组照常挂件。 */
    public static final class Storage extends EarthSpiritScreen {
        private Storage(EarthSpiritMenu menu, Inventory inventory, Component title, Tab tab) {
            super(menu, inventory, title, tab);
        }
    }
}
