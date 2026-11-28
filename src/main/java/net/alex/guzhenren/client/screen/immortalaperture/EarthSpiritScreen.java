package net.alex.guzhenren.client.screen.immortalaperture;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.map.MapPage;
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
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地灵[Earth Spirit]经营面板：四个 {@link Page}（地图、库藏、秘境、信息），各页由 {@link PanelPage} 子类绘制。本类是面板
 * 本身（页面、顶栏、两个输入框、提示与输入转发），由两个界面类承载：库藏页是容器界面 {@link Storage}，其余页是普通界面
 * {@link Pages}。
 * <ul>
 *   <li>面板开着时整帧不渲染世界（{@link #hidesWorld}，{@code GameRendererMixin} 跳过；启动参数
 *       {@code -D}{@value #KEEP_WORLD_PROPERTY}{@code =true} 退回照常渲染，供光影或性能模组冲突时排查）。四页都全屏（Alex
 *       定）：最上 {@value #BAR_H} 像素是顶栏（{@code top_bar} 平铺，含金线与投影；左写福地名（Alex 定不显示时辰），右侧并排四个页签，
 *       常态页签只有图标与灰字，悬停与选中才画底板），其余页在 {@link PanelDrawing#BACKDROP} 纯色底上排卡片；地图页整片是
 *       地图，顶栏投影压在地图上。页面原点在库藏页是两张卡片的左上角（即 {@code leftPos}/{@code topPos}，背包格按它摆），
 *       其余页为 0。</li>
 *   <li>秘境页的标签与页名写福地秘境的地名（{@link #realmName}）；信息页内部再分小节（{@link InfoPage}）。</li>
 *   <li>绘制顺序：一个 {@code GuiBatch} 里先一个 {@link SpriteBatch} 画全部精灵图，再填充、再文字；物品图标与头像在批
 *       外；顶栏另起一批画在页面之上；弹窗与操作提示（顶栏下居中的 {@code chip}）在界面自身绘制之后，盖在背包格与光标物品
 *       之上。</li>
 *   <li>数字键 1–4 切页；库藏页鼠标停在背包格上时数字键仍归原版（与快捷栏交换）。切页时发 {@link PanelActions.View}：服务端
 *       只算正在看的页，也记下这一页，下次开面板直接停在这里。</li>
 *   <li>跨库藏页切页就换一个界面实例，面板对象（页码、页面状态、输入框内容与提示）原样带过去：只有库藏页是容器界面，整理类与
 *       配方类模组（InventoryProfilesNext、JEI）只在库藏页挂件；其余页是普通界面，它们一律不挂（Alex 定）。{@link Pages}
 *       自己补齐容器界面的关面板（Esc 与物品栏键发关闭包）、不暂停与光标上拿着的物品。服务端菜单不受换界面影响；换实例不算关
 *       面板，不调页面的 {@code closed}。</li>
 *   <li>两个输入框：秘境页的白名单名字（灰字提示匹配的在线玩家名，Tab 补全，回车添加）、库藏页的搜索（内容一变就经
 *       {@link StoragePage} 发给服务端，筛选在服务端做）；每个界面实例重建，先填旧值后挂监听，不重发；聚焦时吞掉除 Esc 外的
 *       按键，免得按 E 关掉面板。</li>
 *   <li>全屏没有「面板外」（库藏页 {@code hasClickedOutside} 恒假）：拿着物品点空处不会扔出去。</li>
 *   <li>数据只来自菜单里按面板段缓存的快照段：开面板时随缓冲带来，之后每秒与每次操作后由 {@link EarthSpiritPanelPayload}
 *       替换（{@link #onPanel}）。操作结果提示在顶栏下方显示 {@value #NOTICE_MILLIS} 毫秒。</li>
 *   <li>本文件有意超过 300 行：四页切换与全屏布局、两处输入框、共享顶栏与输入转发及两个承载界面同属一个面板生命周期，集中
 *       在一处维护更清晰。</li>
 * </ul>
 */

public final class EarthSpiritScreen {

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
    private static final float CARRIED_Z = 232.0F;
    private static final ResourceLocation TOP_BAR = PanelDrawing.sprite("top_bar");
    private static final ResourceLocation TAB_HIGHLIGHTED = PanelDrawing.sprite("tab_highlighted");
    private static final ResourceLocation TAB_SELECTED = PanelDrawing.sprite("tab_selected");
    private static final boolean KEEPS_WORLD = Boolean.getBoolean(KEEP_WORLD_PROPERTY);
    private static final String[] TAB_ICONS = {"icon_map", "icon_storage", "icon_mountain", "icon_info"};
    private final EarthSpiritMenu menu;
    private final Inventory inventory;
    private final Component title;
    private final Map<Page, PanelPage> pages = new EnumMap<>(Page.class);
    private Page page;
    private Screen host;
    private int x;
    private int y;
    private @Nullable EditBox nameField;
    private @Nullable EditBox searchField;
    private @Nullable String completion;
    private Component notice = Component.empty();
    private long noticeUntil;
    private PanelSnapshot seen;
    private boolean swapping;

    private EarthSpiritScreen(EarthSpiritMenu menu, Inventory inventory, Component title) {
        this.menu = menu;
        this.inventory = inventory;
        this.title = title;
        this.page = menu.getSnapshot().page();
        this.seen = menu.getSnapshot();
        this.pages.put(Page.MAP, new MapPage(this));
        this.pages.put(Page.STORAGE, new StoragePage(this));
        this.pages.put(Page.SECRET_REALM, new SecretRealmPage(this));
        this.pages.put(Page.INFO, new InfoPage(this));
        this.host = newHost();
    }

    /** 菜单界面工厂：按上次停的页给出库藏页的容器界面或其余页的普通界面。 */
    @SuppressWarnings("unchecked")
    public static <S extends Screen & MenuAccess<EarthSpiritMenu>> MenuScreens.ScreenConstructor<EarthSpiritMenu, S>
            factory() {
        return (menu, inventory, title) -> (S) new EarthSpiritScreen(menu, inventory, title).host;
    }

    public static void onPanel(EarthSpiritPanelPayload payload) {
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.containerMenu instanceof EarthSpiritMenu menu) {
            menu.setSnapshot(payload.snapshot());
        }
    }

    //region Layout [布局]
    public static boolean hidesWorld() {
        Screen screen = Minecraft.getInstance().screen;
        return !KEEPS_WORLD && (screen instanceof Pages || screen instanceof Storage);
    }

    private Screen newHost() { return this.page == Page.STORAGE ? new Storage(this) : new Pages(this); }

    private List<EditBox> init(Screen host, int x, int y) {
        this.host = host;
        this.x = x;
        this.y = y;
        SecretRealmPage realm = (SecretRealmPage) this.pages.get(Page.SECRET_REALM);
        realm.resized(host.width, host.height);
        String name = this.nameField == null ? "" : this.nameField.getValue();
        EditBox names = field(realm.fieldX() + 4, realm.fieldY() + 3, realm.fieldW() - 8, SecretRealmPart.NAME_LENGTH,
                "name_hint", name);
        names.setResponder(this::suggestName);
        String query = this.searchField == null ? "" : this.searchField.getValue();
        EditBox search = field(x + StoragePage.SEARCH_X + 3, y + StoragePage.SEARCH_Y + 3, StoragePage.SEARCH_W - 6,
                StorageBrowser.TEXT_LENGTH, "search_hint", query);
        search.setResponder(text -> storage().searchChanged());
        this.nameField = names;
        this.searchField = search;
        suggestName(name);
        applyPage();
        current().resized(host.width, host.height);
        return List.of(names, search);
    }

    private EditBox field(int x, int y, int width, int length, String hint, String value) {
        EditBox box = new EditBox(font(), x, y, width, 10, Component.translatable(UI + hint));
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
        boolean swap = (next == Page.STORAGE) != (this.host instanceof Storage);
        this.page = next;
        if (swap) {
            this.swapping = true;
            Minecraft.getInstance().setScreen(newHost());
            return;
        }
        applyPage();
        current().resized(this.host.width, this.host.height);
    }

    private void applyPage() {
        EditBox names = names();
        EditBox search = search();
        names.setVisible(this.page == Page.SECRET_REALM);
        if (this.page != Page.SECRET_REALM) names.setFocused(false);
        search.setVisible(this.page == Page.STORAGE);
        if (this.page != Page.STORAGE) search.setFocused(false);
    }

    private PanelPage current() { return this.pages.get(this.page); }

    private StoragePage storage() { return (StoragePage) this.pages.get(Page.STORAGE); }

    private EditBox names() { return Objects.requireNonNull(this.nameField); }

    private EditBox search() { return Objects.requireNonNull(this.searchField); }

    private void removed() {
        if (this.swapping) {
            this.swapping = false;
            return;
        }
        for (PanelPage each : this.pages.values()) each.closed();
    }
    //endregion

    //region Page access [页面访问]
    EarthSpiritMenu getMenu() { return this.menu; }

    int width() { return this.host.width; }

    int height() { return this.host.height; }

    void openPage(Page next) { show(next); }

    /** 信息页「种植」点作物行：跳到地图页并选中种这种作物（{@code FarmRules.crops()} 下标）的田。 */
    void openMapSelecting(int crop) {
        ((MapPage) this.pages.get(Page.MAP)).selectCrop(crop);
        show(Page.MAP);
    }

    /** 信息页「养殖」点牧场行：跳到地图页并选中第 {@code index} 个牧场。 */
    void openMapPasture(int index) {
        ((MapPage) this.pages.get(Page.MAP)).selectPasture(index);
        show(Page.MAP);
    }

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

    Font font() { return Minecraft.getInstance().font; }

    boolean isNameFieldFocused() { return names().isFocused(); }

    boolean isSearchFocused() { return search().isFocused(); }

    String searchQuery() { return StorageBrowser.normalize(search().getValue()); }

    String takeNameField() {
        String value = names().getValue();
        names().setValue("");
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
        names().setSuggestion(this.completion == null ? null : this.completion.substring(typed.length()));
    }

    void addTypedName() {
        String name = takeNameField().strip();
        if (!name.isEmpty()) send(new SecretRealmPart.AddWhitelist(name));
    }
    //endregion

    //region Rendering [绘制]
    private void drawBackground(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.page != Page.MAP) graphics.fill(0, 0, width(), height(), PanelDrawing.BACKDROP);
        PanelPage current = current();
        int left = this.x;
        int top = this.y;
        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> current.sprites(sprites, left, top, mouseX, mouseY));
            current.fills(graphics, left, top, mouseX, mouseY);
            current.texts(graphics, left, top, mouseX, mouseY);
        });
        current.items(graphics, left, top, mouseX, mouseY);
        drawBar(graphics, mouseX, mouseY);
    }

    private void drawBar(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> {
                sprites.blit(TOP_BAR, 0, 0, width(), BAR_H);
                for (Page each : Page.values()) drawTab(sprites, each, mouseX, mouseY);
            });
            graphics.drawString(font(), landName(), 8, 8, PanelDrawing.TEXT, false);
            for (Page each : Page.values()) drawTabLabel(graphics, each, mouseX, mouseY);
        });
    }

    private MutableComponent landName() { return Component.translatable(this.menu.getSnapshot().landNameKey()); }

    private int tabLeft(Page each) {
        return width() - TAB_RIGHT - TAB_W - (Page.values().length - 1 - each.ordinal()) * TAB_PITCH;
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
        graphics.drawString(font(), this.pages.get(each).name(), tabLeft(each) + 23, TAB_TOP + 7, colour, false);
    }

    private boolean isOverTab(Page each, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, tabLeft(each), TAB_TOP, TAB_W, TAB_H);
    }

    /** 界面自身绘制之后：页面悬停提示与承载界面的提示（库藏页的背包格），再盖操作提示与弹窗。 */
    private void drawOver(GuiGraphics graphics, int mouseX, int mouseY, Runnable hostTooltip) {
        PanelPage current = current();
        if (!current.hasOverlay()) {
            current.tooltip(graphics, this.x, this.y, mouseX, mouseY);
            hostTooltip.run();
        }
        if (Util.getMillis() < this.noticeUntil) drawNotice(graphics);
        if (current.hasOverlay()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            current.overlay(graphics, this.x, this.y, mouseX, mouseY);
            graphics.pose().popPose();
        }
    }

    private void drawNotice(GuiGraphics graphics) {
        int width = font().width(this.notice) + 2 * PanelDrawing.PAD;
        int left = (width() - width) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        SpriteBatch.draw(graphics, sprites -> sprites.blit(PanelDrawing.CHIP, left, NOTICE_Y, width, 16));
        graphics.drawString(font(), this.notice, left + PanelDrawing.PAD, NOTICE_Y + 4, PanelDrawing.TEXT, false);
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
    private boolean mouseClicked(double mouseX, double mouseY, int button) {
        PanelPage current = current();
        if (current.hasOverlay()) {
            if (button == 0) current.overlayClick(this.x, this.y, mouseX, mouseY);
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
        return (button == 0 || this.page == Page.MAP)
                && current.click(this.x, this.y, mouseX, mouseY, button, Screen.hasShiftDown());
    }

    private boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        PanelPage current = current();
        return !current.hasOverlay() && current.drag(this.x, this.y, mouseX, mouseY, button, dragX, dragY);
    }

    private boolean mouseReleased(double mouseX, double mouseY, int button) {
        PanelPage current = current();
        return !current.hasOverlay() && current.release(this.x, this.y, mouseX, mouseY, button);
    }

    private boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return current().scroll(this.x, this.y, mouseX, mouseY, scrollY);
    }

    private boolean keyPressed(int keyCode, int scanCode, int modifiers, boolean overSlot) {
        PanelPage current = current();
        if (keyCode == InputConstants.KEY_ESCAPE && current.hasOverlay()) {
            current.closeOverlay();
            return true;
        }
        EditBox names = names();
        EditBox focused = names.isFocused() ? names : search().isFocused() ? search() : null;
        if (focused != null && keyCode != InputConstants.KEY_ESCAPE) {
            if (focused == names && keyCode == InputConstants.KEY_TAB && this.completion != null) {
                names.setValue(this.completion);
                return true;
            }
            if (focused == names && (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)) {
                addTypedName();
                return true;
            }
            focused.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (!current.hasOverlay() && current.key(keyCode, scanCode, modifiers)) return true;
        int digit = keyCode - InputConstants.KEY_1;
        if (overSlot || current.hasOverlay() || digit < 0 || digit >= Page.values().length) return false;
        show(Page.values()[digit]);
        return true;
    }
    //endregion

    /** 库藏以外的页：普通界面，整理类与配方类模组不挂件；自己补齐关面板、不暂停与光标上拿着的物品。 */
    public static final class Pages extends Screen implements MenuAccess<EarthSpiritMenu> {

        private final EarthSpiritScreen panel;

        private Pages(EarthSpiritScreen panel) {
            super(panel.title);
            this.panel = panel;
        }

        @Override
        public @NotNull EarthSpiritMenu getMenu() { return this.panel.menu; }

        @Override
        protected void init() { this.panel.init(this, 0, 0).forEach(this::addRenderableWidget); }

        @Override
        public boolean isPauseScreen() { return false; }

        @Override
        public void tick() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;
            if (player.isAlive() && !player.isRemoved()) {
                this.panel.current().tick();
            } else {
                player.closeContainer();
            }
        }

        @Override
        public void removed() { this.panel.removed(); }

        @Override
        public void onClose() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) player.closeContainer();
            super.onClose();
        }

        @Override
        public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.panel.drawBackground(graphics, mouseX, mouseY);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.panel.trackNotice();
            super.render(graphics, mouseX, mouseY, partialTick);
            drawCarried(graphics, mouseX, mouseY);
            this.panel.drawOver(graphics, mouseX, mouseY, () -> {});
        }

        private void drawCarried(GuiGraphics graphics, int mouseX, int mouseY) {
            ItemStack carried = this.panel.menu.getCarried();
            if (carried.isEmpty()) return;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, CARRIED_Z);
            graphics.renderItem(carried, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(this.font, carried, mouseX - 8, mouseY - 8);
            graphics.pose().popPose();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return this.panel.mouseDragged(mouseX, mouseY, button, dragX, dragY)
                    || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.panel.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            return this.panel.mouseScrolled(mouseX, mouseY, scrollY)
                    || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.panel.keyPressed(keyCode, scanCode, modifiers, false)
                    || super.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (!Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) return false;
            onClose();
            return true;
        }
    }

    /** 库藏页：容器界面，背包格可见，存取靠它们，整理类模组照常挂件。 */
    public static final class Storage extends AbstractContainerScreen<EarthSpiritMenu> {

        private final EarthSpiritScreen panel;

        private Storage(EarthSpiritScreen panel) {
            super(panel.menu, panel.inventory, panel.title);
            this.panel = panel;
        }

        @Override
        protected void init() {
            this.imageWidth = this.width;
            this.imageHeight = this.height;
            super.init();
            this.leftPos = StoragePage.left(this.width);
            this.topPos = StoragePage.TOP;
            this.panel.init(this, this.leftPos, this.topPos).forEach(this::addRenderableWidget);
        }

        @Override
        protected void containerTick() { this.panel.current().tick(); }

        @Override
        public void removed() {
            super.removed();
            this.panel.removed();
        }

        @Override
        public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.panel.drawBackground(graphics, mouseX, mouseY);
        }

        @Override
        protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}

        @Override
        protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {}

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.panel.trackNotice();
            super.render(graphics, mouseX, mouseY, partialTick);
            this.panel.drawOver(graphics, mouseX, mouseY, () -> this.renderTooltip(graphics, mouseX, mouseY));
        }

        @Override
        protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
            return false;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return this.panel.mouseDragged(mouseX, mouseY, button, dragX, dragY)
                    || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.panel.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            return this.panel.mouseScrolled(mouseX, mouseY, scrollY)
                    || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return this.panel.keyPressed(keyCode, scanCode, modifiers, this.hoveredSlot != null)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
