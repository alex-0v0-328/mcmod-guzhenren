package net.alex.guzhenren.client.screen.immortalaperture.map;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.gameplay.immortalaperture.mappipeline.MapView;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * 全疆域远景略图：把原正方形疆域整片缩到 {@value #SIZE} 像素见方，供割弃弹窗用。
 * <ul>
 *   <li>已割弃部分画虚空色、现疆域画 {@value #CELL} 像素棋盘格、待割条带盖半透明红与暗红细线（至少 1 像素宽，细线
 *       沿条带短边方向每 {@value #HATCH_PITCH} 像素一条）、地图当前视口画白框、原点区块画山；山图标在填充之后另起一批
 *       画，否则被底色盖住。</li>
 *   <li>{@link #blockAt} 是反向换算：略图像素 → 方块坐标（点略图移地图用）。</li>
 * </ul>
 */

final class TerritorySketch {

    static final int SIZE = 128;
    private static final int CELL = 8;
    private static final int VOID = 0xFF1A1418;
    private static final int LAND_A = 0xFF5C8A46;
    private static final int LAND_B = 0xFF547F40;
    private static final int STRIP = 0x6EFF5555;
    private static final int HATCH = 0xFF8A2C3A;
    private static final int HATCH_PITCH = 3;
    private static final int EDGE = 0xFFFF5555;
    private static final int VIEW = 0xFFFFFFFF;

    private TerritorySketch() {}

    static void draw(GuiGraphics graphics, int left, int top, Territory territory, Territory.@Nullable Side side,
                     int width, MapView view) {
        graphics.fill(left, top, left + SIZE, top + SIZE, VOID);
        int[] land = rect(territory, territory.minChunkX(), territory.maxChunkX() + 1, territory.minChunkZ(),
                territory.maxChunkZ() + 1);
        for (int x = land[0]; x < land[1]; x += CELL) {
            for (int z = land[2]; z < land[3]; z += CELL) {
                boolean even = (x / CELL + z / CELL) % 2 == 0;
                graphics.fill(left + x, top + z, left + Math.min(x + CELL, land[1]), top + Math.min(z + CELL, land[3]),
                        even ? LAND_A : LAND_B);
            }
        }
        if (side != null) drawStrip(graphics, left, top, strip(territory, side, width));
        outline(graphics, left, top, rect(territory, view.minChunkX(), view.maxChunkX() + 1, view.minChunkZ(),
                view.maxChunkZ() + 1), VIEW);
        int[] centre = rect(territory, 0, 1, 0, 1);
        SpriteBatch.draw(graphics, sprites -> sprites.blit(PanelDrawing.sprite("icon_mountain"),
                left + (centre[0] + centre[1]) / 2 - 8, top + (centre[2] + centre[3]) / 2 - 8, 16, 16));
    }

    static double blockAt(Territory territory, double offset) {
        return (offset * 2.0 * territory.half() / SIZE - territory.half()) * 16.0;
    }

    private static int[] strip(Territory territory, Territory.Side side, int width) {
        int[] band = switch (side) {
            case NORTH -> rect(territory, territory.minChunkX(), territory.maxChunkX() + 1, territory.minChunkZ(),
                    territory.minChunkZ() + width);
            case SOUTH -> rect(territory, territory.minChunkX(), territory.maxChunkX() + 1,
                    territory.maxChunkZ() + 1 - width, territory.maxChunkZ() + 1);
            case WEST -> rect(territory, territory.minChunkX(), territory.minChunkX() + width, territory.minChunkZ(),
                    territory.maxChunkZ() + 1);
            case EAST -> rect(territory, territory.maxChunkX() + 1 - width, territory.maxChunkX() + 1,
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
        if (band[1] - band[0] >= band[3] - band[2]) {
            for (int x = band[0] + 1; x < band[1]; x += HATCH_PITCH) {
                graphics.fill(left + x, top + band[2], left + x + 1, top + band[3], HATCH);
            }
        } else {
            for (int z = band[2] + 1; z < band[3]; z += HATCH_PITCH) {
                graphics.fill(left + band[0], top + z, left + band[1], top + z + 1, HATCH);
            }
        }
        outline(graphics, left, top, band, EDGE);
    }

    private static void outline(GuiGraphics graphics, int left, int top, int[] band, int colour) {
        int minX = Math.clamp(band[0], 0, SIZE - 1);
        int maxX = Math.clamp(band[1], minX + 1, SIZE);
        int minZ = Math.clamp(band[2], 0, SIZE - 1);
        int maxZ = Math.clamp(band[3], minZ + 1, SIZE);
        graphics.fill(left + minX, top + minZ, left + maxX, top + minZ + 1, colour);
        graphics.fill(left + minX, top + maxZ - 1, left + maxX, top + maxZ, colour);
        graphics.fill(left + minX, top + minZ, left + minX + 1, top + maxZ, colour);
        graphics.fill(left + maxX - 1, top + minZ, left + maxX, top + maxZ, colour);
    }

    private static int[] rect(Territory territory, int minX, int maxX, int minZ, int maxZ) {
        double scale = (double) SIZE / (2 * territory.half());
        return new int[] { (int) Math.floor((minX + territory.half()) * scale),
                (int) Math.ceil((maxX + territory.half()) * scale), (int) Math.floor((minZ + territory.half()) * scale),
                (int) Math.ceil((maxZ + territory.half()) * scale) };
    }
}
