package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.List;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmRules;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.FarmPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.Page;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandHostingJob;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 「种植」小节（统筹卡，Alex 定：操作都在地图页）：一张卡，标题右侧写「标准田 N / 上限 · 规划 M」（有规划时补「开垦中 · 约
 * M 秒」，每秒 1 块）；凹槽里每种作物一行（图标、名字、成熟数、未熟数、绿色成熟比例条），点一行跳到地图页并选中种这种作物的
 * 田；卡底一行库藏「种子 · 骨粉 · 腐泥冻土」，一行托管状态（下一轮倒计时与上一轮收了几株、补了几格）与「去地图」。
 * <ul>
 *   <li>成熟即收割、用库藏种子补种一直开着，没有开关（Alex 定）。</li>
 *   <li>卡宽至多 {@value InfoPage#COLUMN_W}、在单列里居中；行数随窗口高（{@value #MIN_ROWS} 到 {@value #MAX_ROWS} 行），多了滚轮翻、右侧滚动条。
 *       </li>
 * </ul>
 */

final class FarmPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 8;
    private static final int ROW_H = 20;
    private static final int INSET_Y = 30;
    private static final int FOOT_H = 6 + 14 + 22 + Widgets.PAD;
    private static final int MAP_W = 60;
    private static final int RATIO_W = 40;
    private int x;
    private int y;
    private int w = InfoPage.COLUMN_W;
    private int rows = MIN_ROWS;
    private int offset;

    FarmPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    protected Component name() { return Component.translatable(UI + "tab.farming"); }

    private FarmPart.Section data() { return section(FarmPart.INSTANCE, FarmPart.Section.EMPTY); }

    private List<FarmPart.Crop> crops() { return data().rows(); }

    @Override
    protected void resized(int width, int height) {
        x = InfoPage.columnX(width);
        y = InfoPage.CONTENT_TOP;
        w = InfoPage.columnWidth(width);
        rows = Math.clamp((height - y - INSET_Y - 2 - FOOT_H - PanelDrawing.MARGIN) / ROW_H, MIN_ROWS, MAX_ROWS);
    }

    //region Geometry [几何]
    private int insetX() { return x + Widgets.PAD; }

    private int insetY() { return y + INSET_Y; }

    private int insetW() { return w - 2 * Widgets.PAD; }

    private int insetH() { return rows * ROW_H + 2; }

    private int rowX() { return insetX() + 1; }

    private int rowW() { return insetW() - 2 - (crops().size() > rows ? 8 : 0); }

    private int rowTop(int index) { return insetY() + 1 + index * ROW_H; }

    private int ratioX() { return rowX() + rowW() - 4 - RATIO_W; }

    private int growingRight() { return ratioX() - 10; }

    private int matureRight() { return growingRight() - 34; }

    private int stockY() { return insetY() + insetH() + 6; }

    private int hostY() { return stockY() + 14; }

    private int mapX() { return x + w - Widgets.PAD - MAP_W; }

    private int cardH() { return INSET_Y + insetH() + FOOT_H; }

    private int rowAt(double mouseX, double mouseY) {
        List<FarmPart.Crop> crops = crops();
        for (int i = 0; i < rows && offset + i < crops.size(); i++) {
            if (Widgets.isOver(mouseX, mouseY, rowX(), rowTop(i), rowW(), ROW_H)) return offset + i;
        }
        return -1;
    }
    //endregion

    private Component fields() {
        FarmPart.Section data = data();
        MutableComponent fields = Component.translatable(UI + "fields", data.fields(), data.capacity(), data.planned());
        return data.planned() > 0
                ? fields.append(" · ").append(Component.translatable(UI + "reclaiming", data.planned())) : fields;
    }

    private Component stocks() {
        FarmPart.Section data = data();
        return Component.translatable(UI + "farm_stock", PanelDrawing.count(data.seeds()),
                PanelDrawing.count(data.boneMeal()), PanelDrawing.count(data.mud()));
    }

    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        FarmPart.Section data = data();
        Widgets.card(sprites, this.x, this.y, w, cardH());
        sprites.blit(Widgets.INSET, insetX(), insetY(), insetW(), insetH());
        List<FarmPart.Crop> crops = data.rows();
        offset = Widgets.clampScroll(offset, rows, crops.size());
        for (int i = 0; i < rows && offset + i < crops.size(); i++) {
            FarmPart.Crop crop = crops.get(offset + i);
            int top = rowTop(i);
            Widgets.row(sprites, rowX(), top, rowW(), ROW_H, Widgets.isOver(mouseX, mouseY, rowX(), top, rowW(),
                    ROW_H));
            int total = crop.mature() + crop.growing();
            int filled = total == 0 ? 0 : (RATIO_W - 2) * crop.mature() / total;
            sprites.blit(Widgets.BAR_BACKGROUND, ratioX(), top + 7, RATIO_W, 6);
            sprites.blit(Widgets.BAR_FILL, ratioX() + 1, top + 8, filled, 4, ModPalette.PLANT);
        }
        if (crops.size() > rows) {
            int barX = rowX() + rowW() + 1;
            Widgets.scrollbar(sprites, barX, insetY() + 1, insetH() - 2, offset, rows, crops.size(),
                    Widgets.isOver(mouseX, mouseY, barX, insetY() + 1, 6, insetH() - 2));
        }
        Widgets.button(sprites, mapX(), hostY(), MAP_W, Widgets.BUTTON_H, true, mouseX, mouseY);
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        FarmPart.Section data = data();
        Widgets.title(graphics, font(), name(), fields(), this.x, this.y, w);
        int header = this.y + 19;
        graphics.drawString(font(), Component.translatable(UI + "crop"), rowX() + 4, header, ModPalette.LEGEND_TEXT,
                false);
        Widgets.right(graphics, font(), Component.translatable(UI + "mature"), matureRight(), header,
                ModPalette.LEGEND_TEXT);
        Widgets.right(graphics, font(), Component.translatable(UI + "growing"), growingRight(), header,
                ModPalette.LEGEND_TEXT);
        List<FarmPart.Crop> crops = data.rows();
        for (int i = 0; i < rows && offset + i < crops.size(); i++) {
            FarmPart.Crop crop = crops.get(offset + i);
            int top = rowTop(i);
            Block block = BuiltInRegistries.BLOCK.get(crop.block());
            graphics.drawString(font(), block.getName(), rowX() + 22, top + 6, ModPalette.TEXT, false);
            Widgets.right(graphics, font(), Integer.toString(crop.mature()), matureRight(), top + 6,
                    ModPalette.TEXT);
            Widgets.right(graphics, font(), Integer.toString(crop.growing()), growingRight(), top + 6,
                    ModPalette.LEGEND_TEXT);
        }
        graphics.drawString(font(), stocks(), insetX(), stockY() + 2, ModPalette.LEGEND_TEXT, false);
        Widgets.label(graphics, font(), Component.translatable(UI + "to_map"), mapX(), hostY(), MAP_W,
                Widgets.BUTTON_H, true);
        Component round = Component.translatable(UI + "farm_round", data.lastReaped(), data.lastSown());
        graphics.drawString(font(), Component.translatable(UI + "hosting_status", data.hostingIn(), round), insetX(),
                hostY() + 5, ModPalette.LEGEND_TEXT, false);
    }

    @Override
    protected void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<FarmPart.Crop> crops = crops();
        for (int i = 0; i < rows && offset + i < crops.size(); i++) {
            Block block = BuiltInRegistries.BLOCK.get(crops.get(offset + i).block());
            graphics.renderItem(new ItemStack(block.asItem()), rowX() + 2, rowTop(i) + 2);
        }
    }

    @Override
    protected void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int hostingSeconds = LandHostingJob.INTERVAL_TICKS / Ticks.SECOND;
        int fieldsW = font().width(fields());
        if (tip(graphics, this.x + w - Widgets.PAD - fieldsW, this.y, fieldsW, Widgets.TITLE_H, mouseX,
                mouseY, Component.translatable(TIP + "fields"))) {
            return;
        }
        if (tip(graphics, insetX(), hostY() + 3, mapX() - insetX() - 6, 12, mouseX, mouseY,
                Component.translatable(TIP + "farm_hosting", hostingSeconds, FarmRules.HOSTING_FIELDS))) {
            return;
        }
        if (tip(graphics, mapX(), hostY(), MAP_W, Widgets.BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "to_map"))) {
            return;
        }
        int row = rowAt(mouseX, mouseY);
        if (row >= 0) {
            graphics.renderTooltip(font(), font().split(Component.translatable(TIP + "crop_row",
                    BuiltInRegistries.BLOCK.get(crops().get(row).block()).getName()), 200), mouseX, mouseY);
        }
    }

    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        if (Widgets.isOver(mouseX, mouseY, mapX(), hostY(), MAP_W, Widgets.BUTTON_H)) {
            screen.openPage(Page.MAP);
            return true;
        }
        int row = rowAt(mouseX, mouseY);
        if (row >= 0) {
            screen.openMapSelecting(FarmRules.crops().indexOf(BuiltInRegistries.BLOCK.get(crops().get(row).block())));
            return true;
        }
        return false;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, insetX(), insetY(), insetW(), insetH())) return false;
        offset = Widgets.clampScroll(offset - (int) Math.signum(delta), rows, crops().size());
        return true;
    }
}
