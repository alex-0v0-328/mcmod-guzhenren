package net.alex.guzhenren.client.screen.blissful;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.blissful.BlissfulLands;
import net.alex.guzhenren.gameplay.blissful.farm.FarmModule;
import net.alex.guzhenren.gameplay.blissful.farm.FarmState;
import net.alex.guzhenren.gameplay.blissful.farm.FarmWork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 「种植」页：每种作物一行（图标、名字、成熟数、未熟数、成熟比例条），开垦作物与库藏种子数，标准田数与「开垦」，底部
 * 托管状态、托管开关与「收割并补种」。
 * <ul>
 *   <li>作物只有 {@link FarmWork#CROPS} 四种，列表一屏 {@value #VISIBLE} 行够用；省下的一行放开垦作物。</li>
 *   <li>开垦作物按钮循环切换：自动 → 小麦 → 胡萝卜 → 马铃薯 → 甜菜根 → 自动；右边写库藏里这种的种子数（自动 = 四种合计）。</li>
 *   <li>开垦冷却中或田区已满时按钮失效；冷却中文字换成剩余秒数。没种子照样能开，开出空田，之后有种子自动补种。</li>
 *   <li>托管开着时状态行写下一轮倒计时与上一轮收了几株、补了几格。</li>
 * </ul>
 */

final class FarmPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final int LIST_Y = 31;
    private static final int ROW_H = 20;
    private static final int VISIBLE = 5;
    private static final int LIST_H = VISIBLE * ROW_H + 2;
    private static final int CROP_Y = 137;
    private static final int CROP_X = 54;
    private static final int CROP_W = 46;
    private static final int RECLAIM_Y = 157;
    private static final int STATUS_Y = 180;
    private static final int BUTTON_X = 98;
    private static final int BUTTON_W = 70;
    private static final int BUTTON_H = 20;
    private static final int SWITCH_Y = 197;
    private static final int HARVEST_Y = 192;
    private static final int RATIO_X = 136;
    private static final int RATIO_W = 26;
    private int offset;

    FarmPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    Component name() { return Component.translatable(UI + "tab.farming"); }

    private FarmModule.Section data() { return section(FarmModule.Section.EMPTY); }

    private List<FarmModule.Crop> crops() { return data().rows(); }

    private static Component cropName(int crop) {
        return crop == FarmState.AUTO_CROP ? Component.translatable(UI + "crop_auto")
                : FarmWork.CROPS.get(crop).getName();
    }

    @Override
    void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        FarmModule.Section data = data();
        sprites.blit(PanelKit.WELL, x + 8, y + LIST_Y, 160, LIST_H);
        List<FarmModule.Crop> crops = data.rows();
        offset = PanelKit.clampScroll(offset, VISIBLE, crops.size());
        boolean scrolling = crops.size() > VISIBLE;
        int rowW = scrolling ? 150 : 158;
        for (int i = 0; i < VISIBLE && offset + i < crops.size(); i++) {
            FarmModule.Crop crop = crops.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            PanelKit.row(sprites, x + 9, top, rowW, ROW_H, Widgets.isOver(mouseX, mouseY, x + 9, top, rowW, ROW_H));
            int total = crop.mature() + crop.growing();
            int filled = total == 0 ? 0 : (RATIO_W - 2) * crop.mature() / total;
            sprites.blit(PanelKit.BAR_BACKGROUND, x + RATIO_X - (scrolling ? 8 : 0), top + 7, RATIO_W, 6);
            sprites.blit(PanelKit.BAR_FILL, x + RATIO_X - (scrolling ? 8 : 0) + 1, top + 8, filled, 4,
                    PanelKit.ACCENT);
        }
        if (scrolling) {
            PanelKit.scrollbar(sprites, x + 160, y + LIST_Y + 1, LIST_H - 2, offset, VISIBLE, crops.size(),
                    Widgets.isOver(mouseX, mouseY, x + 160, y + LIST_Y + 1, 6, LIST_H - 2));
        }
        PanelKit.button(sprites, x + CROP_X, y + CROP_Y, CROP_W, PanelKit.SQUARE, true, mouseX, mouseY);
        PanelKit.button(sprites, x + BUTTON_X, y + RECLAIM_Y, BUTTON_W, BUTTON_H, data.canReclaim(), mouseX, mouseY);
        PanelKit.toggle(sprites, x + 8, y + SWITCH_Y, data.hosting(), true, mouseX, mouseY);
        PanelKit.button(sprites, x + BUTTON_X, y + HARVEST_Y, BUTTON_W, BUTTON_H, true, mouseX, mouseY);
    }

    @Override
    void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        FarmModule.Section data = data();
        graphics.drawString(font(), Component.translatable(UI + "crop"), x + 12, y + 21, PanelKit.LEGEND, false);
        PanelKit.right(graphics, font(), Component.translatable(UI + "mature"), x + 100, y + 21, PanelKit.LEGEND);
        PanelKit.right(graphics, font(), Component.translatable(UI + "growing"), x + 130, y + 21, PanelKit.LEGEND);
        List<FarmModule.Crop> crops = data.rows();
        int shift = crops.size() > VISIBLE ? 8 : 0;
        for (int i = 0; i < VISIBLE && offset + i < crops.size(); i++) {
            FarmModule.Crop crop = crops.get(offset + i);
            int top = y + LIST_Y + 1 + i * ROW_H;
            Block block = BuiltInRegistries.BLOCK.get(crop.block());
            graphics.drawString(font(), block.getName(), x + 30, top + 6, PanelKit.TEXT, false);
            PanelKit.right(graphics, font(), Integer.toString(crop.mature()), x + 100 - shift, top + 6, PanelKit.TEXT);
            PanelKit.right(graphics, font(), Integer.toString(crop.growing()), x + 130 - shift, top + 6,
                    PanelKit.LEGEND);
        }
        graphics.drawString(font(), Component.translatable(UI + "crop_pick"), x + 8, y + CROP_Y + 3, PanelKit.LEGEND,
                false);
        PanelKit.label(graphics, font(), cropName(data.crop()), x + CROP_X, y + CROP_Y, CROP_W, PanelKit.SQUARE, true);
        long seeds = data.seedsOf(data.crop());
        PanelKit.right(graphics, font(), Component.translatable(UI + "seeds", PanelKit.count(seeds)), x + 167,
                y + CROP_Y + 3, seeds > 0 ? PanelKit.TEXT : PanelKit.WARN);
        graphics.drawString(font(), Component.translatable(UI + "fields", data.fields(), data.capacity()),
                x + 8, y + RECLAIM_Y + 6, PanelKit.LEGEND, false);
        Component reclaim = data.reclaimCooldown() > 0
                ? Component.translatable(UI + "cooldown", data.reclaimCooldown())
                : Component.translatable(UI + "reclaim");
        PanelKit.label(graphics, font(), reclaim, x + BUTTON_X, y + RECLAIM_Y, BUTTON_W, BUTTON_H, data.canReclaim());
        if (data.hosting()) {
            graphics.drawString(font(), Component.translatable(UI + "farm_status", data.hostingIn(),
                    data.lastReaped(), data.lastSown()), x + 8, y + STATUS_Y, PanelKit.LEGEND, false);
        }
        graphics.drawString(font(), Component.translatable(UI + "auto_harvest"), x + 30, y + SWITCH_Y + 1,
                PanelKit.TEXT, false);
        PanelKit.label(graphics, font(), Component.translatable(UI + "harvest"), x + BUTTON_X, y + HARVEST_Y,
                BUTTON_W, BUTTON_H, true);
    }

    @Override
    void items(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<FarmModule.Crop> crops = crops();
        for (int i = 0; i < VISIBLE && offset + i < crops.size(); i++) {
            Block block = BuiltInRegistries.BLOCK.get(crops.get(offset + i).block());
            graphics.renderItem(new ItemStack(block.asItem()), x + 11, y + LIST_Y + 1 + i * ROW_H + 2);
        }
    }

    @Override
    void tooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int hostingSeconds = BlissfulLands.HOSTING_TICKS / Ticks.SECOND;
        if (tip(graphics, x + CROP_X, y + CROP_Y, CROP_W, PanelKit.SQUARE, mouseX, mouseY,
                Component.translatable(TIP + "crop_pick"))) {
            return;
        }
        if (tip(graphics, x + BUTTON_X, y + RECLAIM_Y, BUTTON_W, BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "reclaim", FarmWork.RECLAIM_COOLDOWN_TICKS / Ticks.SECOND))) {
            return;
        }
        if (tip(graphics, x + 8, y + SWITCH_Y, PanelKit.SWITCH_W, PanelKit.SWITCH_H, mouseX, mouseY,
                Component.translatable(TIP + "farm_hosting", hostingSeconds, BlissfulLands.HOSTING_LIMIT))) {
            return;
        }
        tip(graphics, x + BUTTON_X, y + HARVEST_Y, BUTTON_W, BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "harvest"));
    }

    @Override
    boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        FarmModule.Section data = data();
        if (Widgets.isOver(mouseX, mouseY, x + CROP_X, y + CROP_Y, CROP_W, PanelKit.SQUARE)) {
            int next = data.crop() + 1 >= FarmWork.CROPS.size() ? FarmState.AUTO_CROP : data.crop() + 1;
            send(new FarmModule.SetCrop(next));
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + BUTTON_X, y + RECLAIM_Y, BUTTON_W, BUTTON_H)) {
            if (data.canReclaim()) send(new FarmModule.Reclaim());
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + 8, y + SWITCH_Y, PanelKit.SWITCH_W, PanelKit.SWITCH_H)) {
            send(new FarmModule.SetHosting(!data.hosting()));
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + BUTTON_X, y + HARVEST_Y, BUTTON_W, BUTTON_H)) {
            send(new FarmModule.Harvest());
            return true;
        }
        return false;
    }

    @Override
    boolean scroll(int x, int y, double mouseX, double mouseY, double delta) {
        if (!Widgets.isOver(mouseX, mouseY, x + 8, y + LIST_Y, 160, LIST_H)) return false;
        offset = PanelKit.clampScroll(offset - (int) Math.signum(delta), VISIBLE, crops().size());
        return true;
    }
}
