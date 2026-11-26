package net.alex.guzhenren.client.screen.blissful;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.blissful.BlissfulLands;
import net.alex.guzhenren.gameplay.blissful.gu.GuModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 「蛊虫」小节：福地里每种野生蛊一行（捕获后的蛊虫物品图标、名字、数量、托管开关、保持数 −/+），底部托管状态与「捕获
 * 入库」。
 * <ul>
 *   <li>同「养殖」小节：托管按种类开关，关着时 −/+ 失效但保持数照存；−/+ 每次改 1，按住 Shift 改 {@value #SHIFT_STEP}。</li>
 *   <li>福地暂不自然刷野生蛊，列表多半是空的，空时在列表里写一行说明。</li>
 *   <li>「捕获入库」悬停显示现在会捕几只，一只都不会捕时按钮失效。一屏 {@value #VISIBLE} 行，滚轮翻，右侧滚动条。</li>
 * </ul>
 */

final class GuPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final int SHIFT_STEP = 10;
    private static final int LIST_Y = 31;
    private static final int ROW_H = 20;
    private static final int ROW_W = 150;
    private static final int VISIBLE = 7;
    private static final int LIST_H = VISIBLE * ROW_H + 2;
    private static final int SWITCH_X = 92;
    private static final int MINUS_X = 114;
    private static final int PLUS_X = 144;
    private static final int STATUS_Y = 178;
    private static final int BUTTON_X = 8;
    private static final int BUTTON_Y = 192;
    private static final int BUTTON_W = 160;
    private static final int BUTTON_H = 20;
    private static final Component MINUS = Component.literal("-");
    private static final Component PLUS = Component.literal("+");
    private int offset;

    GuPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    protected Component name() { return Component.translatable(UI + "tab.gu"); }

    private GuModule.Section data() { return section(GuModule.INSTANCE, GuModule.Section.EMPTY); }

    private List<GuModule.Row> rows() { return data().rows(); }

    private static Component nameOf(GuModule.Row row) {
        return BuiltInRegistries.ENTITY_TYPE.get(row.type()).getDescription();
    }

    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        sprites.blit(PanelKit.WELL, x + 8, y + LIST_Y, 160, LIST_H);
        List<GuModule.Row> rows = rows();
        offset = PanelKit.clampScroll(offset, VISIBLE, rows.size());
        for (int i = 0; i < VISIBLE && offset + i < rows.size(); i++) {
            GuModule.Row row = rows.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            PanelKit.row(sprites, x + 9, top, ROW_W, ROW_H, Widgets.isOver(mouseX, mouseY, x + 9, top, ROW_W, ROW_H));
            PanelKit.toggle(sprites, x + SWITCH_X, top + 5, row.hosted(), true, mouseX, mouseY);
            PanelKit.button(sprites, x + MINUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE, row.hosted(), mouseX,
                    mouseY);
            PanelKit.button(sprites, x + PLUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE, row.hosted(), mouseX,
                    mouseY);
        }
        boolean over = Widgets.isOver(mouseX, mouseY, x + 160, y + 32, 6, LIST_H - 2);
        PanelKit.scrollbar(sprites, x + 160, y + 32, LIST_H - 2, offset, VISIBLE, rows.size(), over);
        PanelKit.button(sprites, x + BUTTON_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, data().capturePreview() > 0,
                mouseX, mouseY);
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        GuModule.Section data = data();
        graphics.drawString(font(), Component.translatable(UI + "gu"), x + 12, y + 21, PanelKit.LEGEND, false);
        PanelKit.right(graphics, font(), Component.translatable(UI + "count"), x + 84, y + 21, PanelKit.LEGEND);
        graphics.drawString(font(), Component.translatable(UI + "hosting"), x + 92, y + 21, PanelKit.LEGEND, false);
        PanelKit.centered(graphics, font(), Component.translatable(UI + "keep"), x + 114, x + 157, y + 21,
                PanelKit.LEGEND);
        List<GuModule.Row> rows = data.rows();
        if (rows.isEmpty()) {
            PanelKit.centered(graphics, font(), Component.translatable(UI + "gu_empty"), x + 8, x + 168,
                    y + LIST_Y + LIST_H / 2 - 4, PanelKit.DEAD);
        }
        for (int i = 0; i < VISIBLE && offset + i < rows.size(); i++) {
            GuModule.Row row = rows.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            graphics.drawString(font(), nameOf(row), x + 30, top + 6, PanelKit.TEXT, false);
            PanelKit.right(graphics, font(), Integer.toString(row.count()), x + 84, top + 6, PanelKit.TEXT);
            PanelKit.label(graphics, font(), MINUS, x + MINUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE,
                    row.hosted());
            PanelKit.label(graphics, font(), PLUS, x + PLUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE,
                    row.hosted());
            PanelKit.centered(graphics, font(), Component.literal(Integer.toString(row.keep())), x + 128, x + 143,
                    top + 6, row.hosted() ? PanelKit.TEXT : PanelKit.DEAD);
        }
        Component status = data.hosting()
                ? Component.translatable(UI + "gu_status", data.hostingIn(), data.lastCaptured())
                : Component.translatable(UI + "gu_note");
        graphics.drawString(font(), status, x + 8, y + STATUS_Y, PanelKit.LEGEND, false);
        PanelKit.label(graphics, font(), Component.translatable(UI + "capture"), x + BUTTON_X, y + BUTTON_Y,
                BUTTON_W, BUTTON_H, data.capturePreview() > 0);
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<GuModule.Row> rows = rows();
        for (int i = 0; i < VISIBLE && offset + i < rows.size(); i++) {
            Item item = BuiltInRegistries.ITEM.get(rows.get(offset + i).item());
            if (item != Items.AIR) graphics.renderItem(new ItemStack(item), x + 11, y + LIST_Y + 1 + i * ROW_H + 2);
        }
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        GuModule.Section data = data();
        if (tip(graphics, x + BUTTON_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "capture", data.capturePreview()))) {
            return;
        }
        if (tip(graphics, x + 8, y + STATUS_Y - 2, 160, 12, mouseX, mouseY, Component.translatable(TIP + "gu_hosting",
                BlissfulLands.HOSTING_TICKS / Ticks.SECOND, GuModule.HOSTING_GU))) {
            return;
        }
        List<GuModule.Row> rows = data.rows();
        for (int i = 0; i < VISIBLE && offset + i < rows.size(); i++) {
            int top = y + LIST_Y + 1 + i * ROW_H;
            if (tip(graphics, x + SWITCH_X, top + 5, PanelKit.SWITCH_W, PanelKit.SWITCH_H, mouseX, mouseY,
                    Component.translatable(TIP + "gu_switch"))) {
                return;
            }
            if (tip(graphics, x + MINUS_X, top + 3, PLUS_X + PanelKit.SQUARE - MINUS_X, PanelKit.SQUARE, mouseX, mouseY,
                    Component.translatable(TIP + "keep", SHIFT_STEP))) {
                return;
            }
        }
    }

    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (Widgets.isOver(mouseX, mouseY, x + BUTTON_X, y + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            if (data().capturePreview() > 0) send(new GuModule.Capture());
            return true;
        }
        List<GuModule.Row> rows = rows();
        int step = shift ? SHIFT_STEP : 1;
        for (int i = 0; i < VISIBLE && offset + i < rows.size(); i++) {
            GuModule.Row row = rows.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            if (Widgets.isOver(mouseX, mouseY, x + SWITCH_X, top + 5, PanelKit.SWITCH_W, PanelKit.SWITCH_H)) {
                send(new GuModule.SetHosting(row.type(), !row.hosted()));
                return true;
            }
            if (!row.hosted()) continue;
            if (Widgets.isOver(mouseX, mouseY, x + MINUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE)) {
                send(new GuModule.SetKeep(row.type(), Math.max(0, row.keep() - step)));
                return true;
            }
            if (Widgets.isOver(mouseX, mouseY, x + PLUS_X, top + 3, PanelKit.SQUARE, PanelKit.SQUARE)) {
                send(new GuModule.SetKeep(row.type(), row.keep() + step));
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, x + 8, y + LIST_Y, 160, LIST_H)) return false;
        offset = PanelKit.clampScroll(offset - (int) Math.signum(delta), VISIBLE, rows().size());
        return true;
    }
}
