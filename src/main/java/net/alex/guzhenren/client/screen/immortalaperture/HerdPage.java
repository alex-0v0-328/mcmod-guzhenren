package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.List;
import java.util.stream.Collectors;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdModule;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdWork;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandHostingJob;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/**
 * 「养殖」小节：每种动物一行（刷怪蛋图标、名字、数量、托管开关、保持数 −/+），牧场一行（牧场数、栏内、栏外），底部托管
 * 状态与「喂养繁殖」「宰杀收取」。放置、迁移、拆除牧场与「收拢」在地图页。
 * <ul>
 *   <li>行与两个一键操作只算各牧场栏内的动物。</li>
 *   <li>托管按种类开关；关着时 −/+ 失效、保持数画失效色，但保持数照存，一键宰杀也照它算。</li>
 *   <li>−/+ 每次改 1，按住 Shift 改 {@value #SHIFT_STEP}；保持数不小于 0。</li>
 *   <li>悬停一行看库藏里这种动物的饲料；饲料不够喂一对（少于 2 份）且至少有两只时名字画警示色。</li>
 *   <li>「宰杀收取」悬停显示现在会宰几头，一头都不会宰时按钮失效。</li>
 *   <li>一屏 {@value #VISIBLE} 行，滚轮翻，右侧滚动条。没有刷怪蛋的种类不画图标。</li>
 * </ul>
 */

