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

/**
 * 「养殖」小节：一张 {@link KeepList} 卡，标题右侧写牧场数与栏内、栏外头数；每个牧场一行（Alex 定：一个牧场一种动物，文字
 * 不用刷怪蛋）：「牧场 N · 动物」、栏内头数、下限 −/+；卡底托管状态与「喂养繁殖」、危险键「宰杀收取」。放置、移动、拆除牧场，
 * 指定动物与「收拢」在地图页的牧场对象栏。
 * <ul>
 *   <li>托管有主就跑、没有开关：下限大于 0 的牧场每轮多了宰、少了喂；下限 0 = 不动。没指定动物的牧场行画失效色，−/+ 失效。
 *       </li>
 *   <li>−/+ 每次改 1，按住 Shift 改 {@value #SHIFT_STEP}；下限与地图牧场对象栏「宰杀」弹窗里的是同一个数。</li>
 *   <li>点行的名字部分跳到地图页并选中这个牧场；悬停名字看库藏里这种动物的饲料，饲料不够喂一对（少于 2 份）且栏内至少有两只
 *       时名字画警示色。</li>
 *   <li>两个一键操作对全部牧场各按自己的下限做；「宰杀收取」悬停显示现在会宰几头，一头都不会宰时按钮失效。</li>
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
        return row.feed() != HerdPart.UNKNOWN_FEED && row.feed() < 2 && row.herd() >= 2;
    }

    private static Component nameOf(HerdPart.Row row) {
        return row.animal().map(type -> Component.translatable(UI + "pasture_row", row.index() + 1,
                        BuiltInRegistries.ENTITY_TYPE.get(type).getDescription()))
                .orElseGet(() -> Component.translatable(UI + "pasture_row_none", row.index() + 1));
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
            list.row(sprites, i, rows.size(), rows.get(offset + i).animal().isPresent(), mouseX, mouseY);
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
        list.header(graphics, font(), Component.translatable(UI + "pasture"), Component.translatable(UI + "floor"),
                rows.size());
        if (rows.isEmpty()) {
            PanelDrawing.centered(graphics, font(), Component.translatable(UI + "herd_empty"), list.insetX(),
                    list.insetX() + list.insetW(), list.insetY() + list.insetH() / 2 - 4, PanelDrawing.DEAD);
        }
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            HerdPart.Row row = rows.get(offset + i);
            boolean live = row.animal().isPresent();
            int colour = !live ? PanelDrawing.DEAD : shortOfFeed(row) ? PanelDrawing.WARN : PanelDrawing.TEXT;
            list.rowTexts(graphics, font(), i, rows.size(), nameOf(row), colour, row.penned(), row.floor(), live);
        }
        Component status = Component.translatable(UI + "hosting_status", data.hostingIn(),
                Component.translatable(UI + "herd_round", data.lastSlaughtered(), data.lastBred()));
        graphics.drawString(font(), status, list.insetX(), list.statusY(), PanelDrawing.LEGEND, false);
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "breed"), list.buttonX(BREED), list.buttonY(),
                KeepList.BUTTON_W, KeepList.BUTTON_H, true);
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "slaughter"), list.buttonX(SLAUGHTER),
                list.buttonY(), KeepList.BUTTON_W, KeepList.BUTTON_H, PanelDrawing.Look.DANGER,
                data.slaughterPreview() > 0);
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
        Component hosting = Component.translatable(TIP + "herd_hosting", LandHostingJob.INTERVAL_TICKS / Ticks.SECOND,
                HerdRules.HOSTING_ANIMALS);
        if (tip(graphics, list.insetX(), list.statusY() - 2, list.insetW(), 12, mouseX, mouseY, hosting)) return;
        for (int i = 0; i < list.rows() && offset + i < rows.size(); i++) {
            HerdPart.Row row = rows.get(offset + i);
            int top = list.rowTop(i);
            int minus = list.minusX(rows.size());
            if (tip(graphics, minus, top + 3, list.plusX(rows.size()) + PanelDrawing.GHOST_H - minus,
                    PanelDrawing.GHOST_H, mouseX, mouseY, Component.translatable(TIP + "floor", SHIFT_STEP))) {
                return;
            }
            Component feed = row.feed() == HerdPart.UNKNOWN_FEED ? Component.translatable(TIP + "feed_unknown")
                    : Component.translatable(TIP + "feed", row.feed());
            if (tip(graphics, list.rowX(), top, minus - list.rowX(), KeepList.ROW_H, mouseX, mouseY, feed)) return;
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
            int minus = list.minusX(rows.size());
            if (row.animal().isPresent()
                    && Widgets.isOver(mouseX, mouseY, minus, top + 3, PanelDrawing.GHOST_H, PanelDrawing.GHOST_H)) {
                send(new HerdPart.SetFloor(row.index(), Math.max(0, row.floor() - step)));
                return true;
            }
            if (row.animal().isPresent() && Widgets.isOver(mouseX, mouseY, list.plusX(rows.size()), top + 3,
                    PanelDrawing.GHOST_H, PanelDrawing.GHOST_H)) {
                send(new HerdPart.SetFloor(row.index(), row.floor() + step));
                return true;
            }
            if (Widgets.isOver(mouseX, mouseY, list.rowX(), top, minus - list.rowX(), KeepList.ROW_H)) {
                screen.openMapPasture(row.index());
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
