package net.alex.guzhenren.client.screen.blissful;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.blissful.mountain.MountainModule;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.blissful.panel.Page;
import net.alex.guzhenren.gameplay.blissful.panel.PanelAction;
import net.alex.guzhenren.gameplay.blissful.panel.PanelActions;
import net.alex.guzhenren.gameplay.blissful.panel.PanelSnapshot;
import net.alex.guzhenren.network.payload.EarthSpiritActionPayload;
import net.alex.guzhenren.network.payload.EarthSpiritPanelPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地灵[Earth Spirit]经营面板：粉色面板加左侧外挂分页标签（一个 {@link Page} 一个），各页由 {@link PanelPage} 子类绘制。
 * <ul>
 *   <li>布局按面板美术规格：主面板 176×222，标签在面板左侧外挂（未选中先画、被面板压住右端；选中后画、盖住面板左框），
 *       整体231×222 居中，所以 {@code leftPos} 右移 {@value #TAB_SPAN} 的一半。</li>
 *   <li>绘制顺序：一个 {@code GuiBatch} 里先一个 {@link SpriteBatch} 画全部精灵图，再填充、再文字；物品图标与头像在批
 *       外；弹窗与操作提示在 {@code super.render} 之后，盖在背包格与光标物品之上。</li>
 *   <li>数字键切页；鼠标停在背包格上时数字键仍归原版（与快捷栏交换）。切页时发 {@link PanelActions.View}：服务端只
 *       算正在看的页，也记下这一页，下次开面板直接停在这里。</li>
 *   <li>库藏页是 {@link Storage}，其余页是 {@link Pages}，跨这条线切页就换一个界面实例（同一个菜单，页码、两个输入框内
 *       容与提示带过去，各页滚动位置重置）：整理类模组按界面类挂件，InventoryProfilesNext 只认 {@link IPNIgnore}，所
 *       以只有库藏页出现它的整理按钮与快捷栏箭头。客户端 {@code removed} 不动菜单，服务端菜单不受换界面影响。</li>
 *   <li>两个输入框：荡魂山页的白名单名字（灰字提示匹配的在线玩家名，Tab 补全，回车添加）、库藏页的搜索；聚焦时吞掉除
 *       Esc 外的按键，免得按 E 关掉面板。</li>
 *   <li>左侧标签列算面板内（{@link #hasClickedOutside}）：手上拿着库藏物品点标签，松手时原版会把它当点在面板外扔出
 *       去。</li>
 *   <li>数据只来自菜单里按页缓存的快照段：开面板时随缓冲带来，之后每秒与每次操作后由 {@link EarthSpiritPanelPayload}
 *       替换（{@link #onPanel}）。操作结果提示在面板上方显示 {@value #NOTICE_MILLIS} 毫秒。</li>
 *   <li>本文件有意超过 300 行：六页切换容器、两处输入框与共享外框/标签/悬停绘制共用同一界面生命周期，集中在一处维护
 *       更清晰。</li>
 * </ul>
 */

public abstract class EarthSpiritScreen extends AbstractContainerScreen<EarthSpiritMenu> {

    static final String UI = "guzhenren.blissful_land.ui.";
    static final String TIP = "guzhenren.blissful_land.tip.";
    private static final int TAB_SPAN = 55;
    private static final int TAB_X = -53;
    private static final int TAB_SELECTED_X = -55;
    private static final int TAB_W = 58;
    private static final int TAB_H = 22;
    private static final int TAB_TOP = 6;
    private static final int TAB_PITCH = 24;
    private static final int RULE_Y = 15;
    private static final int NOTICE_MILLIS = 4000;
    private static final int SEARCH_LENGTH = 32;
    private static final String[] TAB_ICONS = {"icon_overview", "icon_mountain", "icon_husbandry", "icon_farming",
            "icon_territory", "icon_storage"};
    private final Map<Page, PanelPage> pages = new EnumMap<>(Page.class);
    private final Inventory inventory;
    private Page page;
    private EditBox nameField;
    private EditBox searchField;
    private String keptName = "";
    private String keptQuery = "";
    private @Nullable String completion;
    private Component notice = Component.empty();
    private long noticeUntil;
    private PanelSnapshot seen;

    private EarthSpiritScreen(EarthSpiritMenu menu, Inventory inventory, Component title, Page page) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.page = page;
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.pages.put(Page.OVERVIEW, new OverviewPage(this));
        this.pages.put(Page.MOUNTAIN, new MountainPage(this));
        this.pages.put(Page.HUSBANDRY, new HerdPage(this));
        this.pages.put(Page.FARMING, new FarmPage(this));
        this.pages.put(Page.TERRITORY, new TerritoryPage(this));
        this.pages.put(Page.STORAGE, new StoragePage(this));
        this.seen = menu.getSnapshot();
    }

    public static @NotNull EarthSpiritScreen open(EarthSpiritMenu menu, Inventory inventory, Component title) {
        Page page = menu.getSnapshot().section().page();
        return page == Page.STORAGE ? new Storage(menu, inventory, title, page)
                : new Pages(menu, inventory, title, page);
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
        String name = this.nameField == null ? this.keptName : this.nameField.getValue();
        this.nameField = field(this.leftPos + 12, this.topPos + 90, 120, MountainModule.NAME_LENGTH, "name_hint", name);
        this.nameField.setResponder(this::suggestName);
        String query = this.searchField == null ? this.keptQuery : this.searchField.getValue();
        this.searchField = field(this.leftPos + StoragePage.SEARCH_X + 3, this.topPos + StoragePage.SEARCH_Y + 3,
                StoragePage.SEARCH_W - 6, SEARCH_LENGTH, "search_hint", query);
        this.addRenderableWidget(this.nameField);
        this.addRenderableWidget(this.searchField);
        this.menu.setInventoryShown(this instanceof Storage);
        suggestName(name);
        applyPage();
    }

    private EditBox field(int x, int y, int width, int length, String hint, String value) {
        EditBox box = new EditBox(this.font, x, y, width, 10, Component.translatable(UI + hint));
        box.setBordered(false);
        box.setMaxLength(length);
        box.setTextColor(PanelKit.TEXT);
        box.setHint(Component.translatable(UI + hint).withColor(PanelKit.DEAD));
        box.setValue(value);
        return box;
    }

    private void show(Page next) {
        pages.get(this.page).closeOverlay();
        if (next == this.page) return;
        send(new PanelActions.View(next));
        if ((next == Page.STORAGE) != (this instanceof Storage)) {
            EarthSpiritScreen other = next == Page.STORAGE ? new Storage(this.menu, this.inventory, this.title, next)
                    : new Pages(this.menu, this.inventory, this.title, next);
            other.keptName = this.nameField.getValue();
            other.keptQuery = this.searchField.getValue();
            other.notice = this.notice;
            other.noticeUntil = this.noticeUntil;
            other.seen = this.seen;
            Minecraft.getInstance().setScreen(other);
            return;
        }
        this.page = next;
        applyPage();
    }

    private void applyPage() {
        this.nameField.setVisible(this.page == Page.MOUNTAIN);
        if (this.page != Page.MOUNTAIN) this.nameField.setFocused(false);
        this.searchField.setVisible(this.page == Page.STORAGE);
        if (this.page != Page.STORAGE) this.searchField.setFocused(false);
    }

    void openPage(Page next) { show(next); }

    Font font() { return this.font; }

    boolean isNameFieldFocused() { return this.nameField.isFocused(); }

    boolean isSearchFocused() { return this.searchField.isFocused(); }

    String searchQuery() { return this.searchField.getValue().strip().toLowerCase(Locale.ROOT); }

    String takeNameField() {
        String value = this.nameField.getValue();
        this.nameField.setValue("");
        return value;
    }

    void send(PanelAction action) { PacketDistributor.sendToServer(new EarthSpiritActionPayload(action)); }

    //region Name completion [名字补全]
    private void suggestName(String typed) {
        this.completion = null;
        if (!typed.isEmpty() && Minecraft.getInstance().getConnection() != null) {
            String prefix = typed.toLowerCase(Locale.ROOT);
            for (PlayerInfo info : Minecraft.getInstance().getConnection().getOnlinePlayers()) {
                String name = info.getProfile().getName();
                if (name.length() > typed.length() && name.toLowerCase(Locale.ROOT).startsWith(prefix)
                        && (this.completion == null || name.compareToIgnoreCase(this.completion) < 0)) {
                    this.completion = name;
                }
            }
        }
        this.nameField.setSuggestion(this.completion == null ? null : this.completion.substring(typed.length()));
    }

    void addTypedName() {
        String name = takeNameField().strip();
        if (!name.isEmpty()) send(new MountainModule.AddWhitelist(name));
    }
    //endregion

    //region Rendering [绘制]
    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        PanelPage current = pages.get(this.page);
        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> {
                for (Page each : Page.values()) {
                    if (each != this.page) drawTab(sprites, each, x, y, mouseX, mouseY);
                }
                sprites.blit(PanelKit.PANEL, x, y, this.imageWidth, this.imageHeight);
                drawTab(sprites, this.page, x, y, mouseX, mouseY);
                current.sprites(sprites, x, y, mouseX, mouseY);
            });
            graphics.fill(x + 7, y + RULE_Y, x + 168, y + RULE_Y + 1, PanelKit.ACCENT);
            current.fills(graphics, x, y, mouseX, mouseY);
            Component heading = Component.translatable(this.menu.getSnapshot().landNameKey()).append(" · ")
                    .append(current.name());
            graphics.drawString(this.font, heading, x + 8, y + 6, PanelKit.ACCENT, false);
            for (Page each : Page.values()) drawTabLabel(graphics, each, x, y, mouseX, mouseY);
            current.texts(graphics, x, y, mouseX, mouseY);
        });
        current.items(graphics, x, y, mouseX, mouseY);
    }

    private void drawTab(SpriteBatch sprites, Page each, int x, int y, int mouseX, int mouseY) {
        boolean selected = each == this.page;
        int left = x + (selected ? TAB_SELECTED_X : TAB_X);
        int top = y + TAB_TOP + each.ordinal() * TAB_PITCH;
        boolean hover = !selected && isOverTab(each, mouseX, mouseY);
        String name = selected ? "tab_selected" : hover ? "tab_highlighted" : "tab";
        sprites.blit(PanelKit.sprite(name), left, top, TAB_W, TAB_H);
        sprites.blit(PanelKit.sprite(TAB_ICONS[each.ordinal()]), left + 5, top + 3, 16, 16);
    }

    private void drawTabLabel(GuiGraphics graphics, Page each, int x, int y, int mouseX, int mouseY) {
        boolean selected = each == this.page;
        int left = x + (selected ? TAB_SELECTED_X : TAB_X);
        int top = y + TAB_TOP + each.ordinal() * TAB_PITCH;
        int colour = selected ? PanelKit.ACCENT : isOverTab(each, mouseX, mouseY) ? PanelKit.TEXT : PanelKit.LEGEND;
        graphics.drawString(this.font, pages.get(each).name(), left + 24, top + 7, colour, false);
    }

    private boolean isOverTab(Page each, double mouseX, double mouseY) {
        int top = this.topPos + TAB_TOP + each.ordinal() * TAB_PITCH;
        return Widgets.isOver(mouseX, mouseY, this.leftPos + TAB_SELECTED_X, top, -TAB_SELECTED_X, TAB_H);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return super.hasClickedOutside(mouseX, mouseY, left, top, button) && !Widgets.isOver(mouseX, mouseY,
                left + TAB_SELECTED_X, top + TAB_TOP, -TAB_SELECTED_X, Page.values().length * TAB_PITCH);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        trackNotice();
        super.render(graphics, mouseX, mouseY, partialTick);
        PanelPage current = pages.get(this.page);
        if (!current.hasOverlay()) {
            current.tooltip(graphics, this.leftPos, this.topPos, mouseX, mouseY);
            this.renderTooltip(graphics, mouseX, mouseY);
        }
        if (Util.getMillis() < this.noticeUntil) {
            PanelKit.centered(graphics, this.font, this.notice, this.leftPos, this.leftPos + this.imageWidth,
                    this.topPos - 12, PanelKit.ACCENT);
        }
        if (current.hasOverlay()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            current.overlay(graphics, this.leftPos, this.topPos, mouseX, mouseY);
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
        PanelPage current = pages.get(this.page);
        if (current.hasOverlay()) {
            if (button == 0) current.overlayClick(this.leftPos, this.topPos, mouseX, mouseY);
            return true;
        }
        if (button == 0) {
            for (Page each : Page.values()) {
                if (isOverTab(each, mouseX, mouseY)) {
                    show(each);
                    return true;
                }
            }
            if (current.click(this.leftPos, this.topPos, mouseX, mouseY, button, Screen.hasShiftDown())) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pages.get(this.page).scroll(this.leftPos, this.topPos, mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PanelPage current = pages.get(this.page);
        if (keyCode == InputConstants.KEY_ESCAPE && current.hasOverlay()) {
            current.closeOverlay();
            return true;
        }
        EditBox focused = this.nameField.isFocused() ? this.nameField
                : this.searchField.isFocused() ? this.searchField : null;
        if (focused != null && keyCode != InputConstants.KEY_ESCAPE) {
            if (focused == this.nameField && keyCode == InputConstants.KEY_TAB && this.completion != null) {
                this.nameField.setValue(this.completion);
                return true;
            }
            if (focused == this.nameField
                    && (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)) {
                addTypedName();
                return true;
            }
            focused.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        int digit = keyCode - InputConstants.KEY_1;
        if (this.hoveredSlot == null && !current.hasOverlay() && digit >= 0 && digit < Page.values().length) {
            show(Page.values()[digit]);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    //endregion

    /** 库藏以外的页：背包格隐藏，InventoryProfilesNext 不挂件。 */
    @IPNIgnore
    public static final class Pages extends EarthSpiritScreen {
        private Pages(EarthSpiritMenu menu, Inventory inventory, Component title, Page page) {
            super(menu, inventory, title, page);
        }
    }

    /** 库藏页：背包格可见，存取靠它们，整理类模组照常挂件。 */
    public static final class Storage extends EarthSpiritScreen {
        private Storage(EarthSpiritMenu menu, Inventory inventory, Component title, Page page) {
            super(menu, inventory, title, page);
        }
    }
}
