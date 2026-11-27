package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.gu.GuRules;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.GuPart;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandHostingJob;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 「蛊虫」小节：一张 {@link KeepList} 卡，福地里每种野生蛊一行（捕获后的蛊虫物品图标、名字、数量、托管开关、保持数
 * −/+），卡底托管状态与「捕获入库」。
 * <ul>
 *   <li>同「养殖」小节：托管按种类开关，关着时 −/+ 失效但保持数照存；−/+ 每次改 1，按住 Shift 改 {@value #SHIFT_STEP}。
 *       托管说明在「托管」表头的悬停提示。</li>
 *   <li>福地暂不自然刷野生蛊，列表多半是空的，空时在凹槽中间写一行失效色的「眼下没有」。</li>
 *   <li>「捕获入库」悬停显示现在会捕几只，一只都不会捕时按钮失效。</li>
 * </ul>
 */

final class GuPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final int SHIFT_STEP = 10;
    private static final int CAPTURE = 0;
    private final KeepList list = new KeepList();
    private int offset;

    GuPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    protected Component name() { return Component.translatable(UI + "tab.gu"); }

    private GuPart.Section data() { return section(GuPart.INSTANCE, GuPart.Section.EMPTY); }

    private List<GuPart.Row> rows() { return data().rows(); }

    private static Component nameOf(GuPart.Row row) {
        return BuiltInRegistries.ENTITY_TYPE.get(row.type()).getDescription();
    }

    @Override
    protected void resized(int width, int height) { list.layout(width, height); }

    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        List<GuPart.Row> rows = rows();
        offset = PanelDrawing.clampScroll(offset, list.rows(), rows.size());
        list.frame(sprites, rows.size(), offset, mouseX, mouseY);
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            list.row(sprites, i, rows.size(), rows.get(offset + i).hosted(), mouseX, mouseY);
        }
        PanelDrawing.button(sprites, list.buttonX(CAPTURE), list.buttonY(), KeepList.BUTTON_W, KeepList.BUTTON_H,
                data().capturePreview() > 0, mouseX, mouseY);
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        GuPart.Section data = data();
        List<GuPart.Row> rows = data.rows();
        PanelDrawing.title(graphics, font(), name(), Component.translatable(UI + "kinds", rows.size()), list.x(),
                list.y(), list.w());
        list.header(graphics, font(), Component.translatable(UI + "gu"), rows.size());
        if (rows.isEmpty()) {
            PanelDrawing.centered(graphics, font(), Component.translatable(UI + "gu_empty"), list.insetX(),
                    list.insetX() + list.insetW(), list.insetY() + list.insetH() / 2 - 4, PanelDrawing.DEAD);
        }
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            GuPart.Row row = rows.get(offset + i);
            list.rowTexts(graphics, font(), i, rows.size(), nameOf(row), PanelDrawing.TEXT, row.count(), row.keep(),
                    row.hosted());
        }
        if (data.hosting()) {
            graphics.drawString(font(), Component.translatable(UI + "gu_status", data.hostingIn(), data.lastCaptured()),
                    list.insetX(), list.statusY(), PanelDrawing.LEGEND, false);
        }
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "capture"), list.buttonX(CAPTURE),
                list.buttonY(), KeepList.BUTTON_W, KeepList.BUTTON_H, data.capturePreview() > 0);
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<GuPart.Row> rows = rows();
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            Item item = BuiltInRegistries.ITEM.get(rows.get(offset + i).item());
            if (item != Items.AIR) graphics.renderItem(new ItemStack(item), list.rowX() + 2, list.rowTop(i) + 2);
        }
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        GuPart.Section data = data();
        List<GuPart.Row> rows = data.rows();
        if (tip(graphics, list.buttonX(CAPTURE), list.buttonY(), KeepList.BUTTON_W, KeepList.BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "capture", data.capturePreview()))) {
            return;
        }
        Component hosting = Component.translatable(TIP + "gu_hosting", LandHostingJob.INTERVAL_TICKS / Ticks.SECOND,
                GuRules.HOSTING_GU);
        if (tip(graphics, list.insetX(), list.statusY() - 2, list.insetW(), 12, mouseX, mouseY, hosting)) return;
        if (tip(graphics, list.switchX(rows.size()), list.y() + 17, PanelDrawing.SWITCH_W + 8, 12, mouseX, mouseY,
                Component.translatable(UI + "gu_note"))) {
            return;
        }
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            int top = list.rowTop(i);
            if (tip(graphics, list.switchX(rows.size()), top + 5, PanelDrawing.SWITCH_W, PanelDrawing.SWITCH_H, mouseX,
                    mouseY, Component.translatable(TIP + "gu_switch"))) {
                return;
            }
            int minus = list.minusX(rows.size());
            if (tip(graphics, minus, top + 3, list.plusX(rows.size()) + PanelDrawing.GHOST_H - minus,
                    PanelDrawing.GHOST_H, mouseX, mouseY, Component.translatable(TIP + "keep", SHIFT_STEP))) {
                return;
            }
        }
    }

    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (list.isOverButton(CAPTURE, mouseX, mouseY)) {
            if (data().capturePreview() > 0) send(new GuPart.Capture());
            return true;
        }
        List<GuPart.Row> rows = rows();
        int step = shift ? SHIFT_STEP : 1;
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            GuPart.Row row = rows.get(offset + i);
            int top = list.rowTop(i);
            if (Widgets.isOver(mouseX, mouseY, list.switchX(rows.size()), top + 5, PanelDrawing.SWITCH_W,
                    PanelDrawing.SWITCH_H)) {
                send(new GuPart.SetHosting(row.type(), !row.hosted()));
                return true;
            }
            if (!row.hosted()) continue;
            if (Widgets.isOver(mouseX, mouseY, list.minusX(rows.size()), top + 3, PanelDrawing.GHOST_H,
                    PanelDrawing.GHOST_H)) {
                send(new GuPart.SetKeep(row.type(), Math.max(0, row.keep() - step)));
                return true;
            }
            if (Widgets.isOver(mouseX, mouseY, list.plusX(rows.size()), top + 3, PanelDrawing.GHOST_H,
                    PanelDrawing.GHOST_H)) {
                send(new GuPart.SetKeep(row.type(), row.keep() + step));
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!list.isOverList(mouseX, mouseY)) return false;
        offset = PanelDrawing.clampScroll(offset - (int) Math.signum(delta), list.rows(), rows().size());
        return true;
    }
}
