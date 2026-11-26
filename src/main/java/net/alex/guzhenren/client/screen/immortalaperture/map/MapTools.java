package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.List;
import java.util.Locale;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页的选址工具条：开垦（1 区块一片田）、牧场（2×2 区块）、拆除、迁移；按住哪个工具，光标下就预览那块占地，合格
 * 描绿、不合格描红并在提示里写原因，左键执行，右键或 Esc 退出工具。
 * <ul>
 *   <li>占地数据、预判与发操作都经 {@link Land}（由 {@link MapToolLand} 用 {@code MapModule.Section} 与 {@code Siting}
 *       实现），本类只管工具状态、目标换算、绘制与命中。</li>
 *   <li>迁移分两步：先点要迁的牧场，再点新址；预判时忽略这个牧场自己的 4 个区块。</li>
 *   <li>田的轮廓描整区块，牧场描栅栏所在的 30×30 周界（{@link Siting#penMin} 到 {@link Siting#penMax}）。</li>
 *   <li>按钮条在视角按钮条上方一行，右边写田与牧场的已用 / 上限（田含规划中的）。</li>
 * </ul>
 */

final class MapTools {

    private static final int GAP = 2;
    private static final int BUTTON_W = 30;
    private static final int ROW_GAP = 4;
    private static final int MARGIN = 6;
    private static final String UI = EarthSpiritScreen.UI + "map.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_tool_";
    private static final int NONE = -1;

    enum Tool { FARM, PASTURE, REMOVE, MOVE }

    /** 预判结果：不合格时 {@code reason} 写给玩家看。 */
    record Verdict(boolean fit, Component reason) {

        static final Verdict FIT = new Verdict(true, Component.empty());
    }

    /** 光标下的工具目标：以 {@code size}×{@code size} 区块计，{@code plot} 是被点中的已有占地（拆除、迁移第一步）。 */
    record Target(int chunkX, int chunkZ, int size, Verdict verdict, MapPlots.@Nullable Plot plot) {}

    /** 地图页提供给工具条与对象卡片的经营数据与操作。 */
    interface Land {

        List<MapPlots.Plot> plots();

        int fieldCap();

        int pastureCap();

        int gatherSeconds();

        Verdict farm(int chunkX, int chunkZ);

        Verdict pasture(int chunkX, int chunkZ, int ignoredIndex);

        void plan(int chunkX, int chunkZ);

        void unplan(int chunkX, int chunkZ);

        void remove(int chunkX, int chunkZ);

        void placePasture(int chunkX, int chunkZ);

        void movePasture(int index, int chunkX, int chunkZ);

        void removePasture(int index);

        void gather(int index);
    }

    private @Nullable Tool tool;
    private int moving = NONE;
    private int top;

    void layout(int controlsTop) { top = controlsTop - ROW_GAP - MapControls.HEIGHT; }

    @Nullable Tool active() { return tool; }

    void cancel() {
        tool = null;
        moving = NONE;
    }

    private int left(Tool each) { return MARGIN + each.ordinal() * (BUTTON_W + GAP); }

    @Nullable Tool at(double mouseX, double mouseY) {
        for (Tool each : Tool.values()) {
            if (Widgets.isOver(mouseX, mouseY, left(each), top, BUTTON_W, MapControls.HEIGHT)) return each;
        }
        return null;
    }

    void press(Tool each) {
        moving = NONE;
        tool = tool == each ? null : each;
    }

    void startMove(int index) {
        tool = Tool.MOVE;
        moving = index;
    }

    //region Targets [目标]
    @Nullable Target target(Land land, MapPick.@Nullable Hit hover) {
        if (tool == null || hover == null) return null;
        int chunkX = hover.x() >> 4;
        int chunkZ = hover.z() >> 4;
        MapPlots.Plot plot = MapPlots.at(land.plots(), chunkX, chunkZ);
        return switch (tool) {
            case FARM -> new Target(chunkX, chunkZ, 1, land.farm(chunkX, chunkZ), null);
            case PASTURE -> pasture(land, hover, NONE);
            case REMOVE -> plot == null ? null : new Target(plot.chunkX(), plot.chunkZ(), plot.size(),
                    Verdict.FIT, plot);
            case MOVE -> moving != NONE ? pasture(land, hover, moving)
                    : plot == null || plot.kind() != MapPlots.Kind.PASTURE ? null
                    : new Target(plot.chunkX(), plot.chunkZ(), 2, Verdict.FIT, plot);
        };
    }

    private static Target pasture(Land land, MapPick.Hit hover, int ignored) {
        int chunkX = MapPlots.pastureCorner(hover.x() + 0.5);
        int chunkZ = MapPlots.pastureCorner(hover.z() + 0.5);
        return new Target(chunkX, chunkZ, 2, land.pasture(chunkX, chunkZ, ignored), null);
    }

    void apply(Land land, Target target) {
        if (tool == null) return;
        MapPlots.Plot plot = target.plot();
        switch (tool) {
            case FARM -> {
                if (target.verdict().fit()) land.plan(target.chunkX(), target.chunkZ());
            }
            case PASTURE -> {
                if (target.verdict().fit()) land.placePasture(target.chunkX(), target.chunkZ());
            }
            case REMOVE -> {
                if (plot != null) remove(land, plot);
            }
            case MOVE -> {
                if (moving == NONE && plot != null) {
                    moving = plot.index();
                } else if (moving != NONE && target.verdict().fit()) {
                    land.movePasture(moving, target.chunkX(), target.chunkZ());
                    cancel();
                }
            }
        }
    }

    static void remove(Land land, MapPlots.Plot plot) {
        switch (plot.kind()) {
            case FIELD -> land.remove(plot.chunkX(), plot.chunkZ());
            case PLANNED -> land.unplan(plot.chunkX(), plot.chunkZ());
            case PASTURE -> land.removePasture(plot.index());
        }
    }

    static void outline(MapOverlay overlay, int chunkX, int chunkZ, int size, int y, int colour) {
        if (size == Siting.PEN_CHUNKS) {
            overlay.outline(Siting.penMin(chunkX), Siting.penMin(chunkZ), Siting.penMax(chunkX), Siting.penMax(chunkZ),
                    y, colour);
            return;
        }
        overlay.outline(chunkX * 16, chunkZ * 16, chunkX * 16 + size * 16 - 1, chunkZ * 16 + size * 16 - 1, y, colour);
    }

    int colour(Target target) {
        if (tool == Tool.REMOVE) return MapOverlay.UNFIT;
        if (tool == Tool.MOVE && moving == NONE) return MapOverlay.SELECTED;
        return target.verdict().fit() ? MapOverlay.FIT : MapOverlay.UNFIT;
    }
    //endregion

    //region Drawing [绘制]
    void sprites(SpriteBatch sprites, double mouseX, double mouseY) {
        for (Tool each : Tool.values()) {
            boolean hover = Widgets.isOver(mouseX, mouseY, left(each), top, BUTTON_W, MapControls.HEIGHT);
            PanelDrawing.button(sprites, left(each), top, BUTTON_W, MapControls.HEIGHT, true, hover && each != tool,
                    each == tool, each == Tool.REMOVE);
        }
    }

    void texts(GuiGraphics graphics, Font font, Land land) {
        for (Tool each : Tool.values()) {
            PanelDrawing.label(graphics, font, Component.translatable(UI + "tool." + key(each)), left(each), top,
                    BUTTON_W, MapControls.HEIGHT, true);
        }
        int fields = 0;
        int pastures = 0;
        for (MapPlots.Plot plot : land.plots()) {
            if (plot.kind() == MapPlots.Kind.PASTURE) pastures++;
            else fields++;
        }
        graphics.drawString(font, Component.translatable(UI + "counts", fields, land.fieldCap(), pastures,
                land.pastureCap()), left(Tool.MOVE) + BUTTON_W + 6, top + 4, PanelDrawing.LEGEND, false);
    }

    boolean tip(MapPage page, GuiGraphics graphics, int mouseX, int mouseY) {
        Tool each = at(mouseX, mouseY);
        if (each == null) return false;
        return page.tipAt(graphics, left(each), top, BUTTON_W, MapControls.HEIGHT, mouseX, mouseY,
                Component.translatable(TIP + key(each)));
    }

    private static String key(Tool each) { return each.name().toLowerCase(Locale.ROOT); }
    //endregion
}
