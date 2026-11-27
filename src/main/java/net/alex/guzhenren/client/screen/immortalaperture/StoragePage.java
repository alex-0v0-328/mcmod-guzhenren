package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.StoragePart;
import net.alex.guzhenren.gameplay.immortalaperture.storage.StorageBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * 「库藏」页：服务端下发的当前一页条目铺成 9×{@value #ROWS} 的格子，滚轮翻页；格子下面一行是搜索框、排序、「存同类」；
 * 再下面是玩家背包与快捷栏（菜单里的真格子）。
 * <ul>
 *   <li>库藏不设上限，一个格子是一种物品，右下角画总数（过万缩写成 k / m），悬停提示补完整数量；物品数据过大只下发了
 *       种类的格子，悬停多一行说明。</li>
 *   <li>左键取一组到光标（光标有东西就进背包），Shift 左键取满背包；光标拿着东西点库藏格 = 整组存入。背包格 Shift 点也
 *       是整组存入，由菜单的 {@code quickMoveStack} 在服务端做。「存同类」把背包主格里库藏已有的同类一次存入。取物带条
 *       目流水号。</li>
 *   <li>搜索、排序、翻页都在服务端（{@link StorageBrowser}）：输入一变、点排序、滚轮翻页就发一次 {@link
 *       StoragePart.Browse}，带上按本地物品名算好的提示表（专用服务器认不出中文名）。翻页与排序先记成「待回」值，
 *       服务端回了同样的页码或排序才清掉，连续滚动与连点不丢步。</li>
 *   <li>右上角写种类数：筛选时写「匹配数 / 总数」，超过一页时补「当前页 / 页数」。</li>
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
    private int pendingPage = -1;
    private @Nullable StorageBrowser.Sort pendingSort;

    StoragePage(EarthSpiritScreen screen) { super(screen); }

    @Override
    protected Component name() { return Component.translatable(UI + "tab.storage"); }

    private StoragePart.Section section() {
        StoragePart.Section section = section(StoragePart.INSTANCE, StoragePart.Section.EMPTY);
        if (section.pageIndex() == pendingPage) pendingPage = -1;
        if (section.sort() == pendingSort) pendingSort = null;
        return section;
    }

    private StorageBrowser.Sort sort() { return pendingSort != null ? pendingSort : section().sort(); }

    private int pageIndex() { return pendingPage >= 0 ? pendingPage : section().pageIndex(); }

    void searchChanged() { browse(sort(), 0); }

    private void browse(StorageBrowser.Sort sort, int pageIndex) {
        pendingSort = sort;
        pendingPage = pageIndex;
        String text = screen.searchQuery();
        send(new StoragePart.Browse(text, sort, pageIndex, hints(text)));
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

    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                sprites.blit(PanelDrawing.SLOT, x + GRID_X + c * PITCH - 1, y + GRID_Y + r * PITCH - 1, 18, 18);
            }
        }
        sprites.blit(screen.isSearchFocused() ? PanelDrawing.TEXT_FIELD_FOCUSED : PanelDrawing.TEXT_FIELD, x + SEARCH_X,
                y + SEARCH_Y, SEARCH_W, PanelDrawing.SQUARE);
        PanelDrawing.button(sprites, x + SORT_X, y + SEARCH_Y, SORT_W, PanelDrawing.SQUARE, true, mouseX, mouseY);
        PanelDrawing.button(sprites, x + DEPOSIT_X, y + SEARCH_Y, DEPOSIT_W, PanelDrawing.SQUARE, true, mouseX, mouseY);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < COLS; c++) {
                sprites.blit(PanelDrawing.SLOT, x + EarthSpiritMenu.INVENTORY_X + c * PITCH - 1,
                        y + EarthSpiritMenu.INVENTORY_Y + r * PITCH - 1, 18, 18);
            }
        }
        for (int c = 0; c < COLS; c++) {
            sprites.blit(PanelDrawing.SLOT, x + EarthSpiritMenu.INVENTORY_X + c * PITCH - 1,
                    y + EarthSpiritMenu.HOTBAR_Y - 1, 18, 18);
        }
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        StoragePart.Section section = section();
        Component count = section.matched() < section.total()
                ? Component.translatable(UI + "kinds_filtered", section.matched(), section.total())
                : Component.translatable(UI + "kinds", section.total());
        Component kinds = section.pages() > 1
                ? Component.translatable(UI + "kinds_paged", count, section.pageIndex() + 1, section.pages()) : count;
        PanelDrawing.right(graphics, font(), kinds, x + 168, y + 6, PanelDrawing.LEGEND);
        PanelDrawing.label(graphics, font(), Component.translatable(UI + SORT_KEYS[sort().ordinal()]), x + SORT_X,
                y + SEARCH_Y, SORT_W, PanelDrawing.SQUARE, true);
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "deposit_matching"), x + DEPOSIT_X,
                y + SEARCH_Y, DEPOSIT_W, PanelDrawing.SQUARE, true);
        graphics.drawString(font(), Component.translatable(UI + "inventory"), x + 8, y + 128, PanelDrawing.TEXT, false);
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<StorageBrowser.Stored> entries = section().entries();
        for (int i = 0; i < Math.min(entries.size(), ROWS * COLS); i++) {
            StorageBrowser.Stored entry = entries.get(i);
            int left = x + GRID_X + (i % COLS) * PITCH;
            int top = y + GRID_Y + (i / COLS) * PITCH;
            graphics.renderItem(entry.key(), left, top);
            graphics.renderItemDecorations(font(), entry.key(), left, top, PanelDrawing.count(entry.count()));
        }
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (tip(graphics, x + SEARCH_X, y + SEARCH_Y, SEARCH_W, PanelDrawing.SQUARE, mouseX, mouseY,
                Component.translatable(TIP + "search"))) {
            return;
        }
        if (tip(graphics, x + SORT_X, y + SEARCH_Y, SORT_W, PanelDrawing.SQUARE, mouseX, mouseY,
                Component.translatable(TIP + "sort"))) {
            return;
        }
        if (tip(graphics, x + DEPOSIT_X, y + SEARCH_Y, DEPOSIT_W, PanelDrawing.SQUARE, mouseX, mouseY,
                Component.translatable(TIP + "deposit_matching"))) {
            return;
        }
        StorageBrowser.Stored entry = entryAt(x, y, mouseX, mouseY);
        if (entry == null || !screen.getMenu().getCarried().isEmpty()) return;

        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), entry.key()));
        lines.add(Component.translatable(UI + "stored", entry.count()).withColor(PanelDrawing.LEGEND));
        if (entry.trimmed()) lines.add(Component.translatable(UI + "trimmed").withColor(PanelDrawing.LEGEND));
        graphics.renderComponentTooltip(font(), lines, mouseX, mouseY);
    }

    private @Nullable StorageBrowser.Stored entryAt(int x, int y, double mouseX, double mouseY) {
        if (!Widgets.isOver(mouseX, mouseY, x + GRID_X, y + GRID_Y, COLS * PITCH, ROWS * PITCH)) return null;
        int c = (int) (mouseX - x - GRID_X) / PITCH;
        int r = (int) (mouseY - y - GRID_Y) / PITCH;
        List<StorageBrowser.Stored> entries = section().entries();
        int index = r * COLS + c;
        return index < entries.size() ? entries.get(index) : null;
    }

    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (Widgets.isOver(mouseX, mouseY, x + SORT_X, y + SEARCH_Y, SORT_W, PanelDrawing.SQUARE)) {
            browse(sort().next(), 0);
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + DEPOSIT_X, y + SEARCH_Y, DEPOSIT_W, PanelDrawing.SQUARE)) {
            send(new StoragePart.DepositMatching());
            return true;
        }
        if (!Widgets.isOver(mouseX, mouseY, x + GRID_X, y + GRID_Y, COLS * PITCH, ROWS * PITCH)) return false;
        if (!screen.getMenu().getCarried().isEmpty()) {
            send(new StoragePart.DepositCarried());
            return true;
        }
        StorageBrowser.Stored entry = entryAt(x, y, mouseX, mouseY);
        if (entry != null && button == 0) send(new StoragePart.Take(entry.serial(), shift));
        return true;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, x + GRID_X, y + GRID_Y, COLS * PITCH, ROWS * PITCH)) return false;
        int next = Math.clamp(pageIndex() - (int) Math.signum(delta), 0, section().pages() - 1);
        if (next != pageIndex()) browse(sort(), next);
        return true;
    }
}
