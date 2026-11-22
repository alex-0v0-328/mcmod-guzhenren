package net.alex.guzhenren.client.screen.blissful;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.blissful.storage.StorageModule;
import net.alex.guzhenren.gameplay.blissful.storage.StorageWork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 「库藏」页：库藏条目铺成 9×{@value #ROWS} 的格子，滚轮按行翻；格子下面一行是搜索框、排序、「存同类」；再下面是玩家背包
 * 与快捷栏（菜单里的真格子）。
 * <ul>
 *   <li>库藏不设上限，一个格子是一种物品，右下角画总数（过万缩写成 k / m），悬停提示补完整数量。</li>
 *   <li>左键取一组到光标（光标有东西就进背包），Shift 左键取满背包；光标拿着东西点库藏格 = 整组存入。背包格 Shift 点也
 *       是整组存入，由菜单的 {@code quickMoveStack} 在服务端做。「存同类」把背包主格里库藏已有的同类一次存入。</li>
 *   <li>搜索按物品名或 ID 包含输入（不分大小写）；排序在入库顺序、数量（多的在前）、名称（按中文排序规则）之间循环，
 *       排序方式在本次游戏里记着。筛选排序只改显示顺序，取物仍带条目的原下标与物品 ID。</li>
 *   <li>右上角写种类数：筛选时写「显示数 / 总数」，超过一屏时补当前行位置。</li>
 * </ul>
 */

final class StoragePage extends PanelPage {

    static final int SEARCH_X = 8;
    static final int SEARCH_Y = 111;
    static final int SEARCH_W = 96;
    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;
    private static final int COLS = 9;
    private static final int ROWS = 5;
    private static final int PITCH = EarthSpiritMenu.SLOT;
    private static final int SORT_X = 106;
    private static final int SORT_W = 28;
    private static final int DEPOSIT_X = 136;
    private static final int DEPOSIT_W = 32;
    private static final String[] SORT_KEYS = {"sort_order", "sort_count", "sort_name"};
    private static int sort;
    private int row;

    StoragePage(EarthSpiritScreen screen) { super(screen); }

    @Override
    Component name() { return Component.translatable(UI + "tab.storage"); }

    private List<StorageModule.Stored> stored() { return section(StorageModule.Section.EMPTY).entries(); }

    private List<Integer> view() {
        List<StorageModule.Stored> stored = stored();
        String query = screen.searchQuery();
        List<Integer> view = new ArrayList<>();
        for (int i = 0; i < stored.size(); i++) {
            if (query.isEmpty() || matches(stored.get(i), query)) view.add(i);
        }
        if (sort == 1) view.sort(Comparator.comparingLong(i -> -stored.get(i).count()));
        if (sort == 2) {
            Collator collator = Collator.getInstance(Locale.CHINA);
            view.sort(Comparator.comparing(i -> stored.get(i).key().getHoverName().getString(), collator));
        }
        return view;
    }

    private static boolean matches(StorageModule.Stored entry, String query) {
        return entry.key().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                || StorageWork.idOf(entry.key()).contains(query);
    }

    private static int totalRows(int size) { return (size + COLS - 1) / COLS; }

    @Override
    void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        row = PanelKit.clampScroll(row, ROWS, totalRows(view().size()));
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                sprites.blit(PanelKit.SLOT, x + GRID_X + c * PITCH - 1, y + GRID_Y + r * PITCH - 1, 18, 18);
            }
        }
        sprites.blit(screen.isSearchFocused() ? PanelKit.TEXT_FIELD_FOCUSED : PanelKit.TEXT_FIELD, x + SEARCH_X,
                y + SEARCH_Y, SEARCH_W, PanelKit.SQUARE);
        PanelKit.button(sprites, x + SORT_X, y + SEARCH_Y, SORT_W, PanelKit.SQUARE, true, mouseX, mouseY);
        PanelKit.button(sprites, x + DEPOSIT_X, y + SEARCH_Y, DEPOSIT_W, PanelKit.SQUARE, true, mouseX, mouseY);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < COLS; c++) {
                sprites.blit(PanelKit.SLOT, x + EarthSpiritMenu.INVENTORY_X + c * PITCH - 1,
                        y + EarthSpiritMenu.INVENTORY_Y + r * PITCH - 1, 18, 18);
            }
        }
        for (int c = 0; c < COLS; c++) {
            sprites.blit(PanelKit.SLOT, x + EarthSpiritMenu.INVENTORY_X + c * PITCH - 1,
                    y + EarthSpiritMenu.HOTBAR_Y - 1, 18, 18);
        }
    }

    @Override
    void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int shown = view().size();
        int total = stored().size();
        Component count = shown < total ? Component.translatable(UI + "kinds_filtered", shown, total)
                : Component.translatable(UI + "kinds", total);
        Component kinds = totalRows(shown) > ROWS
                ? Component.translatable(UI + "kinds_paged", count, row + 1, totalRows(shown) - ROWS + 1) : count;
        PanelKit.right(graphics, font(), kinds, x + 168, y + 6, PanelKit.LEGEND);
        PanelKit.label(graphics, font(), Component.translatable(UI + SORT_KEYS[sort]), x + SORT_X, y + SEARCH_Y, SORT_W,
                PanelKit.SQUARE, true);
        PanelKit.label(graphics, font(), Component.translatable(UI + "deposit_matching"), x + DEPOSIT_X, y + SEARCH_Y,
                DEPOSIT_W, PanelKit.SQUARE, true);
        graphics.drawString(font(), Component.translatable(UI + "inventory"), x + 8, y + 128, PanelKit.TEXT, false);
    }

    @Override
    void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<StorageModule.Stored> stored = stored();
        List<Integer> view = view();
        for (int i = 0; i < ROWS * COLS; i++) {
            int index = row * COLS + i;
            if (index >= view.size()) break;
            StorageModule.Stored entry = stored.get(view.get(index));
            int left = x + GRID_X + (i % COLS) * PITCH;
            int top = y + GRID_Y + (i / COLS) * PITCH;
            graphics.renderItem(entry.key(), left, top);
            graphics.renderItemDecorations(font(), entry.key(), left, top, PanelKit.count(entry.count()));
        }
    }

    @Override
    void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (tip(graphics, x + SEARCH_X, y + SEARCH_Y, SEARCH_W, PanelKit.SQUARE, mouseX, mouseY,
                Component.translatable(TIP + "search"))) {
            return;
        }
        if (tip(graphics, x + SORT_X, y + SEARCH_Y, SORT_W, PanelKit.SQUARE, mouseX, mouseY,
                Component.translatable(TIP + "sort"))) {
            return;
        }
        if (tip(graphics, x + DEPOSIT_X, y + SEARCH_Y, DEPOSIT_W, PanelKit.SQUARE, mouseX, mouseY,
                Component.translatable(TIP + "deposit_matching"))) {
            return;
        }
        int index = indexAt(x, y, mouseX, mouseY);
        if (index < 0 || !screen.getMenu().getCarried().isEmpty()) return;

        StorageModule.Stored entry = stored().get(index);
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), entry.key()));
        lines.add(Component.translatable(UI + "stored", entry.count()).withColor(PanelKit.LEGEND));
        graphics.renderComponentTooltip(font(), lines, mouseX, mouseY);
    }

    private int indexAt(int x, int y, double mouseX, double mouseY) {
        if (!Widgets.isOver(mouseX, mouseY, x + GRID_X, y + GRID_Y, COLS * PITCH, ROWS * PITCH)) return -1;
        int c = (int) (mouseX - x - GRID_X) / PITCH;
        int r = (int) (mouseY - y - GRID_Y) / PITCH;
        int index = (row + r) * COLS + c;
        List<Integer> view = view();
        return index < view.size() ? view.get(index) : -1;
    }

    @Override
    boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (Widgets.isOver(mouseX, mouseY, x + SORT_X, y + SEARCH_Y, SORT_W, PanelKit.SQUARE)) {
            sort = (sort + 1) % SORT_KEYS.length;
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + DEPOSIT_X, y + SEARCH_Y, DEPOSIT_W, PanelKit.SQUARE)) {
            send(new StorageModule.DepositMatching());
            return true;
        }
        if (!Widgets.isOver(mouseX, mouseY, x + GRID_X, y + GRID_Y, COLS * PITCH, ROWS * PITCH)) return false;
        if (!screen.getMenu().getCarried().isEmpty()) {
            send(new StorageModule.DepositCarried());
            return true;
        }
        int index = indexAt(x, y, mouseX, mouseY);
        if (index >= 0 && button == 0) {
            send(new StorageModule.Take(index, StorageWork.idOf(stored().get(index).key()), shift));
        }
        return true;
    }

    @Override
    boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, x + GRID_X, y + GRID_Y, COLS * PITCH, ROWS * PITCH)) return false;
        row = PanelKit.clampScroll(row - (int) Math.signum(delta), ROWS, totalRows(view().size()));
        return true;
    }
}
