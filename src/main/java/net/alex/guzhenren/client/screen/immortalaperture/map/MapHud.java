package net.alex.guzhenren.client.screen.immortalaperture.map;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页左上的资源条（{@code chip}）：田已用 / 上限含规划、牧场已用 / 上限、各牧场栏内牲畜合计；以及底栏上方的按键提示条
 * {@link #hintChip}（浏览时写「[WASD] 平移 [滚轮] 缩放 [右键] 传送」，选中田时写选区按键，Alex 定统一放底栏上方）。
 * <ul>
 *   <li>Alex 定：资源条只写字（灰标签 + 白读数）、不放图标与「栏外」警报；详细说明走悬停提示。一键收获挪进工
 *       具栏的统筹组（{@link MapTools}）。</li>
 *   <li>{@link #keyPhrase} 画「键帽 + 灰字」短语，各提示条都用它；画图与写字两步各调一次，传空的那一侧跳过。</li>
 * </ul>
 */

final class MapHud {

    static final int CHIP_H = 16;
    private static final int TOP = EarthSpiritScreen.BAR_H + 6;
    private static final int LEFT = 6;
    private static final String UI = EarthSpiritScreen.UI + "map.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_res_";
    private static final String[] RESOURCES = {"farm", "pasture", "livestock"};
    private final int[] lefts = new int[RESOURCES.length];
    private final int[] widths = new int[RESOURCES.length];

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

    /** 一枚居中于 {@code centerX} 的按键提示条，{@code phrases} 每项是 {键名, 提示名}（{@code map.key.} / {@code map.hint.}）。 */
    static void hintChip(@Nullable SpriteBatch sprites, @Nullable GuiGraphics graphics, Font font, String[][] phrases,
                         int centerX, int y) {
        int width = 6;
        for (String[] phrase : phrases) {
            width += PanelDrawing.keycapWidth(font, key(phrase[0])) + 3 + font.width(hint(phrase[1])) + 8;
        }
        width -= 2;
        int x = centerX - width / 2;
        if (sprites != null) sprites.blit(PanelDrawing.CHIP, x, y, width, CHIP_H);
        x += 6;
        for (String[] phrase : phrases) {
            x = keyPhrase(sprites, graphics, font, key(phrase[0]), hint(phrase[1]), x, y) + 8;
        }
    }

    private static Component key(String name) { return Component.translatable(UI + "key." + name); }

    private static Component hint(String name) { return Component.translatable(UI + "hint." + name); }

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
        return false;
    }

    void sprites(SpriteBatch sprites, Font font, MapTools.Land land) {
        String[] values = values(land);
        int x = LEFT;
        for (int i = 0; i < RESOURCES.length; i++) {
            lefts[i] = x;
            widths[i] = 5 + font.width(label(i)) + 3 + font.width(values[i]) + 5;
            sprites.blit(PanelDrawing.CHIP, x, TOP, widths[i], CHIP_H);
            x += widths[i] + 4;
        }
    }

    private static Component label(int index) { return Component.translatable(UI + "res." + RESOURCES[index]); }

    void texts(GuiGraphics graphics, Font font, MapTools.Land land) {
        String[] values = values(land);
        for (int i = 0; i < RESOURCES.length; i++) {
            Component label = label(i);
            graphics.drawString(font, label, lefts[i] + 5, TOP + 4, PanelDrawing.LEGEND, false);
            graphics.drawString(font, values[i], lefts[i] + 5 + font.width(label) + 3, TOP + 4, PanelDrawing.TEXT,
                    false);
        }
    }

    boolean tip(MapPage page, GuiGraphics graphics, MapTools.Land land, int mouseX, int mouseY) {
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