final class HerdPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final int SHIFT_STEP = 10;
    private static final int LIST_Y = 31;
    private static final int ROW_H = 20;
    private static final int ROW_W = 150;
    private static final int VISIBLE = 6;
    private static final int LIST_H = VISIBLE * ROW_H + 2;
    private static final int SWITCH_X = 92;
    private static final int MINUS_X = 114;
    private static final int PLUS_X = 144;
    private static final int PEN_Y = 157;
    private static final int PEN_H = 16;
    private static final int STATUS_Y = 178;
    private static final int BUTTON_Y = 192;
    private static final int BUTTON_W = 78;
    private static final int BUTTON_H = 20;
    private static final int BREED_X = 8;
    private static final int SLAUGHTER_X = 90;
    private static final Component MINUS = Component.literal("-");
    private static final Component PLUS = Component.literal("+");
    private int offset;

    HerdPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    protected Component name() { return Component.translatable(UI + "tab.husbandry"); }

    private HerdModule.Section data() { return section(HerdModule.INSTANCE, HerdModule.Section.EMPTY); }

    private List<HerdModule.Row> rows() { return data().rows(); }

    private static boolean shortOfFeed(HerdModule.Row row) {
        return row.feed() != HerdModule.UNKNOWN_FEED && row.feed() < 2 && row.count() >= 2;
    }

    private static Component nameOf(HerdModule.Row row) {
        return BuiltInRegistries.ENTITY_TYPE.get(row.type()).getDescription();
    }

    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        sprites.blit(PanelDrawing.WELL, x + 8, y + LIST_Y, 160, LIST_H);
        List<HerdModule.Row> rows = rows();
        offset = PanelDrawing.clampScroll(offset, VISIBLE, rows.size());
        for (int i = 0; i < VISIBLE && offset + i < rows.size(); i++) {
            HerdModule.Row row = rows.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            PanelDrawing.row(sprites, x + 9, top, ROW_W, ROW_H, Widgets.isOver(mouseX, mouseY, x + 9, top, ROW_W,
                    ROW_H));
            PanelDrawing.toggle(sprites, x + SWITCH_X, top + 5, row.hosted(), true, mouseX, mouseY);
            PanelDrawing.button(sprites, x + MINUS_X, top + 3, PanelDrawing.SQUARE, PanelDrawing.SQUARE, row.hosted(),
                    mouseX, mouseY);
            PanelDrawing.button(sprites, x + PLUS_X, top + 3, PanelDrawing.SQUARE, PanelDrawing.SQUARE, row.hosted(),
                    mouseX, mouseY);
        }
        boolean over = Widgets.isOver(mouseX, mouseY, x + 160, y + 32, 6, LIST_H - 2);
        PanelDrawing.scrollbar(sprites, x + 160, y + 32, LIST_H - 2, offset, VISIBLE, rows.size(), over);
        PanelDrawing.button(sprites, x + BREED_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, true, mouseX, mouseY);
        PanelDrawing.button(sprites, x + SLAUGHTER_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, data().slaughterPreview() > 0,
                mouseX, mouseY);
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        HerdModule.Section data = data();
        graphics.drawString(font(), Component.translatable(UI + "animal"), x + 12, y + 21, PanelDrawing.LEGEND, false);
        PanelDrawing.right(graphics, font(), Component.translatable(UI + "count"), x + 84, y + 21, PanelDrawing.LEGEND);
        graphics.drawString(font(), Component.translatable(UI + "hosting"), x + 92, y + 21, PanelDrawing.LEGEND, false);
        PanelDrawing.centered(graphics, font(), Component.translatable(UI + "keep"), x + 114, x + 157, y + 21,
                PanelDrawing.LEGEND);
        List<HerdModule.Row> rows = data.rows();
        for (int i = 0; i < VISIBLE && offset + i < rows.size(); i++) {
            HerdModule.Row row = rows.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            graphics.drawString(font(), nameOf(row), x + 30, top + 6,
                    shortOfFeed(row) ? PanelDrawing.WARN : PanelDrawing.TEXT,
                    false);
            PanelDrawing.right(graphics, font(), Integer.toString(row.count()), x + 84, top + 6, PanelDrawing.TEXT);
            PanelDrawing.label(graphics, font(), MINUS, x + MINUS_X, top + 3, PanelDrawing.SQUARE, PanelDrawing.SQUARE,
                    row.hosted());
            PanelDrawing.label(graphics, font(), PLUS, x + PLUS_X, top + 3, PanelDrawing.SQUARE, PanelDrawing.SQUARE,
                    row.hosted());
            PanelDrawing.centered(graphics, font(), Component.literal(Integer.toString(row.keep())), x + 128, x + 143,
                    top + 6, row.hosted() ? PanelDrawing.TEXT : PanelDrawing.DEAD);
        }
        graphics.drawString(font(), Component.translatable(UI + "pen", data.pen().pastures(), data.pen().inside(),
                data.pen().outside()), x + 8, y + PEN_Y + 4, PanelDrawing.LEGEND, false);
        Component status = data.hosting()
                ? Component.translatable(UI + "herd_status", data.hostingIn(), data.lastSlaughtered(), data.lastBred())
                : Component.translatable(UI + "herd_note");
        graphics.drawString(font(), status, x + 8, y + STATUS_Y, PanelDrawing.LEGEND, false);
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "breed"), x + BREED_X, y + BUTTON_Y, BUTTON_W,
                BUTTON_H, true);
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "slaughter"), x + SLAUGHTER_X, y + BUTTON_Y,
                BUTTON_W, BUTTON_H, data.slaughterPreview() > 0);
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<HerdModule.Row> rows = rows();
        for (int i = 0; i < VISIBLE && offset + i < rows.size(); i++) {
            SpawnEggItem egg = SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(rows.get(offset + i).type()));
            if (egg != null) graphics.renderItem(new ItemStack(egg), x + 11, y + LIST_Y + 1 + i * ROW_H + 2);
        }
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        HerdModule.Section data = data();
        String hungry = data.rows().stream().filter(HerdPage::shortOfFeed).map(row -> nameOf(row).getString())
                .collect(Collectors.joining("、"));
        Component breed = hungry.isEmpty() ? Component.translatable(TIP + "breed", HerdWork.LIMIT)
                : Component.translatable(TIP + "breed_hungry", HerdWork.LIMIT, hungry);
        if (tip(graphics, x + BREED_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, mouseX, mouseY, breed)) return;
        if (tip(graphics, x + SLAUGHTER_X, y + BUTTON_Y, BUTTON_W, BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "slaughter", data.slaughterPreview()))) {
            return;
        }
        if (tip(graphics, x + 8, y + PEN_Y, 160, PEN_H, mouseX, mouseY, Component.translatable(TIP + "pen"))) {
            return;
        }
        if (tip(graphics, x + 8, y + STATUS_Y - 2, 160, 12, mouseX, mouseY, Component.translatable(TIP + "herd_hosting",
                LandHostingJob.INTERVAL_TICKS / Ticks.SECOND, HerdWork.HOSTING_ANIMALS))) {
            return;
        }
        List<HerdModule.Row> rows = data.rows();
        for (int i = 0; i < VISIBLE && offset + i < rows.size(); i++) {
            HerdModule.Row row = rows.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            if (tip(graphics, x + SWITCH_X, top + 5, PanelDrawing.SWITCH_W, PanelDrawing.SWITCH_H, mouseX, mouseY,
                    Component.translatable(TIP + "herd_switch"))) {
                return;
            }
            if (tip(graphics, x + MINUS_X, top + 3, PLUS_X + PanelDrawing.SQUARE - MINUS_X, PanelDrawing.SQUARE,
                    mouseX, mouseY, Component.translatable(TIP + "keep", SHIFT_STEP))) {
                return;
            }
            Component feed = row.feed() == HerdModule.UNKNOWN_FEED ? Component.translatable(TIP + "feed_unknown")
                    : Component.translatable(TIP + "feed", row.feed());
            if (tip(graphics, x + 9, top, SWITCH_X - 9, ROW_H, mouseX, mouseY, feed)) return;
        }
    }

    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (Widgets.isOver(mouseX, mouseY, x + BREED_X, y + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            send(new HerdModule.Breed());
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + SLAUGHTER_X, y + BUTTON_Y, BUTTON_W, BUTTON_H)) {
            if (data().slaughterPreview() > 0) send(new HerdModule.Slaughter());
            return true;
        }
        List<HerdModule.Row> rows = rows();
        int step = shift ? SHIFT_STEP : 1;
        for (int i = 0; i < VISIBLE && offset + i < rows.size(); i++) {
            HerdModule.Row row = rows.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            if (Widgets.isOver(mouseX, mouseY, x + SWITCH_X, top + 5, PanelDrawing.SWITCH_W, PanelDrawing.SWITCH_H)) {
                send(new HerdModule.SetHosting(row.type(), !row.hosted()));
                return true;
            }
            if (!row.hosted()) continue;
            if (Widgets.isOver(mouseX, mouseY, x + MINUS_X, top + 3, PanelDrawing.SQUARE, PanelDrawing.SQUARE)) {
                send(new HerdModule.SetKeep(row.type(), Math.max(0, row.keep() - step)));
                return true;
            }
            if (Widgets.isOver(mouseX, mouseY, x + PLUS_X, top + 3, PanelDrawing.SQUARE, PanelDrawing.SQUARE)) {
                send(new HerdModule.SetKeep(row.type(), row.keep() + step));
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, x + 8, y + LIST_Y, 160, LIST_H)) return false;
        offset = PanelDrawing.clampScroll(offset - (int) Math.signum(delta), VISIBLE, rows().size());
        return true;
    }
}
