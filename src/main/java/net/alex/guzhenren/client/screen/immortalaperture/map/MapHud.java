package net.alex.guzhenren.client.screen.immortalaperture.map;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页左侧的两组 {@code chip}：左上资源条（田已用 / 上限含规划、牧场已用 / 上限、各牧场栏内牲畜合计）加「一键收获」键
 * （写全部田的熟株数，点一下收全部熟株并补种，0 株时失效），左下按键提示「[WASD] 平移 [滚轮] 缩放 [右键] 传送」（右键或
 * 中键拖动平移照常可用，不写；左键拖动在选择模式下是框选）。
 * <ul>
 *   <li>Alex 定：资源条不放「栏外」警报；详细说明走悬停提示。</li>
 *   <li>{@link #keyPhrase} 画「键帽 + 灰字」短语，工具栏上方的操作提示也用它；画图与写字两步各调一次，传空的那一侧跳过。</li>
 * </ul>
 */

final class MapHud {

    private static final int CHIP_H = 16;
    private static final int HARVEST_H = 20;
    private static final int TOP = EarthSpiritScreen.BAR_H + 6;
    private static final int LEFT = 6;
    private static final int BOTTOM = 7;
    private static final String UI = EarthSpiritScreen.UI + "map.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_res_";
    private static final String[] RESOURCES = {"farm", "pasture", "livestock"};
    private final int[] lefts = new int[RESOURCES.length];
    private final int[] widths = new int[RESOURCES.length];
    private int keysY;
    private int keysW;
    private int harvestX;
    private int harvestW;

    void layout(int height) { keysY = height - BOTTOM - CHIP_H; }

    /** 左下按键提示的顶边，选区卡贴在它上方。 */
    int keysTop() { return keysY; }

    private static int ripe(MapTools.Land land) {
        int ripe = 0;
        for (MapPlots.Plot plot : land.plots()) {
            if (plot.kind() == MapPlots.Kind.FIELD) ripe += plot.mature();
        }
        return ripe;
    }

    private static Component harvestLabel(MapTools.Land land) {
        return Component.translatable(UI + "harvest_all", PanelDrawing.count(ripe(land)));
    }

    boolean isOverHarvest(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, harvestX, TOP - 2, harvestW, HARVEST_H);
    }

    /** 点「一键收获」：有熟株才发。 */
    void clickHarvest(MapTools.Land land) {
        if (ripe(land) > 0) land.harvestAll();
    }

    static int keyPhrase(@Nullable SpriteBatch sprites, @Nullable GuiGraphics graphics, Font font, Component key,
                         Component action, int x, int y) {
        int keyW = PanelDrawing.keycapWidth(font, key);
        if (sprites != null) PanelDrawing.keycap(sprites, x, y + 2, keyW);
        if (graphics != null) {
            PanelDrawing.keycapText(graphics, font, key, x, y + 2, keyW);
            graphics.drawString(font, action, x + keyW + 3, y + 4, PanelDrawing.LEGEND, false);
        }
        return x + keyW + 3 + font.width(action);
    }

    private static String[] values(MapTools.Land land) {
        int fields = 0;
        int pastures = 0;
        int livestock = 0;
        for (MapPlots.Plot plot : land.plots()) {
            if (plot.kind() == MapPlots.Kind.PASTURE) {
                pastures++;
                livestock += plot.penned();
            } else {
                fields++;
            }
        }
        return new String[] {fields + "/" + land.fieldCap(), pastures + "/" + land.pastureCap(),
                Integer.toString(livestock)};
    }

    boolean contains(double mouseX, double mouseY) {
        for (int i = 0; i < RESOURCES.length; i++) {
            if (Widgets.isOver(mouseX, mouseY, lefts[i], TOP, widths[i], CHIP_H)) return true;
        }
        return isOverHarvest(mouseX, mouseY) || Widgets.isOver(mouseX, mouseY, LEFT, keysY, keysW, CHIP_H);
    }

    void sprites(SpriteBatch sprites, Font font, MapTools.Land land, double mouseX, double mouseY) {
        String[] values = values(land);
        int x = LEFT;
        for (int i = 0; i < RESOURCES.length; i++) {
            lefts[i] = x;
            widths[i] = 4 + 9 + 4 + font.width(values[i]) + 5;
            sprites.blit(PanelDrawing.CHIP, x, TOP, widths[i], CHIP_H);
            sprites.blit(icon(i), x + 4, TOP + 3, 9, 9);
            x += widths[i] + 4;
        }
        harvestX = x + 2;
        harvestW = 3 + 16 + 3 + font.width(harvestLabel(land)) + 6;
        boolean live = ripe(land) > 0;
        PanelDrawing.button(sprites, harvestX, TOP - 2, harvestW, HARVEST_H, live, mouseX, mouseY);
        int sunk = !live || PanelDrawing.pressed(harvestX, TOP - 2, harvestW, HARVEST_H) ? 1 : 0;
        sprites.blit(PanelDrawing.sprite("harvest_all"), harvestX + 3, TOP + sunk, 16, 16,
                live ? 0xFFFFFFFF : PanelDrawing.DIM_ICON);
        keysW = keys(null, null, font) + 6 - LEFT;
        sprites.blit(PanelDrawing.CHIP, LEFT, keysY, keysW, CHIP_H);
        keys(sprites, null, font);
    }

    private static ResourceLocation icon(int index) { return PanelDrawing.sprite("res_" + RESOURCES[index]); }

    void texts(GuiGraphics graphics, Font font, MapTools.Land land) {
        String[] values = values(land);
        for (int i = 0; i < RESOURCES.length; i++) {
            graphics.drawString(font, values[i], lefts[i] + 17, TOP + 4, PanelDrawing.TEXT, false);
        }
        boolean live = ripe(land) > 0;
        int sunk = !live || PanelDrawing.pressed(harvestX, TOP - 2, harvestW, HARVEST_H) ? 1 : 0;
        graphics.drawString(font, harvestLabel(land), harvestX + 22, TOP + 4 + sunk,
                live ? PanelDrawing.TEXT : PanelDrawing.DEAD, false);
        keys(null, graphics, font);
    }

    private int keys(@Nullable SpriteBatch sprites, @Nullable GuiGraphics graphics, Font font) {
        int x = keyPhrase(sprites, graphics, font, Component.translatable(UI + "key.wasd"),
                Component.translatable(UI + "hint.pan"), LEFT + 4, keysY);
        x = keyPhrase(sprites, graphics, font, Component.translatable(UI + "key.wheel"),
                Component.translatable(UI + "hint.zoom"), x + 8, keysY);
        return keyPhrase(sprites, graphics, font, Component.translatable(UI + "key.right"),
                Component.translatable(UI + "hint.teleport"), x + 8, keysY);
    }

    boolean tip(MapPage page, GuiGraphics graphics, MapTools.Land land, int mouseX, int mouseY) {
        if (page.tipAt(graphics, harvestX, TOP - 2, harvestW, HARVEST_H, mouseX, mouseY,
                Component.translatable(EarthSpiritScreen.TIP + "map_harvest_all", ripe(land)))) {
            return true;
        }
        String[] values = values(land);
        for (int i = 0; i < RESOURCES.length; i++) {
            if (page.tipAt(graphics, lefts[i], TOP, widths[i], CHIP_H, mouseX, mouseY,
                    Component.translatable(TIP + RESOURCES[i], values[i]))) {
                return true;
            }
        }
        return false;
    }
}
