package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.StoragePart;
import net.alex.guzhenren.gameplay.immortalaperture.storage.StorageBrowser;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 「库藏」页：顶栏下居中两张卡片，左「库藏」卡（Alex 定改列表）：最上一条元石栏，下面搜索框、排序、「存同类」，再下是服务端
 * 下发的条目列表（一行一种：图标、名字、数量，滚轮滚一行）；右「背包」卡是玩家背包与快捷栏（菜单里的真格子，坐标相对本页
 * 左上角，见 {@code EarthSpiritMenu#INVENTORY_X}）。
 * <ul>
 *   <li>元石栏：左写库藏元石数，右写背包里那只已炼化元老蛊（物品名、已存 / 容量）与「装入」「倒出」；背包里没有就只写一行
 *       灰字。挑哪只、怎么搬在服务端 {@code StorageWork#vault}。元石照样也在列表里。</li>
 *   <li>库藏不设上限，悬停一行补完整数量；物品数据过大只下发了种类的行，悬停多一行说明。</li>
 *   <li>左键取一组到光标（光标有东西就进背包），Shift 左键取满背包；光标拿着东西点列表 = 整组存入。背包格 Shift 点也
 *       是整组存入，由菜单的 {@code quickMoveStack} 在服务端做。「存同类」把背包主格里库藏已有的同类一次存入。取物带条
 *       目流水号。</li>
 *   <li>搜索、排序、滚动都在服务端（{@link StorageBrowser}）：输入一变、点排序、滚一行就发一次 {@link StoragePart.Browse}，
 *       带上按本地物品名算好的提示表（专用服务器认不出中文名）。起始行与排序先记成「待回」值，服务端回了同样的才清掉，连续
 *       滚动与连点不丢步；回来之前列表按手上那窗条目先挪，窗外的行留空。</li>
 *   <li>列表行数随窗口高（{@value #MIN_ROWS} 到 {@value #MAX_ROWS} 行，不超过服务端一窗 {@value StorageBrowser#PAGE_SIZE}
 *       条）。</li>
 * </ul>
 */

final class StoragePage extends PanelPage {

    static final int TOP = EarthSpiritScreen.BAR_H + 7;
    static final int SEARCH_X = 5;
    static final int SEARCH_Y = 48;
    static final int SEARCH_W = 166;
    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final int LIST_W = 250;
    private static final int BAG_W = 172;
    private static final int BAG_X = LIST_W + PanelDrawing.GAP;
    private static final int BAG_H = EarthSpiritMenu.HOTBAR_Y + 22;
    private static final int WIDTH = BAG_X + BAG_W;
    private static final int STRIP_Y = 18;
    private static final int STRIP_H = 26;
    private static final int LIST_Y = SEARCH_Y + 20;
    private static final int ROW_H = 16;
    private static final int MIN_ROWS = 5;
    private static final int MAX_ROWS = 20;
    private static final int SORT_X = LIST_W - 77;
    private static final int SORT_W = 34;
    private static final int DEPOSIT_X = LIST_W - 41;
    private static final int DEPOSIT_W = 36;
    private static final int VAULT_X = 76;
    private static final int VAULT_KEY_W = 36;
    private static final int STRIPE = 0x30000000;
    private static final String[] SORT_KEYS = {"sort_order", "count", "sort_name"};
    private int rows = MIN_ROWS;
    private int pendingFirst = -1;
    private @Nullable StorageBrowser.Sort pendingSort;

    StoragePage(EarthSpiritScreen screen) { super(screen); }

    static int left(int screenWidth) { return (screenWidth - WIDTH) / 2; }

    @Override
    protected Component name() { return Component.translatable(UI + "tab.storage"); }

    @Override
    protected void resized(int width, int height) {
        rows = Math.clamp((height - TOP - LIST_Y - 2 - Widgets.PAD - PanelDrawing.MARGIN) / ROW_H, MIN_ROWS,
                MAX_ROWS);
    }

    private StoragePart.Section section() {
        StoragePart.Section section = section(StoragePart.INSTANCE, StoragePart.Section.EMPTY);
        if (section.first() == pendingFirst) pendingFirst = -1;
        if (section.sort() == pendingSort) pendingSort = null;
        return section;
    }

    private StorageBrowser.Sort sort() { return pendingSort != null ? pendingSort : section().sort(); }

    private int first() { return pendingFirst >= 0 ? pendingFirst : section().first(); }

    void searchChanged() { browse(sort(), 0); }

    private void browse(StorageBrowser.Sort sort, int first) {
        pendingSort = sort;
        pendingFirst = first;
        String text = screen.searchQuery();
        send(new StoragePart.Browse(text, sort, first, hints(text)));
    }

    private static List<Item> hints(String text) {
        List<Item> hints = new ArrayList<>();
        if (text.isEmpty()) return hints;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item.getDescription().getString().toLowerCase(Locale.ROOT).contains(text)) {
                hints.add(item);
                if (hints.size() == StorageBrowser.HINT_LIMIT) break;
            }
        }
        return hints;
    }

    //region Geometry [几何]
    private int cardH() { return LIST_Y + rows * ROW_H + 2 + Widgets.PAD; }

    private boolean scrolls() { return section().matched() > rows; }

    private int rowW() { return LIST_W - 12 - (scrolls() ? 8 : 0); }

    private int rowTop(int y, int row) { return y + LIST_Y + 1 + row * ROW_H; }

    private int fillX(int x) { return x + LIST_W - 5 - 2 * VAULT_KEY_W - 4; }

    private int emptyX(int x) { return x + LIST_W - 5 - VAULT_KEY_W - 2; }

    private int keyY(int y) { return y + STRIP_Y + 5; }

    private boolean isOverList(int x, int y, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, x + 5, y + LIST_Y, LIST_W - 10, rows * ROW_H + 2);
    }

    /** 第 {@code row} 行显示的条目：按手上那一窗换算，窗外为空。 */
    private @Nullable StorageBrowser.Stored entry(int row) {
        StoragePart.Section section = section();
        int index = first() - section.first() + row;
        return index >= 0 && index < section.entries().size() && first() + row < section.matched()
                ? section.entries().get(index) : null;
    }

    private int rowAt(int x, int y, double mouseX, double mouseY) {
        for (int row = 0; row < rows; row++) {
            if (Widgets.isOver(mouseX, mouseY, x + 6, rowTop(y, row), rowW(), ROW_H)) return row;
        }
        return -1;
    }
    //endregion

    //region Vault [元石栏]
    private boolean fillLive() {
        StoragePart.Section section = section();
        return section.vault().map(vault -> vault.stored() < vault.capacity() && section.stones() > 0).orElse(false);
    }

    private boolean emptyLive() { return section().vault().map(vault -> vault.stored() > 0).orElse(false); }

    private long fillAmount() {
        StoragePart.Section section = section();
        return section.vault().map(vault -> Math.min(vault.capacity() - vault.stored(), section.stones())).orElse(0L);
    }
    //endregion

    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        Widgets.card(sprites, x, y, LIST_W, cardH());
        Widgets.card(sprites, x + BAG_X, y, BAG_W, BAG_H);
        sprites.blit(Widgets.STAT, x + 5, y + STRIP_Y, LIST_W - 10, STRIP_H);
        if (section().vault().isPresent()) {
            Widgets.button(sprites, fillX(x), keyY(y), VAULT_KEY_W, Widgets.BUTTON_H, fillLive(), mouseX,
                    mouseY);
            Widgets.button(sprites, emptyX(x), keyY(y), VAULT_KEY_W, Widgets.BUTTON_H, emptyLive(), mouseX,
                    mouseY);
        }
        sprites.blit(screen.isSearchFocused() ? Widgets.TEXT_FIELD_FOCUSED : Widgets.TEXT_FIELD, x + SEARCH_X,
                y + SEARCH_Y, SEARCH_W, Widgets.BUTTON_H);
        Widgets.button(sprites, x + SORT_X, y + SEARCH_Y, SORT_W, Widgets.BUTTON_H, true, mouseX, mouseY);
        Widgets.button(sprites, x + DEPOSIT_X, y + SEARCH_Y, DEPOSIT_W, Widgets.BUTTON_H, true, mouseX,
                mouseY);
        sprites.blit(Widgets.INSET, x + 5, y + LIST_Y, LIST_W - 10, rows * ROW_H + 2);
        int hovered = rowAt(x, y, mouseX, mouseY);
        for (int row = 0; row < rows; row++) {
            if (entry(row) != null && row == hovered) Widgets.row(sprites, x + 6, rowTop(y, row), rowW(), ROW_H, true);
        }
        if (scrolls()) {
            Widgets.scrollbar(sprites, x + LIST_W - 12, y + LIST_Y + 1, rows * ROW_H, first(), rows,
                    section().matched(), false);
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                sprites.blit(Widgets.SLOT, x + EarthSpiritMenu.INVENTORY_X + c * EarthSpiritMenu.SLOT - 1,
                        y + EarthSpiritMenu.INVENTORY_Y + r * EarthSpiritMenu.SLOT - 1, 18, 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            sprites.blit(Widgets.SLOT, x + EarthSpiritMenu.INVENTORY_X + c * EarthSpiritMenu.SLOT - 1,
                    y + EarthSpiritMenu.HOTBAR_Y - 1, 18, 18);
        }
    }

    @Override
    protected void fills(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        for (int row = 1; row < rows; row += 2) {
            if (entry(row) != null) graphics.fill(x + 6, rowTop(y, row), x + 6 + rowW(), rowTop(y, row) + ROW_H, STRIPE);
        }
        graphics.fill(x + VAULT_X - 4, y + STRIP_Y + 4, x + VAULT_X - 3, y + STRIP_Y + STRIP_H - 4, PanelDrawing.DIM_ICON);
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        StoragePart.Section section = section();
        Component kinds = section.matched() < section.total()
                ? Component.translatable(UI + "kinds_filtered", section.matched(), section.total())
                : Component.translatable(UI + "kinds", section.total());
        Widgets.title(graphics, font(), name(), kinds, x, y, LIST_W);
        graphics.drawString(font(), Component.translatable(UI + "stones"), x + 26, y + STRIP_Y + 4, ModPalette.LEGEND_TEXT,
                false);
        graphics.drawString(font(), String.format(Locale.ROOT, "%,d", section.stones()), x + 26, y + STRIP_Y + 14,
                ModPalette.TEXT, false);
        section.vault().ifPresentOrElse(vault -> {
            Component name = BuiltInRegistries.ITEM.get(vault.item()).getDescription();
            graphics.drawString(font(), name, x + VAULT_X + 20, y + STRIP_Y + 4, ModPalette.LEGEND_TEXT, false);
            graphics.drawString(font(), String.format(Locale.ROOT, "%,d / %,d", vault.stored(), vault.capacity()),
                    x + VAULT_X + 20, y + STRIP_Y + 14, ModPalette.TEXT, false);
            Widgets.label(graphics, font(), Component.translatable(UI + "vault_fill"), fillX(x), keyY(y),
                    VAULT_KEY_W, Widgets.BUTTON_H, fillLive());
            Widgets.label(graphics, font(), Component.translatable(UI + "vault_empty"), emptyX(x), keyY(y),
                    VAULT_KEY_W, Widgets.BUTTON_H, emptyLive());
        }, () -> graphics.drawString(font(), Component.translatable(UI + "no_vault"), x + VAULT_X, y + STRIP_Y + 9,
                ModPalette.TEXT_DEAD, false));
        Widgets.label(graphics, font(), Component.translatable(UI + SORT_KEYS[sort().ordinal()]), x + SORT_X,
                y + SEARCH_Y, SORT_W, Widgets.BUTTON_H, true);
        Widgets.label(graphics, font(), Component.translatable(UI + "deposit_matching"), x + DEPOSIT_X,
                y + SEARCH_Y, DEPOSIT_W, Widgets.BUTTON_H, true);
        for (int row = 0; row < rows; row++) {
            StorageBrowser.Stored entry = entry(row);
            if (entry == null) continue;
            int top = rowTop(y, row);
            graphics.drawString(font(), entry.key().getHoverName(), x + 26, top + 4, ModPalette.TEXT, false);
            Widgets.right(graphics, font(), String.format(Locale.ROOT, "%,d", entry.count()), x + 6 + rowW() - 4,
                    top + 4, ModPalette.TEXT);
        }
        Widgets.title(graphics, font(), Component.translatable(UI + "inventory"), Component.empty(), x + BAG_X, y,
                BAG_W);
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        graphics.renderItem(new ItemStack(ModItems.PRIMEVAL_STONE.get()), x + 8, y + STRIP_Y + 5);
        section().vault().ifPresent(vault -> graphics.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(vault.item())),
                x + VAULT_X, y + STRIP_Y + 5));
        for (int row = 0; row < rows; row++) {
            StorageBrowser.Stored entry = entry(row);
            if (entry != null) graphics.renderItem(entry.key(), x + 7, rowTop(y, row));
        }
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        StoragePart.Section section = section();
        if (section.vault().isPresent()) {
            if (tip(graphics, fillX(x), keyY(y), VAULT_KEY_W, Widgets.BUTTON_H, mouseX, mouseY,
                    Component.translatable(TIP + "vault_fill", fillAmount(), section.stones() - fillAmount()))) {
                return;
            }
            if (tip(graphics, emptyX(x), keyY(y), VAULT_KEY_W, Widgets.BUTTON_H, mouseX, mouseY,
                    Component.translatable(TIP + "vault_empty", section.vault().get().stored()))) {
                return;
            }
        }
        if (tip(graphics, x + SEARCH_X, y + SEARCH_Y, SEARCH_W, Widgets.BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "search"))) {
            return;
        }
        if (tip(graphics, x + SORT_X, y + SEARCH_Y, SORT_W, Widgets.BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "sort"))) {
            return;
        }
        if (tip(graphics, x + DEPOSIT_X, y + SEARCH_Y, DEPOSIT_W, Widgets.BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "deposit_matching"))) {
            return;
        }
        int row = rowAt(x, y, mouseX, mouseY);
        StorageBrowser.Stored entry = row < 0 ? null : entry(row);
        if (entry == null || !screen.getMenu().getCarried().isEmpty()) return;

        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), entry.key()));
        lines.add(Component.translatable(UI + "stored", entry.count()).withColor(ModPalette.LEGEND_TEXT));
        if (entry.trimmed()) lines.add(Component.translatable(UI + "trimmed").withColor(ModPalette.LEGEND_TEXT));
        graphics.renderComponentTooltip(font(), lines, mouseX, mouseY);
    }

    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (section().vault().isPresent()) {
            if (Widgets.isOver(mouseX, mouseY, fillX(x), keyY(y), VAULT_KEY_W, Widgets.BUTTON_H)) {
                if (fillLive()) send(new StoragePart.FillVault());
                return true;
            }
            if (Widgets.isOver(mouseX, mouseY, emptyX(x), keyY(y), VAULT_KEY_W, Widgets.BUTTON_H)) {
                if (emptyLive()) send(new StoragePart.EmptyVault());
                return true;
            }
        }
        if (Widgets.isOver(mouseX, mouseY, x + SORT_X, y + SEARCH_Y, SORT_W, Widgets.BUTTON_H)) {
            browse(sort().next(), 0);
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + DEPOSIT_X, y + SEARCH_Y, DEPOSIT_W, Widgets.BUTTON_H)) {
            send(new StoragePart.DepositMatching());
            return true;
        }
        if (!isOverList(x, y, mouseX, mouseY)) return false;
        if (!screen.getMenu().getCarried().isEmpty()) {
            send(new StoragePart.DepositCarried());
            return true;
        }
        int row = rowAt(x, y, mouseX, mouseY);
        StorageBrowser.Stored entry = row < 0 ? null : entry(row);
        if (entry != null && button == 0) send(new StoragePart.Take(entry.serial(), shift));
        return true;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!isOverList(x, y, mouseX, mouseY)) return false;
        int next = Math.clamp(first() - (int) Math.signum(delta), 0, Math.max(0, section().matched() - rows));
        if (next != first()) browse(sort(), next);
        return true;
    }
}
