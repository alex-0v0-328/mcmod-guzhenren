package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.List;
import java.util.stream.Collectors;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdRules;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.HerdPart;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandHostingJob;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/**
 * 「养殖」小节：一张 {@link KeepList} 卡，标题右侧写牧场数与栏内、栏外头数；每种动物一行（刷怪蛋图标、名字、数量、托管
 * 开关、保持数 −/+），卡底托管状态与「喂养繁殖」、危险键「宰杀收取」。放置、迁移、拆除牧场与「收拢」在地图页。
 * <ul>
 *   <li>行与两个一键操作只算各牧场栏内的动物。</li>
 *   <li>托管按种类开关；关着时 −/+ 失效、保持数画失效色，但保持数照存，一键宰杀也照它算。托管说明在「托管」表头的悬停提示。
 *       </li>
 *   <li>−/+ 每次改 1，按住 Shift 改 {@value #SHIFT_STEP}；保持数不小于 0。</li>
 *   <li>悬停一行看库藏里这种动物的饲料；饲料不够喂一对（少于 2 份）且至少有两只时名字画警示色。</li>
 *   <li>「宰杀收取」悬停显示现在会宰几头，一头都不会宰时按钮失效。没有刷怪蛋的种类不画图标。</li>
 * </ul>
 */

final class HerdPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final int SHIFT_STEP = 10;
    private static final int SLAUGHTER = 0;
    private static final int BREED = 1;
    private final KeepList list = new KeepList();
    private int offset;

    HerdPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    protected Component name() { return Component.translatable(UI + "tab.husbandry"); }

    private HerdPart.Section data() { return section(HerdPart.INSTANCE, HerdPart.Section.EMPTY); }

    private List<HerdPart.Row> rows() { return data().rows(); }

    private static boolean shortOfFeed(HerdPart.Row row) {
        return row.feed() != HerdPart.UNKNOWN_FEED && row.feed() < 2 && row.count() >= 2;
    }

    private static Component nameOf(HerdPart.Row row) {
        return BuiltInRegistries.ENTITY_TYPE.get(row.type()).getDescription();
    }

    private Component pen() {
        HerdPart.Section data = data();
        return Component.translatable(UI + "pen", data.pen().pastures(), data.pen().inside(), data.pen().outside());
    }

    @Override
    protected void resized(int width, int height) { list.layout(width, height); }

    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        List<HerdPart.Row> rows = rows();
        offset = PanelDrawing.clampScroll(offset, list.rows(), rows.size());
        list.frame(sprites, rows.size(), offset, mouseX, mouseY);
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            list.row(sprites, i, rows.size(), rows.get(offset + i).hosted(), mouseX, mouseY);
        }
        PanelDrawing.button(sprites, list.buttonX(BREED), list.buttonY(), KeepList.BUTTON_W, KeepList.BUTTON_H, true,
                mouseX, mouseY);
        PanelDrawing.button(sprites, list.buttonX(SLAUGHTER), list.buttonY(), KeepList.BUTTON_W, KeepList.BUTTON_H,
                PanelDrawing.Look.DANGER, data().slaughterPreview() > 0, mouseX, mouseY);
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        HerdPart.Section data = data();
        List<HerdPart.Row> rows = data.rows();
        PanelDrawing.title(graphics, font(), name(), pen(), list.x(), list.y(), list.w());
        list.header(graphics, font(), Component.translatable(UI + "animal"), rows.size());
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            HerdPart.Row row = rows.get(offset + i);
            list.rowTexts(graphics, font(), i, rows.size(), nameOf(row),
                    shortOfFeed(row) ? PanelDrawing.WARN : PanelDrawing.TEXT, row.count(), row.keep(), row.hosted());
        }
        if (data.hosting()) {
            Component status = Component.translatable(UI + "hosting_status", data.hostingIn(),
                    Component.translatable(UI + "herd_round", data.lastSlaughtered(), data.lastBred()));
            graphics.drawString(font(), status, list.insetX(), list.statusY(), PanelDrawing.LEGEND, false);
        }
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "breed"), list.buttonX(BREED), list.buttonY(),
                KeepList.BUTTON_W, KeepList.BUTTON_H, true);
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "slaughter"), list.buttonX(SLAUGHTER),
                list.buttonY(), KeepList.BUTTON_W, KeepList.BUTTON_H, PanelDrawing.Look.DANGER,
                data.slaughterPreview() > 0);
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<HerdPart.Row> rows = rows();
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            SpawnEggItem egg = SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(rows.get(offset + i).type()));
            if (egg != null) graphics.renderItem(new ItemStack(egg), list.rowX() + 2, list.rowTop(i) + 2);
        }
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        HerdPart.Section data = data();
        List<HerdPart.Row> rows = data.rows();
        String hungry = rows.stream().filter(HerdPage::shortOfFeed).map(row -> nameOf(row).getString())
                .collect(Collectors.joining("、"));
        Component breed = hungry.isEmpty() ? Component.translatable(TIP + "breed", HerdRules.LIMIT)
                : Component.translatable(TIP + "breed_hungry", HerdRules.LIMIT, hungry);
        if (tip(graphics, list.buttonX(BREED), list.buttonY(), KeepList.BUTTON_W, KeepList.BUTTON_H, mouseX, mouseY,
                breed)) {
            return;
        }
        if (tip(graphics, list.buttonX(SLAUGHTER), list.buttonY(), KeepList.BUTTON_W, KeepList.BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "slaughter", data.slaughterPreview()))) {
            return;
        }
        int penW = font().width(pen());
        if (tip(graphics, list.x() + list.w() - PanelDrawing.PAD - penW, list.y(), penW, PanelDrawing.TITLE_H, mouseX,
                mouseY, Component.translatable(TIP + "pen"))) {
            return;
        }
        Component hosting = Component.translatable(TIP + "hosting", LandHostingJob.INTERVAL_TICKS / Ticks.SECOND,
                Component.translatable(TIP + "herd_hosting", HerdRules.HOSTING_ANIMALS));
        if (tip(graphics, list.insetX(), list.statusY() - 2, list.insetW(), 12, mouseX, mouseY, hosting)) return;
        if (tip(graphics, list.switchX(rows.size()), list.y() + 17, PanelDrawing.SWITCH_W + 8, 12, mouseX, mouseY,
                Component.translatable(UI + "herd_note"))) {
            return;
        }
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            HerdPart.Row row = rows.get(offset + i);
            int top = list.rowTop(i);
            if (tip(graphics, list.switchX(rows.size()), top + 5, PanelDrawing.SWITCH_W, PanelDrawing.SWITCH_H, mouseX,
                    mouseY, Component.translatable(TIP + "herd_switch"))) {
                return;
            }
            int minus = list.minusX(rows.size());
            if (tip(graphics, minus, top + 3, list.plusX(rows.size()) + PanelDrawing.GHOST_H - minus,
                    PanelDrawing.GHOST_H, mouseX, mouseY, Component.translatable(TIP + "keep", SHIFT_STEP))) {
                return;
            }
            Component feed = row.feed() == HerdPart.UNKNOWN_FEED ? Component.translatable(TIP + "feed_unknown")
                    : Component.translatable(TIP + "feed", row.feed());
            if (tip(graphics, list.rowX(), top, list.switchX(rows.size()) - list.rowX(), KeepList.ROW_H, mouseX, mouseY,
                    feed)) {
                return;
            }
        }
    }

    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (list.isOverButton(BREED, mouseX, mouseY)) {
            send(new HerdPart.Breed());
            return true;
        }
        if (list.isOverButton(SLAUGHTER, mouseX, mouseY)) {
            if (data().slaughterPreview() > 0) send(new HerdPart.Slaughter());
            return true;
        }
        List<HerdPart.Row> rows = rows();
        int step = shift ? SHIFT_STEP : 1;
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            HerdPart.Row row = rows.get(offset + i);
            int top = list.rowTop(i);
            if (Widgets.isOver(mouseX, mouseY, list.switchX(rows.size()), top + 5, PanelDrawing.SWITCH_W,
                    PanelDrawing.SWITCH_H)) {
                send(new HerdPart.SetHosting(row.type(), !row.hosted()));
                return true;
            }
            if (!row.hosted()) continue;
            if (Widgets.isOver(mouseX, mouseY, list.minusX(rows.size()), top + 3, PanelDrawing.GHOST_H,
                    PanelDrawing.GHOST_H)) {
                send(new HerdPart.SetKeep(row.type(), Math.max(0, row.keep() - step)));
                return true;
            }
            if (Widgets.isOver(mouseX, mouseY, list.plusX(rows.size()), top + 3, PanelDrawing.GHOST_H,
                    PanelDrawing.GHOST_H)) {
                send(new HerdPart.SetKeep(row.type(), row.keep() + step));
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
