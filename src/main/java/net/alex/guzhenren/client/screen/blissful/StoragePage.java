package net.alex.guzhenren.client.screen.blissful;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.blissful.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.blissful.EarthSpiritPanel;
import net.alex.guzhenren.gameplay.blissful.PanelAction;
import net.alex.guzhenren.gameplay.blissful.PanelSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 「库藏」页：库藏条目铺成 9×6 的格子，滚轮按行翻；下方是玩家背包与快捷栏（菜单里的真格子）。
 * <ul>
 *   <li>库藏不设上限，一个格子是一种物品，右下角画总数（过万缩写成 k / m），悬停提示补完整数量。</li>
 *   <li>左键取一组到光标（光标有东西就进背包），Shift 左键取满背包；光标拿着东西点库藏格 = 整组存入。背包格 Shift 点也
 *       是整组存入，由菜单的 {@code quickMoveStack} 在服务端做。</li>
 *   <li>右上角写种类数，超过一屏时补当前行位置。</li>
 * </ul>
 */

final class StoragePage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;
    private static final int COLS = 9;
    private static final int ROWS = 6;
    private static final int PITCH = EarthSpiritMenu.SLOT;
    private int row;

    StoragePage(EarthSpiritScreen screen) { super(screen); }

    @Override
    Component name() { return Component.translatable(UI + "tab.storage"); }

    private List<PanelSnapshot.Stored> stored() { return snapshot().storage(); }

    private int totalRows() { return (stored().size() + COLS - 1) / COLS; }

    @Override
    void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        row = PanelKit.clampScroll(row, ROWS, totalRows());
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                sprites.blit(PanelKit.SLOT, x + GRID_X + c * PITCH - 1, y + GRID_Y + r * PITCH - 1, 18, 18);
            }
        }
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
        Component kinds = totalRows() > ROWS
                ? Component.translatable(UI + "kinds_paged", stored().size(), row + 1, totalRows() - ROWS + 1)
                : Component.translatable(UI + "kinds", stored().size());
        PanelKit.right(graphics, font(), kinds, x + 168, y + 6, PanelKit.LEGEND);
        graphics.drawString(font(), Component.translatable(UI + "inventory"), x + 8, y + 128, PanelKit.TEXT, false);
    }

    @Override
    void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<PanelSnapshot.Stored> stored = stored();
        for (int i = 0; i < ROWS * COLS; i++) {
            int index = row * COLS + i;
            if (index >= stored.size()) break;
            PanelSnapshot.Stored entry = stored.get(index);
            int left = x + GRID_X + (i % COLS) * PITCH;
            int top = y + GRID_Y + (i / COLS) * PITCH;
            graphics.renderItem(entry.key(), left, top);
            graphics.renderItemDecorations(font(), entry.key(), left, top, PanelKit.count(entry.count()));
        }
    }

    @Override
    void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int index = indexAt(x, y, mouseX, mouseY);
        if (index < 0 || !screen.getMenu().getCarried().isEmpty()) return;

        PanelSnapshot.Stored entry = stored().get(index);
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), entry.key()));
        lines.add(Component.translatable(UI + "stored", entry.count()).withColor(PanelKit.LEGEND));
        graphics.renderComponentTooltip(font(), lines, mouseX, mouseY);
    }

    private int indexAt(int x, int y, double mouseX, double mouseY) {
        if (!Widgets.isOver(mouseX, mouseY, x + GRID_X, y + GRID_Y, COLS * PITCH, ROWS * PITCH)) return -1;
        int c = (int) (mouseX - x - GRID_X) / PITCH;
        int r = (int) (mouseY - y - GRID_Y) / PITCH;
        int index = (row + r) * COLS + c;
        return index < stored().size() ? index : -1;
    }

    @Override
    boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (!Widgets.isOver(mouseX, mouseY, x + GRID_X, y + GRID_Y, COLS * PITCH, ROWS * PITCH)) return false;
        if (!screen.getMenu().getCarried().isEmpty()) {
            screen.send(PanelAction.DEPOSIT_CARRIED, 0, "");
            return true;
        }
        int index = indexAt(x, y, mouseX, mouseY);
        if (index >= 0 && button == 0) {
            screen.send(shift ? PanelAction.TAKE_ALL : PanelAction.TAKE_STACK, index,
                    EarthSpiritPanel.idOf(stored().get(index).key()));
        }
        return true;
    }

    @Override
    boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, x + GRID_X, y + GRID_Y, COLS * PITCH, ROWS * PITCH)) return false;
        row = PanelKit.clampScroll(row - (int) Math.signum(delta), ROWS, totalRows());
        return true;
    }
}
