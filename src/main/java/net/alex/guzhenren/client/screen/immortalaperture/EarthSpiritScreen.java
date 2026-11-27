package net.alex.guzhenren.client.screen.immortalaperture;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.map.MapPage;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.Page;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelAction;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelActions;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.PanelSnapshot;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.SecretRealmPart;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.storage.StorageBrowser;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地灵[Earth Spirit]经营面板：四个 {@link Page}（地图、库藏、秘境、信息），各页由 {@link PanelPage} 子类绘制。
 * <ul>
 *   <li>面板开着时整帧不渲染世界（{@link #hidesWorld}，{@code GameRendererMixin} 跳过；启动参数
 *       {@code -D}{@value #KEEP_WORLD_PROPERTY}{@code =true} 退回照常渲染，供光影或性能模组冲突时排查）。四页都全屏（Alex
 *       定）：最上 {@value #BAR_H} 像素是顶栏（{@code top_bar} 平铺，含金线与投影；左写「福地名 · 时辰」，右侧并排四个页签，
 *       常态页签只有图标与灰字，悬停与选中才画底板），其余页在 {@link PanelDrawing#BACKDROP} 纯色底上排卡片；地图页整片是
 *       地图，顶栏投影压在地图上。库藏页的 {@code leftPos}/{@code topPos} 是两张卡片的左上角（背包格按它摆），其余页为 0。
 *       </li>
 *   <li>秘境页的标签与页名写福地秘境的地名（{@link #realmName}）；信息页内部再分小节（{@link InfoPage}）。</li>
 *   <li>绘制顺序：一个 {@code GuiBatch} 里先一个 {@link SpriteBatch} 画全部精灵图，再填充、再文字；物品图标与头像在批
 *       外；顶栏另起一批画在页面之上；弹窗与操作提示（顶栏下居中的 {@code chip}）在 {@code super.render} 之后，盖在背包格与
 *       光标物品之上。</li>
 *   <li>数字键 1–4 切页；鼠标停在背包格上时数字键仍归原版（与快捷栏交换）。切页时发 {@link PanelActions.View}：服务端只
 *       算正在看的页，也记下这一页，下次开面板直接停在这里。</li>
 *   <li>库藏页是 {@link Storage}，其余页是 {@link Pages}，跨这条线切页就换一个界面实例（同一个菜单与同一批页面对象，页码、
 *       两个输入框内容与提示带过去）：整理类模组按界面类挂件，InventoryProfilesNext 只认 {@link IPNIgnore}，所以只有库藏
 *       页出现它的整理按钮与快捷栏箭头。客户端 {@code removed} 不动菜单，服务端菜单不受换界面影响；换实例不算关面板，
 *       不调页面的 {@code closed}。</li>
 *   <li>两个输入框：秘境页的白名单名字（灰字提示匹配的在线玩家名，Tab 补全，回车添加）、库藏页的搜索（内容一变就经
 *       {@link StoragePage} 发给服务端，筛选在服务端做；换界面实例时先填值后挂监听，不重发）；聚焦时吞掉除 Esc 外的按键，
 *       免得按 E 关掉面板。</li>
 *   <li>全屏没有「面板外」（{@link #hasClickedOutside} 恒假）：拿着物品点空处不会扔出去。</li>
 *   <li>数据只来自菜单里按面板段缓存的快照段：开面板时随缓冲带来，之后每秒与每次操作后由 {@link EarthSpiritPanelPayload}
 *       替换（{@link #onPanel}）。操作结果提示在顶栏下方显示 {@value #NOTICE_MILLIS} 毫秒。</li>
 *   <li>本文件有意超过 300 行：四页切换与全屏布局、两处输入框与共享外框/标签/顶栏/输入转发共用同一界面生命周期，集中
 *       在一处维护更清晰。</li>
 * </ul>
 */

public abstract class EarthSpiritScreen extends AbstractContainerScreen<EarthSpiritMenu> {

    public static final String UI = "guzhenren.blissful_land.ui.";
    public static final String TIP = "guzhenren.blissful_land.tip.";
    public static final String KEEP_WORLD_PROPERTY = "guzhenren.mapKeepsWorld";
    public static final int BAR_H = 26;
    private static final int TAB_W = 60;
    private static final int TAB_H = 21;
    private static final int TAB_TOP = 2;
    private static final int TAB_PITCH = 62;
    private static final int TAB_RIGHT = 5;
    private static final int NOTICE_Y = BAR_H + 6;
    private static final int NOTICE_MILLIS = 4000;
    private static final ResourceLocation TOP_BAR = PanelDrawing.sprite("top_bar");
    private static final ResourceLocation TAB_HIGHLIGHTED = PanelDrawing.sprite("tab_highlighted");
    private static final ResourceLocation TAB_SELECTED = PanelDrawing.sprite("tab_selected");
    private static final boolean KEEPS_WORLD = Boolean.getBoolean(KEEP_WORLD_PROPERTY);
    private static final int HOURS = 12;
    private static final String[] TAB_ICONS = {"icon_map", "icon_storage", "icon_mountain", "icon_info"};
    private final Inventory inventory;
    private final Map<Page, PanelPage> pages = new EnumMap<>(Page.class);
    private Page page;
    private EditBox nameField;
    private EditBox searchField;
    private String keptName = "";
    private String keptQuery = "";
    private @Nullable String completion;
    private Component notice = Component.empty();
    private long noticeUntil;
    private PanelSnapshot seen;
    private boolean swapping;

    private EarthSpiritScreen(EarthSpiritMenu menu, Inventory inventory, Component title, Page page,
                              @Nullable Map<Page, PanelPage> carried) {
        super(menu, inventory, title);
        this.inventory = inventory;
        this.page = page;
        if (carried == null) {
            this.pages.put(Page.MAP, new MapPage(this));
            this.pages.put(Page.STORAGE, new StoragePage(this));
            this.pages.put(Page.SECRET_REALM, new SecretRealmPage(this));
            this.pages.put(Page.INFO, new InfoPage(this));
        } else {
            this.pages.putAll(carried);
            for (PanelPage each : this.pages.values()) each.attach(this);
        }
        this.seen = menu.getSnapshot();
    }

    public static @NotNull EarthSpiritScreen open(EarthSpiritMenu menu, Inventory inventory, Component title) {
        Page page = menu.getSnapshot().page();
        return page == Page.STORAGE ? new Storage(menu, inventory, title, page, null)
                : new Pages(menu, inventory, title, page, null);
    }

    public static void onPanel(EarthSpiritPanelPayload payload) {
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.containerMenu instanceof EarthSpiritMenu menu) {
            menu.setSnapshot(payload.snapshot());
        }
    }

    //region Layout [布局]
    public static boolean hidesWorld() {
        return !KEEPS_WORLD && Minecraft.getInstance().screen instanceof EarthSpiritScreen;
    }

    @Override
    protected void init() {
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        super.init();
        if (this instanceof Storage) {
            this.leftPos = StoragePage.left(this.width);
            this.topPos = StoragePage.TOP;
        }
        SecretRealmPage realm = (SecretRealmPage) this.pages.get(Page.SECRET_REALM);
        realm.resized(this.width, this.height);
        String name = this.nameField == null ? this.keptName : this.nameField.getValue();
        this.nameField = field(realm.fieldX() + 4, realm.fieldY() + 3, realm.fieldW() - 8, SecretRealmPart.NAME_LENGTH,
                "name_hint", name);
        this.nameField.setResponder(this::suggestName);
        String query = this.searchField == null ? this.keptQuery : this.searchField.getValue();
        this.searchField = field(this.leftPos + StoragePage.SEARCH_X + 3, this.topPos + StoragePage.SEARCH_Y + 3,
                StoragePage.SEARCH_W - 6, StorageBrowser.TEXT_LENGTH, "search_hint", query);
        this.searchField.setResponder(text -> storage().searchChanged());
        this.addRenderableWidget(this.nameField);
        this.addRenderableWidget(this.searchField);
        this.menu.setInventoryShown(this instanceof Storage);
        suggestName(name);
        applyPage();
        current().resized(this.width, this.height);
    }

    private EditBox field(int x, int y, int width, int length, String hint, String value) {
        EditBox box = new EditBox(this.font, x, y, width, 10, Component.translatable(UI + hint));
        box.setBordered(false);
        box.setMaxLength(length);
        box.setTextColor(PanelDrawing.TEXT);
        box.setHint(Component.translatable(UI + hint).withColor(PanelDrawing.DEAD));
        box.setValue(value);
        return box;
    }

    private void show(Page next) {
        PanelPage leaving = current();
        leaving.closeOverlay();
        if (next == this.page) return;
        leaving.hidden();
        send(new PanelActions.View(next));
        if ((next == Page.STORAGE) != (this instanceof Storage)) {
            EarthSpiritScreen other = next == Page.STORAGE
                    ? new Storage(this.menu, this.inventory, this.title, next, this.pages)
                    : new Pages(this.menu, this.inventory, this.title, next, this.pages);
            other.keptName = this.nameField.getValue();
            other.keptQuery = this.searchField.getValue();
            other.notice = this.notice;
            other.noticeUntil = this.noticeUntil;
            other.seen = this.seen;
            this.swapping = true;
            Minecraft.getInstance().setScreen(other);
            return;
        }
        this.page = next;
        applyPage();
        current().resized(this.width, this.height);
    }

    private void applyPage() {
        this.nameField.setVisible(this.page == Page.SECRET_REALM);
        if (this.page != Page.SECRET_REALM) this.nameField.setFocused(false);
        this.searchField.setVisible(this.page == Page.STORAGE);
        if (this.page != Page.STORAGE) this.searchField.setFocused(false);
    }

    private PanelPage current() { return this.pages.get(this.page); }

    private StoragePage storage() { return (StoragePage) this.pages.get(Page.STORAGE); }

    @Override
    public void removed() {
        super.removed();
        if (this.swapping) return;
        for (PanelPage each : this.pages.values()) each.closed();
    }

    @Override
    protected void containerTick() { current().tick(); }
    //endregion

    //region Page access [页面访问]
    void openPage(Page next) { show(next); }

    void openInfo(int part) {
        ((InfoPage) this.pages.get(Page.INFO)).open(part);
        show(Page.INFO);
    }

    Component realmName() {
        String key = this.menu.getSnapshot().landNameKey();
        for (BlissfulLand land : BlissfulLand.ALL) {
            if (land.nameKey().equals(key)) return Component.translatable(land.realm().title().nameKey());
        }
        return Component.translatable(UI + "tab.mountain");
    }

    Font font() { return this.font; }

    boolean isNameFieldFocused() { return this.nameField.isFocused(); }

    boolean isSearchFocused() { return this.searchField.isFocused(); }

    String searchQuery() { return StorageBrowser.normalize(this.searchField.getValue()); }

    String takeNameField() {
        String value = this.nameField.getValue();
        this.nameField.setValue("");
        return value;
    }

    void send(PanelAction action) { PacketDistributor.sendToServer(new EarthSpiritActionPayload(action)); }
    //endregion

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
        if (!name.isEmpty()) send(new SecretRealmPart.AddWhitelist(name));
    }
    //endregion

    //region Rendering [绘制]
    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.page != Page.MAP) graphics.fill(0, 0, this.width, this.height, PanelDrawing.BACKDROP);
        renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        PanelPage current = current();
        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> current.sprites(sprites, x, y, mouseX, mouseY));
            current.fills(graphics, x, y, mouseX, mouseY);
            current.texts(graphics, x, y, mouseX, mouseY);
        });
        current.items(graphics, x, y, mouseX, mouseY);
        drawBar(graphics, mouseX, mouseY);
    }

    private void drawBar(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> {
                sprites.blit(TOP_BAR, 0, 0, this.width, BAR_H);
                for (Page each : Page.values()) drawTab(sprites, each, mouseX, mouseY);
            });
            MutableComponent name = landName();
            graphics.drawString(this.font, name, 8, 8, PanelDrawing.TEXT, false);
            graphics.drawString(this.font, Component.literal(" · ").append(hour()), 8 + this.font.width(name), 8,
                    PanelDrawing.LEGEND, false);
            for (Page each : Page.values()) drawTabLabel(graphics, each, mouseX, mouseY);
        });
    }

    private MutableComponent landName() { return Component.translatable(this.menu.getSnapshot().landNameKey()); }

    private Component hour() {
        long time = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getDayTime();
        int clock = (int) ((Math.floorMod(time, Ticks.DAY) / 1000L + 6L) % 24L);
        return Component.translatable(UI + "hour." + (clock + 1) / 2 % HOURS);
    }

    private int tabLeft(Page each) {
        return this.width - TAB_RIGHT - TAB_W - (Page.values().length - 1 - each.ordinal()) * TAB_PITCH;
    }

    private void drawTab(SpriteBatch sprites, Page each, int mouseX, int mouseY) {
        boolean selected = each == this.page;
        int left = tabLeft(each);
        if (selected) {
            sprites.blit(TAB_SELECTED, left, TAB_TOP, TAB_W, TAB_H);
        } else if (isOverTab(each, mouseX, mouseY)) {
            sprites.blit(TAB_HIGHLIGHTED, left, TAB_TOP, TAB_W, TAB_H);
        }
        sprites.blit(PanelDrawing.sprite(TAB_ICONS[each.ordinal()]), left + 5, TAB_TOP + 3, 16, 16,
                selected ? 0xFFFFFFFF : PanelDrawing.DIM_ICON);
    }

    private void drawTabLabel(GuiGraphics graphics, Page each, int mouseX, int mouseY) {
        boolean selected = each == this.page;
        int colour = selected ? PanelDrawing.ACCENT
                : isOverTab(each, mouseX, mouseY) ? PanelDrawing.TEXT : PanelDrawing.LEGEND;
        graphics.drawString(this.font, this.pages.get(each).name(), tabLeft(each) + 23, TAB_TOP + 7, colour, false);
    }

    private boolean isOverTab(Page each, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, tabLeft(each), TAB_TOP, TAB_W, TAB_H);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) { return false; }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        trackNotice();
        super.render(graphics, mouseX, mouseY, partialTick);
        PanelPage current = current();
        if (!current.hasOverlay()) {
            current.tooltip(graphics, this.leftPos, this.topPos, mouseX, mouseY);
            this.renderTooltip(graphics, mouseX, mouseY);
        }
        if (Util.getMillis() < this.noticeUntil) drawNotice(graphics);
        if (current.hasOverlay()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            current.overlay(graphics, this.leftPos, this.topPos, mouseX, mouseY);
            graphics.pose().popPose();
        }
    }

    private void drawNotice(GuiGraphics graphics) {
        int width = this.font.width(this.notice) + 2 * PanelDrawing.PAD;
        int left = (this.width - width) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        SpriteBatch.draw(graphics, sprites -> sprites.blit(PanelDrawing.CHIP, left, NOTICE_Y, width, 16));
        graphics.drawString(this.font, this.notice, left + PanelDrawing.PAD, NOTICE_Y + 4, PanelDrawing.TEXT, false);
        graphics.pose().popPose();
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
        PanelPage current = current();
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
        }
        if ((button == 0 || this.page == Page.MAP)
                && current.click(this.leftPos, this.topPos, mouseX, mouseY, button, Screen.hasShiftDown())) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        PanelPage current = current();
        if (!current.hasOverlay() && current.drag(this.leftPos, this.topPos, mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        PanelPage current = current();
        if (!current.hasOverlay() && current.release(this.leftPos, this.topPos, mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (current().scroll(this.leftPos, this.topPos, mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PanelPage current = current();
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
        if (!current.hasOverlay() && current.key(keyCode, scanCode, modifiers)) return true;
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
        private Pages(EarthSpiritMenu menu, Inventory inventory, Component title, Page page,
                      @Nullable Map<Page, PanelPage> carried) {
            super(menu, inventory, title, page, carried);
        }
    }

    /** 库藏页：背包格可见，存取靠它们，整理类模组照常挂件。 */
    public static final class Storage extends EarthSpiritScreen {
        private Storage(EarthSpiritMenu menu, Inventory inventory, Component title, Page page,
                        @Nullable Map<Page, PanelPage> carried) {
            super(menu, inventory, title, page, carried);
        }
    }
}
