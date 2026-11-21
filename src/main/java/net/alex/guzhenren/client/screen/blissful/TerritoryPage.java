package net.alex.guzhenren.client.screen.blissful;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.gameplay.blissful.PanelAction;
import net.alex.guzhenren.gameplay.blissful.PanelSnapshot;
import net.alex.guzhenren.gameplay.blissful.Territory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 「疆域」页：略图、亩数、方向键与条带宽度、割弃预告，「割弃」弹出二次确认。
 * <ul>
 *   <li>略图把原正方形整片缩到 {@value #MAP} 像素见方：已割弃的部分画成虚空色，现疆域画棋盘格，待割弃条带盖半透明红与暗
 *       红细线（至少 1 像素宽；细线沿条带短边方向每 {@value #HATCH_PITCH} 像素一条，整条画，不逐像素画斜线），原点区块
 *       处画山；山图标在填充之后另起一批画，否则被底色盖住。</li>
 *   <li>右栏的区块尺寸写成「宽×高」，四位数边长后面放不下「区块」二字，只好不写单位。</li>
 *   <li>宽度在 1 到该侧还能割的最大值之间；该侧已到中心 64×64 区块时预告换成不可割，「割弃」失效。</li>
 *   <li>宽度 −/+ 每次改 1，按住 Shift 改 {@value #SHIFT_STEP}。疆域快照用 {@link Territory} 复算，规则与服务端同一
 *       份。</li>
 * </ul>
 */

final class TerritoryPage extends PanelPage {

    private static final String UI = EarthSpiritScreen.UI;
    private static final int MAP = 96;
    private static final int MAP_X = 9;
    private static final int MAP_Y = 21;
    private static final int CELL = 8;
    private static final int SHIFT_STEP = 10;
    private static final int VOID = 0xFF1A1418;
    private static final int LAND_A = 0xFF5C8A46;
    private static final int LAND_B = 0xFF547F40;
    private static final int STRIP = 0x6EFF5555;
    private static final int HATCH = 0xFF8A2C3A;
    private static final int HATCH_PITCH = 3;
    private static final int EDGE = 0xFFFF5555;
    private static final int DIM = 0x96000000;
    private static final int POPUP_RULE = 0xFFC84B55;
    private static final int[][] ARROWS = {{131, 56}, {149, 74}, {131, 92}, {113, 74}};
    private static final String[] ARROW_KEYS = {"north", "east", "south", "west"};
    private static final int ARROW = 16;
    private static final int MINUS_X = 48;
    private static final int PLUS_X = 78;
    private static final int WIDTH_Y = 122;
    private static final int CEDE_X = 98;
    private static final int CEDE_Y = 192;
    private static final int CEDE_W = 70;
    private static final int CEDE_H = 20;
    private static final int POPUP_X = 13;
    private static final int POPUP_Y = 70;
    private static final int POPUP_W = 150;
    private static final int POPUP_H = 72;
    private static final int CANCEL_X = 21;
    private static final int CONFIRM_X = 91;
    private static final int POPUP_BUTTON_Y = 119;
    private static final int POPUP_BUTTON_W = 64;
    private static final int POPUP_BUTTON_H = 18;
    private Territory.Side side = Territory.Side.EAST;
    private int width = 1;
    private boolean confirming;

    TerritoryPage(EarthSpiritScreen screen) { super(screen); }

    @Override
    Component name() { return Component.translatable(UI + "tab.territory"); }

    private Territory territory() {
        PanelSnapshot snapshot = snapshot();
        return new Territory(snapshot.half(), snapshot.core(), snapshot.north(), snapshot.east(), snapshot.south(),
                snapshot.west());
    }

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

    private Component sideName() {
        return Component.translatable("guzhenren.blissful_land.side." + ARROW_KEYS[side.ordinal()]);
    }

    @Override
    void sprites(SpriteBatch sprites, int x, int y, int mouseX, int mouseY) {
        sprites.blit(PanelKit.WELL, x + 8, y + 20, 98, 98);
        for (Territory.Side each : Territory.Side.values()) {
            int[] at = ARROWS[each.ordinal()];
            boolean hover = Widgets.isOver(mouseX, mouseY, x + at[0], y + at[1], ARROW, ARROW);
            PanelKit.button(sprites, x + at[0], y + at[1], ARROW, ARROW, true, hover, each == side, false);
        }
        PanelKit.button(sprites, x + MINUS_X, y + WIDTH_Y, PanelKit.SQUARE, PanelKit.SQUARE, true, mouseX, mouseY);
        PanelKit.button(sprites, x + PLUS_X, y + WIDTH_Y, PanelKit.SQUARE, PanelKit.SQUARE, true, mouseX, mouseY);
        boolean live = canCede();
        boolean hover = live && Widgets.isOver(mouseX, mouseY, x + CEDE_X, y + CEDE_Y, CEDE_W, CEDE_H);
        PanelKit.button(sprites, x + CEDE_X, y + CEDE_Y, CEDE_W, CEDE_H, live, hover, false, true);
    }

    @Override
    void fills(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        Territory territory = territory();
        int left = x + MAP_X;
        int top = y + MAP_Y;
        graphics.fill(left, top, left + MAP, top + MAP, VOID);
        int[] land = rect(territory, territory.minChunkX(), territory.maxChunkX() + 1, territory.minChunkZ(),
                territory.maxChunkZ() + 1);
        for (int cx = land[0]; cx < land[1]; cx += CELL) {
            for (int cz = land[2]; cz < land[3]; cz += CELL) {
                boolean even = ((cx / CELL) + (cz / CELL)) % 2 == 0;
                graphics.fill(left + cx, top + cz, left + Math.min(cx + CELL, land[1]), top + Math.min(cz + CELL,
                        land[3]), even ? LAND_A : LAND_B);
            }
        }
        if (canCede()) drawStrip(graphics, left, top, strip(territory));
        int[] centre = rect(territory, 0, 1, 0, 1);
        SpriteBatch.draw(graphics, sprites -> sprites.blit(PanelKit.sprite("icon_mountain"),
                left + (centre[0] + centre[1]) / 2 - 8, top + (centre[2] + centre[3]) / 2 - 8, 16, 16));
    }

    private int[] strip(Territory territory) {
        int w = clampedWidth();
        int[] band = switch (side) {
            case NORTH -> rect(territory, territory.minChunkX(), territory.maxChunkX() + 1, territory.minChunkZ(),
                    territory.minChunkZ() + w);
            case SOUTH -> rect(territory, territory.minChunkX(), territory.maxChunkX() + 1,
                    territory.maxChunkZ() + 1 - w, territory.maxChunkZ() + 1);
            case WEST -> rect(territory, territory.minChunkX(), territory.minChunkX() + w, territory.minChunkZ(),
                    territory.maxChunkZ() + 1);
            case EAST -> rect(territory, territory.maxChunkX() + 1 - w, territory.maxChunkX() + 1,
                    territory.minChunkZ(), territory.maxChunkZ() + 1);
        };
        switch (side) {
            case NORTH -> band[3] = Math.max(band[3], band[2] + 1);
            case SOUTH -> band[2] = Math.min(band[2], band[3] - 1);
            case WEST -> band[1] = Math.max(band[1], band[0] + 1);
            case EAST -> band[0] = Math.min(band[0], band[1] - 1);
        }
        return band;
    }

    private static void drawStrip(GuiGraphics graphics, int left, int top, int[] band) {
        graphics.fill(left + band[0], top + band[2], left + band[1], top + band[3], STRIP);
        boolean across = band[1] - band[0] >= band[3] - band[2];
        if (across) {
            for (int px = band[0] + 1; px < band[1]; px += HATCH_PITCH) {
                graphics.fill(left + px, top + band[2], left + px + 1, top + band[3], HATCH);
            }
        } else {
            for (int pz = band[2] + 1; pz < band[3]; pz += HATCH_PITCH) {
                graphics.fill(left + band[0], top + pz, left + band[1], top + pz + 1, HATCH);
            }
        }
        graphics.fill(left + band[0], top + band[2], left + band[1], top + band[2] + 1, EDGE);
        graphics.fill(left + band[0], top + band[3] - 1, left + band[1], top + band[3], EDGE);
        graphics.fill(left + band[0], top + band[2], left + band[0] + 1, top + band[3], EDGE);
        graphics.fill(left + band[1] - 1, top + band[2], left + band[1], top + band[3], EDGE);
    }

    private static int[] rect(Territory territory, int minX, int maxX, int minZ, int maxZ) {
        double scale = (double) MAP / (2 * territory.half());
        return new int[] {(int) Math.floor((minX + territory.half()) * scale),
                (int) Math.ceil((maxX + territory.half()) * scale), (int) Math.floor((minZ + territory.half()) * scale),
                (int) Math.ceil((maxZ + territory.half()) * scale)};
    }

    @Override
    void texts(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        Territory territory = territory();
        graphics.drawString(font(), Component.translatable(UI + "acres"), x + 110, y + 22, PanelKit.LEGEND, false);
        graphics.drawString(font(), Long.toString(territory.acres()), x + 110, y + 32, PanelKit.TEXT, false);
        graphics.drawString(font(), (territory.maxChunkX() - territory.minChunkX() + 1) + "×"
                + (territory.maxChunkZ() - territory.minChunkZ() + 1), x + 110, y + 42, PanelKit.LEGEND, false);
        for (Territory.Side each : Territory.Side.values()) {
            int[] at = ARROWS[each.ordinal()];
            Component label = Component.translatable("guzhenren.blissful_land.side." + ARROW_KEYS[each.ordinal()]);
            graphics.drawString(font(), label, x + at[0] + (ARROW - font().width(label)) / 2,
                    y + at[1] + (ARROW - font().lineHeight) / 2 + 1 + (each == side ? 1 : 0),
                    each == side ? PanelKit.ACCENT : PanelKit.TEXT, false);
        }
        graphics.drawString(font(), Component.translatable(UI + "strip_width"), x + 8, y + 126, PanelKit.LEGEND,
                false);
        PanelKit.label(graphics, font(), Component.literal("-"), x + MINUS_X, y + WIDTH_Y, PanelKit.SQUARE,
                PanelKit.SQUARE, true);
        PanelKit.centered(graphics, font(), Component.literal(Integer.toString(clampedWidth())), x + 62, x + 77,
                y + 126, PanelKit.TEXT);
        PanelKit.label(graphics, font(), Component.literal("+"), x + PLUS_X, y + WIDTH_Y, PanelKit.SQUARE,
                PanelKit.SQUARE, true);
        graphics.drawString(font(), Component.translatable(UI + "chunk"), x + 96, y + 126, PanelKit.LEGEND, false);
        Component preview = canCede()
                ? Component.translatable(UI + "cede_preview", sideName(), clampedWidth(), pendingAcres())
                : Component.translatable(UI + "cede_core", sideName());
        graphics.drawString(font(), preview, x + 8, y + 144, PanelKit.WARN, false);
        graphics.drawString(font(), Component.translatable(UI + "cede_note"), x + 8, y + 156, PanelKit.LEGEND, false);
        PanelKit.label(graphics, font(), Component.translatable(UI + "cede"), x + CEDE_X, y + CEDE_Y, CEDE_W, CEDE_H,
                canCede());
    }

    @Override
    boolean click(int x, int y, double mouseX, double mouseY, int button, boolean shift) {
        for (Territory.Side each : Territory.Side.values()) {
            int[] at = ARROWS[each.ordinal()];
            if (Widgets.isOver(mouseX, mouseY, x + at[0], y + at[1], ARROW, ARROW)) {
                side = each;
                return true;
            }
        }
        int step = shift ? SHIFT_STEP : 1;
        if (Widgets.isOver(mouseX, mouseY, x + MINUS_X, y + WIDTH_Y, PanelKit.SQUARE, PanelKit.SQUARE)) {
            width = clampedWidth() - step;
            clampedWidth();
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + PLUS_X, y + WIDTH_Y, PanelKit.SQUARE, PanelKit.SQUARE)) {
            width = clampedWidth() + step;
            clampedWidth();
            return true;
        }
        if (Widgets.isOver(mouseX, mouseY, x + CEDE_X, y + CEDE_Y, CEDE_W, CEDE_H)) {
            if (canCede()) confirming = true;
            return true;
        }
        return false;
    }

    //region Confirm [二次确认]
    @Override
    boolean hasOverlay() { return confirming; }

    @Override
    void closeOverlay() { confirming = false; }

    @Override
    void overlay(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        graphics.fill(0, 0, screen.width, screen.height, DIM);
        SpriteBatch.draw(graphics, sprites -> {
            sprites.blit(PanelKit.POPUP, x + POPUP_X, y + POPUP_Y, POPUP_W + 2, POPUP_H + 2);
            PanelKit.button(sprites, x + CANCEL_X, y + POPUP_BUTTON_Y, POPUP_BUTTON_W, POPUP_BUTTON_H, true, mouseX,
                    mouseY);
            boolean hover = Widgets.isOver(mouseX, mouseY, x + CONFIRM_X, y + POPUP_BUTTON_Y, POPUP_BUTTON_W,
                    POPUP_BUTTON_H);
            PanelKit.button(sprites, x + CONFIRM_X, y + POPUP_BUTTON_Y, POPUP_BUTTON_W, POPUP_BUTTON_H, true, hover,
                    false, true);
        });
        graphics.drawString(font(), Component.translatable(UI + "confirm_title"), x + 21, y + 78, PanelKit.WARN,
                false);
        graphics.fill(x + 20, y + 88, x + 155, y + 89, POPUP_RULE);
        graphics.drawString(font(), Component.translatable(UI + "confirm_body", sideName(), clampedWidth(),
                pendingAcres()), x + 21, y + 94, PanelKit.TEXT, false);
        graphics.drawString(font(), Component.translatable(UI + "confirm_note"), x + 21, y + 105, PanelKit.LEGEND,
                false);
        PanelKit.label(graphics, font(), Component.translatable(UI + "cancel"), x + CANCEL_X, y + POPUP_BUTTON_Y,
                POPUP_BUTTON_W, POPUP_BUTTON_H, true);
        PanelKit.label(graphics, font(), Component.translatable(UI + "confirm_title"), x + CONFIRM_X,
                y + POPUP_BUTTON_Y, POPUP_BUTTON_W, POPUP_BUTTON_H, true);
    }

    @Override
    void overlayClick(int x, int y, double mouseX, double mouseY) {
        if (Widgets.isOver(mouseX, mouseY, x + CONFIRM_X, y + POPUP_BUTTON_Y, POPUP_BUTTON_W, POPUP_BUTTON_H)) {
            if (canCede()) screen.send(PanelAction.CEDE, clampedWidth(), side.name());
            confirming = false;
        } else if (Widgets.isOver(mouseX, mouseY, x + CANCEL_X, y + POPUP_BUTTON_Y, POPUP_BUTTON_W, POPUP_BUTTON_H)) {
            confirming = false;
        }
    }
    //endregion
}
