package net.alex.guzhenren.client.screen.immortalaperture.map;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.alex.guzhenren.gameplay.immortalaperture.territory.TerritoryModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 地图页的割弃弹窗：左边是全疆域远景略图（{@link TerritorySketch}），右边选侧、条带宽度与「割弃」，再弹一层二次确认
 * 才发 {@link TerritoryModule.Cede}。
 * <ul>
 *   <li>疆域远大于地图一次能看的 64×64 区块，所以割弃在远景略图上做；点略图把地图视口移到那里。</li>
 *   <li>宽度在 1 到该侧还能割的最大值之间，−/+ 每次 1、按住 Shift 每次 {@value #SHIFT_STEP}；该侧已到中心 64×64 区块时
 *       预告换成不可割、「割弃」失效。疆域用 {@link Territory} 复算，规则与服务端同一份。</li>
 *   <li>确认层另列条带内将一并作废的田、规划田、牧场数量（{@link MapPlots#voided}，牧场任一区块落进条带即算）。</li>
 *   <li>坐标都是屏幕坐标，弹窗在地图区（顶栏以下）居中；Esc 先关确认层、再关弹窗。</li>
 * </ul>
 */

final class CedePanel {

    private static final String UI = EarthSpiritScreen.UI;
    private static final String TIP = EarthSpiritScreen.TIP;
    private static final int WIDTH = 264;
    private static final int HEIGHT = 190;
    private static final int SHIFT_STEP = 10;
    private static final int MAP = TerritorySketch.SIZE;
    private static final int DIM = 0x96000000;
    private static final int POPUP_RULE = 0xFFC84B55;
    private static final int[][] ARROWS = { { 30, 60 }, { 48, 78 }, { 30, 96 }, { 12, 78 } };
    private static final String[] SIDE_KEYS = { "north", "east", "south", "west" };
    private static final int ARROW = 16;
    private static final int CONFIRM_W = 200;
    private static final int CONFIRM_H = 84;
    private static final int BUTTON_W = 50;
    private static final int BUTTON_H = 16;
    private final MapPage page;
    private boolean open;
    private boolean confirming;
    private Territory.Side side = Territory.Side.EAST;
    private int width = 1;
    private int left;
    private int top;
    private int screenWidth;
    private int screenHeight;

    CedePanel(MapPage page) { this.page = page; }

    boolean isOpen() { return open; }

    void open() {
        open = true;
        confirming = false;
    }

    void close() {
        if (confirming) {
            confirming = false;
            return;
        }
        open = false;
    }

    void closeAll() {
        open = false;
        confirming = false;
    }

    void layout(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        left = (screenWidth - WIDTH) / 2;
        top = EarthSpiritScreen.BAR_H + 1 + (screenHeight - EarthSpiritScreen.BAR_H - 1 - HEIGHT) / 2;
    }

    //region State [状态]
    private Territory territory() { return page.data().territory(); }

    private int clampedWidth() {
        int max = territory().maxCede(side);
        width = Math.max(1, Math.min(width, Math.max(1, max)));
        return width;
    }

    private boolean canCede() { return territory().canCede(side, clampedWidth()); }

    private long pendingAcres() {
        Territory territory = territory();
        int span = side == Territory.Side.EAST || side == Territory.Side.WEST
                ? territory.maxChunkZ() - territory.minChunkZ() + 1
                : territory.maxChunkX() - territory.minChunkX() + 1;
        return (long) clampedWidth() * span * Territory.ACRES_PER_CHUNK;
    }

    private Component sideName() { return Component.translatable("guzhenren.blissful_land.side." + key(side)); }

    private static String key(Territory.Side each) { return SIDE_KEYS[each.ordinal()]; }
    //endregion

    //region Drawing [绘制]
    void draw(GuiGraphics graphics, Font font, MapCamera camera, int mouseX, int mouseY) {
        graphics.fill(0, 0, screenWidth, screenHeight, DIM);
        int column = left + 148;
        SpriteBatch.draw(graphics, sprites -> {
            sprites.blit(PanelDrawing.POPUP, left, top, WIDTH, HEIGHT);
            sprites.blit(PanelDrawing.WELL, left + 9, top + 24, MAP + 2, MAP + 2);
            for (Territory.Side each : Territory.Side.values()) {
                int[] at = ARROWS[each.ordinal()];
                boolean hover = !confirming && Widgets.isOver(mouseX, mouseY, column + at[0], top + at[1], ARROW,
                        ARROW);
                PanelDrawing.button(sprites, column + at[0], top + at[1], ARROW, ARROW, true, hover, each == side,
                        false);
            }
            PanelDrawing.button(sprites, column, top + 128, PanelDrawing.SQUARE, PanelDrawing.SQUARE, true, mouseX,
                    mouseY);
            PanelDrawing.button(sprites, column + 44, top + 128, PanelDrawing.SQUARE, PanelDrawing.SQUARE, true,
                    mouseX, mouseY);
            PanelDrawing.button(sprites, column, top + 142, BUTTON_W, BUTTON_H, true, mouseX, mouseY);
            boolean live = canCede();
            boolean hover = live && Widgets.isOver(mouseX, mouseY, column + 56, top + 142, BUTTON_W, BUTTON_H);
            PanelDrawing.button(sprites, column + 56, top + 142, BUTTON_W, BUTTON_H, live, hover, false, true);
        });
        graphics.fill(left + 9, top + 19, left + WIDTH - 9, top + 20, PanelDrawing.ACCENT);
        TerritorySketch.draw(graphics, left + 10, top + 25, territory(), canCede() ? side : null, clampedWidth(),
                camera.viewChunks());
        texts(graphics, font, column);
        if (confirming) drawConfirm(graphics, font, mouseX, mouseY);
    }

    private void texts(GuiGraphics graphics, Font font, int column) {
        Territory territory = territory();
        graphics.drawString(font, Component.translatable(UI + "map.cede_title"), left + 10, top + 8,
                PanelDrawing.ACCENT, false);
        graphics.drawString(font, Component.translatable(UI + "acres"), column, top + 26, PanelDrawing.LEGEND, false);
        graphics.drawString(font, Long.toString(territory.acres()), column, top + 36, PanelDrawing.TEXT, false);
        graphics.drawString(font, (territory.maxChunkX() - territory.minChunkX() + 1) + "×"
                + (territory.maxChunkZ() - territory.minChunkZ() + 1), column, top + 46, PanelDrawing.LEGEND, false);
        for (Territory.Side each : Territory.Side.values()) {
            int[] at = ARROWS[each.ordinal()];
            Component label = Component.translatable("guzhenren.blissful_land.side." + key(each));
            graphics.drawString(font, label, column + at[0] + (ARROW - font.width(label)) / 2,
                    top + at[1] + (ARROW - font.lineHeight) / 2 + 1 + (each == side ? 1 : 0),
                    each == side ? PanelDrawing.ACCENT : PanelDrawing.TEXT, false);
        }
        graphics.drawString(font, Component.translatable(UI + "strip_width"), column, top + 117, PanelDrawing.LEGEND,
                false);
        PanelDrawing.label(graphics, font, Component.literal("-"), column, top + 128, PanelDrawing.SQUARE,
                PanelDrawing.SQUARE, true);
        PanelDrawing.centered(graphics, font, Component.literal(Integer.toString(clampedWidth())), column + 14,
                column + 44, top + 131, PanelDrawing.TEXT);
        PanelDrawing.label(graphics, font, Component.literal("+"), column + 44, top + 128, PanelDrawing.SQUARE,
                PanelDrawing.SQUARE, true);
        graphics.drawString(font, Component.translatable(UI + "chunk"), column + 62, top + 131, PanelDrawing.LEGEND,
                false);
        PanelDrawing.label(graphics, font, Component.translatable(UI + "map.close"), column, top + 142, BUTTON_W,
                BUTTON_H, true);
        PanelDrawing.label(graphics, font, Component.translatable(UI + "cede"), column + 56, top + 142, BUTTON_W,
                BUTTON_H, canCede());
        Component preview = canCede()
                ? Component.translatable(UI + "cede_preview", sideName(), clampedWidth(), pendingAcres())
                : Component.translatable(UI + "cede_core", sideName());
        graphics.drawString(font, preview, left + 10, top + 162, PanelDrawing.WARN, false);
        graphics.drawString(font, Component.translatable(UI + "cede_note"), left + 10, top + 174, PanelDrawing.LEGEND,
                false);
    }

    private void drawConfirm(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int boxLeft = left + (WIDTH - CONFIRM_W) / 2;
        int boxTop = top + (HEIGHT - CONFIRM_H) / 2;
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, DIM);
        SpriteBatch.draw(graphics, sprites -> {
            sprites.blit(PanelDrawing.POPUP, boxLeft, boxTop, CONFIRM_W, CONFIRM_H);
            PanelDrawing.button(sprites, boxLeft + 10, boxTop + CONFIRM_H - 24, 70, BUTTON_H, true, mouseX, mouseY);
            boolean hover = Widgets.isOver(mouseX, mouseY, boxLeft + CONFIRM_W - 80, boxTop + CONFIRM_H - 24, 70,
                    BUTTON_H);
            PanelDrawing.button(sprites, boxLeft + CONFIRM_W - 80, boxTop + CONFIRM_H - 24, 70, BUTTON_H, true, hover,
                    false, true);
        });
        graphics.drawString(font, Component.translatable(UI + "confirm_title"), boxLeft + 10, boxTop + 8,
                PanelDrawing.WARN, false);
        graphics.fill(boxLeft + 9, boxTop + 18, boxLeft + CONFIRM_W - 9, boxTop + 19, POPUP_RULE);
        graphics.drawString(font, Component.translatable(UI + "confirm_body", sideName(), clampedWidth(),
                pendingAcres()), boxLeft + 10, boxTop + 24, PanelDrawing.TEXT, false);
        MapPlots.Voided voided = MapPlots.voided(page.land().plots(), territory(), side, clampedWidth());
        if (!voided.isEmpty()) {
            graphics.drawString(font, Component.translatable(UI + "map.cede_voids", voided.fields(), voided.planned(),
                    voided.pastures()), boxLeft + 10, boxTop + 36, PanelDrawing.WARN, false);
        }
        graphics.drawString(font, Component.translatable(UI + "confirm_note"), boxLeft + 10, boxTop + 48,
                PanelDrawing.LEGEND, false);
        PanelDrawing.label(graphics, font, Component.translatable(UI + "cancel"), boxLeft + 10, boxTop + CONFIRM_H - 24,
                70, BUTTON_H, true);
        PanelDrawing.label(graphics, font, Component.translatable(UI + "confirm_title"), boxLeft + CONFIRM_W - 80,
                boxTop + CONFIRM_H - 24, 70, BUTTON_H, true);
    }
    //endregion

    //region Input [输入]
    void click(MapCamera camera, double mouseX, double mouseY, boolean shift) {
        if (confirming) {
            clickConfirm(mouseX, mouseY);
            return;
        }
        int column = left + 148;
        for (Territory.Side each : Territory.Side.values()) {
            int[] at = ARROWS[each.ordinal()];
            if (Widgets.isOver(mouseX, mouseY, column + at[0], top + at[1], ARROW, ARROW)) {
                side = each;
                return;
            }
        }
        int step = shift ? SHIFT_STEP : 1;
        if (Widgets.isOver(mouseX, mouseY, column, top + 128, PanelDrawing.SQUARE, PanelDrawing.SQUARE)) {
            width = clampedWidth() - step;
            clampedWidth();
        } else if (Widgets.isOver(mouseX, mouseY, column + 44, top + 128, PanelDrawing.SQUARE, PanelDrawing.SQUARE)) {
            width = clampedWidth() + step;
            clampedWidth();
        } else if (Widgets.isOver(mouseX, mouseY, column, top + 142, BUTTON_W, BUTTON_H)) {
            open = false;
        } else if (Widgets.isOver(mouseX, mouseY, column + 56, top + 142, BUTTON_W, BUTTON_H)) {
            if (canCede()) confirming = true;
        } else if (Widgets.isOver(mouseX, mouseY, left + 10, top + 25, MAP, MAP)) {
            camera.setCenter(TerritorySketch.blockAt(territory(), mouseX - left - 10), camera.y(),
                    TerritorySketch.blockAt(territory(), mouseY - top - 25));
        }
    }

    private void clickConfirm(double mouseX, double mouseY) {
        int boxLeft = left + (WIDTH - CONFIRM_W) / 2;
        int boxTop = top + (HEIGHT - CONFIRM_H) / 2;
        int buttonTop = boxTop + CONFIRM_H - 24;
        if (Widgets.isOver(mouseX, mouseY, boxLeft + CONFIRM_W - 80, buttonTop, 70, BUTTON_H)) {
            if (canCede()) page.sendAction(new TerritoryModule.Cede(side, clampedWidth()));
            closeAll();
        } else if (Widgets.isOver(mouseX, mouseY, boxLeft + 10, buttonTop, 70, BUTTON_H)) {
            confirming = false;
        }
    }

    boolean tip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (confirming) return false;
        int column = left + 148;
        for (Territory.Side each : Territory.Side.values()) {
            int[] at = ARROWS[each.ordinal()];
            if (page.tipAt(graphics, column + at[0], top + at[1], ARROW, ARROW, mouseX, mouseY,
                    Component.translatable(TIP + "side"))) {
                return true;
            }
        }
        if (page.tipAt(graphics, column, top + 128, 44 + PanelDrawing.SQUARE, PanelDrawing.SQUARE, mouseX, mouseY,
                Component.translatable(TIP + "strip_width", SHIFT_STEP))) {
            return true;
        }
        if (page.tipAt(graphics, left + 10, top + 25, MAP, MAP, mouseX, mouseY,
                Component.translatable(TIP + "map_sketch"))) {
            return true;
        }
        return page.tipAt(graphics, column + 56, top + 142, BUTTON_W, BUTTON_H, mouseX, mouseY,
                Component.translatable(TIP + "cede"));
    }
    //endregion
}
