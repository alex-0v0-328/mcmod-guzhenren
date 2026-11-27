package net.alex.guzhenren.client.screen.immortalaperture;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmRules;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmState;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.FarmPart;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandHostingJob;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 「种植」小节：一张卡，标题右侧写「标准田 N / 上限 · 规划 M」（有规划时补「开垦中」，每秒 1 块，不设冷却）；凹槽里每种
 * 作物一行（图标、名字、成熟数、未熟数、金色成熟比例条），卡底一行新田作物与库藏种子数，一行自动收割补种开关与「收割并
 * 补种」，托管开着时再一行状态（下一轮倒计时与上一轮收了几株、补了几格）。选址、规划、拆田与单田的作物、田型在地图页。
 * <ul>
 *   <li>新田作物按钮循环切换：自动 → 小麦 → 胡萝卜 → 马铃薯 → 甜菜根 → 知心草 → 自动；右边写库藏里这种的种子数（自动 =
 *       五种合计），没有种子时警示色。</li>
 *   <li>卡宽至多 {@value #MAX_W}；行数随窗口高（{@value #MIN_ROWS} 到 {@value #MAX_ROWS} 行），多了滚轮翻、右侧滚动条。
 *       </li>
 * </ul>
 */

final class FarmPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final int MAX_W = 420;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 8;
    private static final int ROW_H = 20;
    private static final int INSET_Y = 30;
    private static final int FOOT_H = 6 + 22 + 24 + 10 + PanelDrawing.PAD;
    private static final int CROP_W = 60;
    private static final int HARVEST_W = 72;
    private static final int HARVEST_H = 18;
    private static final int RATIO_W = 40;
    private int x;
    private int y;
    private int w = MAX_W;
    private int rows = MIN_ROWS;
    private int offset;

    FarmPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    protected Component name() { return Component.translatable(UI + "tab.farming"); }

    private FarmPart.Section data() { return section(FarmPart.INSTANCE, FarmPart.Section.EMPTY); }

    private List<FarmPart.Crop> crops() { return data().rows(); }

    private static Component cropName(int crop) {
        return crop == FarmState.AUTO_CROP ? Component.translatable(UI + "crop_auto")
                : FarmRules.crops().get(crop).getName();
    }

    @Override
    protected void resized(int width, int height) {
        x = PanelDrawing.contentX(width);
        y = InfoPage.CONTENT_TOP;
        w = Math.min(PanelDrawing.contentWidth(width), MAX_W);
        rows = Math.clamp((height - y - INSET_Y - 2 - FOOT_H - PanelDrawing.MARGIN) / ROW_H, MIN_ROWS, MAX_ROWS);
    }

    //region Geometry [几何]
    private int insetX() { return x + PanelDrawing.PAD; }

    private int insetY() { return y + INSET_Y; }

    private int insetW() { return w - 2 * PanelDrawing.PAD; }

    private int insetH() { return rows * ROW_H + 2; }

    private int rowX() { return insetX() + 1; }

    private int rowW() { return insetW() - 2 - (crops().size() > rows ? 8 : 0); }

    private int rowTop(int index) { return insetY() + 1 + index * ROW_H; }

    private int ratioX() { return rowX() + rowW() - 4 - RATIO_W; }

    private int growingRight() { return ratioX() - 10; }

    private int matureRight() { return growingRight() - 34; }

    private int pickY() { return insetY() + insetH() + 6; }

    private int pickX() { return insetX() + font().width(Component.translatable(UI + "crop_pick")) + 6; }

    private int hostY() { return pickY() + 22; }

    private int harvestX() { return x + w - PanelDrawing.PAD - HARVEST_W; }

    private int statusY() { return hostY() + 24; }

    private int cardH() { return INSET_Y + insetH() + FOOT_H; }
    //endregion

    private Component fields() {
        FarmPart.Section data = data();
        MutableComponent fields = Component.translatable(UI + "fields", data.fields(), data.capacity(), data.planned());
        return data.planned() > 0 ? fields.append(" · ").append(Component.translatable(UI + "reclaiming")) : fields;
    }

    @Override
    protected void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        FarmPart.Section data = data();
        PanelDrawing.card(sprites, this.x, this.y, w, cardH());
        sprites.blit(PanelDrawing.INSET, insetX(), insetY(), insetW(), insetH());
        List<FarmPart.Crop> crops = data.rows();
        offset = PanelDrawing.clampScroll(offset, rows, crops.size());
        for (int i = 0; i < rows && offset + i < crops.size(); i++) {
            FarmPart.Crop crop = crops.get(offset + i);
            int top = rowTop(i);
            PanelDrawing.row(sprites, rowX(), top, rowW(), ROW_H, Widgets.isOver(mouseX, mouseY, rowX(), top, rowW(),
                    ROW_H));
            int total = crop.mature() + crop.growing();
            int filled = total == 0 ? 0 : (RATIO_W - 2) * crop.mature() / total;
            sprites.blit(PanelDrawing.BAR_BACKGROUND, ratioX(), top + 7, RATIO_W, 6);
            sprites.blit(PanelDrawing.BAR_FILL, ratioX() + 1, top + 8, filled, 4, PanelDrawing.ACCENT);
        }
        if (crops.size() > rows) {
            int barX = rowX() + rowW() + 1;
            PanelDrawing.scrollbar(sprites, barX, insetY() + 1, insetH() - 2, offset, rows, crops.size(),
                    Widgets.isOver(mouseX, mouseY, barX, insetY() + 1, 6, insetH() - 2));
        }
        PanelDrawing.button(sprites, pickX(), pickY(), CROP_W, PanelDrawing.BUTTON_H, true, mouseX, mouseY);
        PanelDrawing.toggle(sprites, insetX(), hostY() + 4, data.hosting(), true, mouseX, mouseY);
        PanelDrawing.button(sprites, harvestX(), hostY(), HARVEST_W, HARVEST_H, true, mouseX, mouseY);
    }

    @Override
    protected void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        FarmPart.Section data = data();
        PanelDrawing.title(graphics, font(), name(), fields(), this.x, this.y, w);
        int header = this.y + 19;
        graphics.drawString(font(), Component.translatable(UI + "crop"), rowX() + 4, header, PanelDrawing.LEGEND,
                false);
        PanelDrawing.right(graphics, font(), Component.translatable(UI + "mature"), matureRight(), header,
                PanelDrawing.LEGEND);
        PanelDrawing.right(graphics, font(), Component.translatable(UI + "growing"), growingRight(), header,
                PanelDrawing.LEGEND);
        List<FarmPart.Crop> crops = data.rows();
        for (int i = 0; i < rows && offset + i < crops.size(); i++) {
            FarmPart.Crop crop = crops.get(offset + i);
            int top = rowTop(i);
            Block block = BuiltInRegistries.BLOCK.get(crop.block());
            graphics.drawString(font(), block.getName(), rowX() + 22, top + 6, PanelDrawing.TEXT, false);
            PanelDrawing.right(graphics, font(), Integer.toString(crop.mature()), matureRight(), top + 6,
                    PanelDrawing.TEXT);
            PanelDrawing.right(graphics, font(), Integer.toString(crop.growing()), growingRight(), top + 6,
                    PanelDrawing.LEGEND);
        }
        graphics.drawString(font(), Component.translatable(UI + "crop_pick"), insetX(), pickY() + 4,
                PanelDrawing.LEGEND, false);
        PanelDrawing.label(graphics, font(), cropName(data.crop()), pickX(), pickY(), CROP_W, PanelDrawing.BUTTON_H,
                true);
        long seeds = data.seedsOf(data.crop());
        PanelDrawing.right(graphics, font(), Component.translatable(UI + "seeds", PanelDrawing.count(seeds)),
                this.x + w - PanelDrawing.PAD, pickY() + 4, seeds > 0 ? PanelDrawing.TEXT : PanelDrawing.WARN);
        graphics.drawString(font(), Component.translatable(UI + "auto_harvest"), insetX() + PanelDrawing.SWITCH_W + 6,
                hostY() + 5, PanelDrawing.TEXT, false);
        PanelDrawing.label(graphics, font(), Component.translatable(UI + "harvest"), harvestX(), hostY(), HARVEST_W,
                HARVEST_H, true);
        if (data.hosting()) {
            graphics.drawString(font(), Component.translatable(UI + "farm_status", data.hostingIn(),
                    data.lastReaped(), data.lastSown()), insetX(), statusY(), PanelDrawing.LEGEND, false);
        }
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
        if (tip(graphics, pickX(), pickY(), CROP_W, PanelDrawing.BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "crop_pick"))) {
            return;
        }
        int fieldsW = font().width(fields());
        if (tip(graphics, this.x + w - PanelDrawing.PAD - fieldsW, this.y, fieldsW, PanelDrawing.TITLE_H, mouseX,
                mouseY, Component.translatable(TIP + "fields"))) {
            return;
        }
        if (tip(graphics, insetX(), hostY() + 4, PanelDrawing.SWITCH_W, PanelDrawing.SWITCH_H, mouseX, mouseY,
                Component.translatable(TIP + "farm_hosting", hostingSeconds, FarmRules.HOSTING_FIELDS))) {
            return;
        }
        tip(graphics, harvestX(), hostY(), HARVEST_W, HARVEST_H, mouseX, mouseY,
                Component.translatable(TIP + "harvest"));
    }

    @Override
    protected boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        FarmPart.Section data = data();
        if (Widgets.isOver(mouseX, mouseY, pickX(), pickY(), CROP_W, PanelDrawing.BUTTON_H)) {
            int next = data.crop() + 1 >= FarmRules.crops().size() ? FarmState.AUTO_CROP : data.crop() + 1;
            send(new FarmPart.SetCrop(next));
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, insetX(), hostY() + 4, PanelDrawing.SWITCH_W, PanelDrawing.SWITCH_H)) {
            send(new FarmPart.SetHosting(!data.hosting()));
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, harvestX(), hostY(), HARVEST_W, HARVEST_H)) {
            send(new FarmPart.Harvest());
            return true;
        }
        return false;
    }

    @Override
    protected boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, insetX(), insetY(), insetW(), insetH())) return false;
        offset = PanelDrawing.clampScroll(offset - (int) Math.signum(delta), rows, crops().size());
        return true;
    }
}
