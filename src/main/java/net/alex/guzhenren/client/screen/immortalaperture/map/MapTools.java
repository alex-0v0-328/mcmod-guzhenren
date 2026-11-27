package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapOverlay;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapPick;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.MapPart;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页底部正中的工具栏（{@code overlay} 底）：开垦（1 区块一片田）、牧场（2×2 区块）、拆除、迁移四枚 {@value #BUTTON} 方键，
 * 键下写名字，竖线隔开的危险键「割弃」；按住哪个工具，光标下就预览那块占地，合格描绿、不合格描红并在提示里写原因，左键
 * 执行（目标还在迷雾里就改为探明，Alex 定），右键或 Esc 退出工具。
 * <ul>
 *   <li>占地数据、预判与发操作都经 {@link Land}（由 {@link MapToolLand} 用 {@code MapPart.Section} 与 {@code Siting}
 *       实现），本类只管工具状态、目标换算、绘制与命中。</li>
 *   <li>迁移分两步：先点要迁的牧场，再点新址；预判时忽略这个牧场自己的 4 个区块。</li>
 *   <li>开垦可拖框（{@link #area}）：按住左键拖出区块矩形，逐块描绿 / 红，松手把合格的一次交上去（{@link #planArea}）；
 *       拿着开垦工具时左键拖动不再平移地图（WASD 与右键拖动照常）。</li>
 *   <li>田的轮廓描整区块，牧场描栅栏所在的 30×30 周界（{@link Siting#penMin} 到 {@link Siting#penMax}）。</li>
 *   <li>拿着工具时工具栏上方一枚 {@code chip} 用键帽短语写操作（「牧场 [左键] 圈地 [右键] 取消」，Esc 照常可用不写）；
 *       当前工具是选中键、名字金色；选工具只靠点击，数字键仍切页签（Alex 定）。</li>
 * </ul>
 */

final class MapTools {

    private static final int BUTTON = 26;
    private static final int PITCH = BUTTON + 4;
    private static final int SIDE = 8;
    private static final int SPLIT = 12;
    private static final int BOX_W = SIDE + 4 * BUTTON + 3 * 4 + SPLIT + BUTTON + 5;
    private static final int BOX_H = 44;
    private static final int BOTTOM = 7;
    private static final int RULE = 0xFF4A4045;
    private static final String UI = EarthSpiritScreen.UI + "map.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_tool_";
    private static final int NONE = -1;
    static final int AREA_SIDE = 8;

    enum Tool { FARM, PASTURE, REMOVE, MOVE }

    /** 预判结果：不合格时 {@code reason} 写给玩家看；{@code fog} 为真即目标还在迷雾里，左键改为探明。 */
    record Verdict(boolean fit, Component reason, boolean fog) {

        static final Verdict FIT = new Verdict(true, Component.empty(), false);
    }

    /** 光标下的工具目标：以 {@code size}×{@code size} 区块计，{@code plot} 是被点中的已有占地（拆除、迁移第一步）。 */
    record Target(int chunkX, int chunkZ, int size, Verdict verdict, MapPlots.@Nullable Plot plot) {}

    /** 地图页提供给工具条与对象卡片的经营数据与操作。 */
    interface Land {

        List<MapPlots.Plot> plots();

        int fieldCap();

        int pastureCap();

        MapPart.Section.@Nullable Field field(int index);

        MapPart.Section.@Nullable Pen pen(int index);

        long mud();

        long seeds(int crop);

        List<ResourceLocation> animals();

        Verdict farm(int chunkX, int chunkZ);

        Verdict pasture(int chunkX, int chunkZ, int ignoredIndex);

        void plan(int chunkX, int chunkZ);

        void planMany(List<Long> chunks);

        void unplan(int chunkX, int chunkZ);

        void remove(int chunkX, int chunkZ);

        void placePasture(int chunkX, int chunkZ);

        void movePasture(int index, int chunkX, int chunkZ);

        void removePasture(int index);

        void gather(int index);

        void setFieldCrop(int chunkX, int chunkZ, int crop);

        void setAllCrops(int crop);

        void setFieldKind(int chunkX, int chunkZ, int kind);

        void setAutoGather(int index, boolean on);

        void setGatherType(int index, ResourceLocation type, boolean on);

        void reveal(int chunkX, int chunkZ, int side);

        void teleport(int x, int z);

        void returnToSpirit();
    }

    private @Nullable Tool tool;
    private int moving = NONE;
    private int boxX;
    private int boxY;

    void layout(int width, int height) {
        boxX = (width - BOX_W) / 2;
        boxY = height - BOTTOM - BOX_H;
    }

    boolean contains(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, boxX, boxY, BOX_W, BOX_H);
    }

    @Nullable Tool active() { return tool; }

    void cancel() {
        tool = null;
        moving = NONE;
    }

    private int left(Tool each) { return boxX + SIDE + each.ordinal() * PITCH; }

    private int cedeX() { return boxX + BOX_W - 5 - BUTTON; }

    private int buttonY() { return boxY + 5; }

    @Nullable Tool at(double mouseX, double mouseY) {
        for (Tool each : Tool.values()) {
            if (Widgets.isOver(mouseX, mouseY, left(each), buttonY(), BUTTON, BUTTON)) return each;
        }
        return null;
    }

    boolean isOverCede(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, cedeX(), buttonY(), BUTTON, BUTTON);
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
                else if (target.verdict().fog()) land.reveal(target.chunkX(), target.chunkZ(), target.size());
            }
            case PASTURE -> {
                if (target.verdict().fit()) land.placePasture(target.chunkX(), target.chunkZ());
                else if (target.verdict().fog()) land.reveal(target.chunkX(), target.chunkZ(), target.size());
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
                } else if (moving != NONE && target.verdict().fog()) {
                    land.reveal(target.chunkX(), target.chunkZ(), target.size());
                }
            }
        }
    }

    /**
     * 开垦拖框：从按下处到光标处的区块矩形（每边至多 {@value #AREA_SIDE} 区块）逐块预判；按行序数到田的剩余上限为止，
     * 之后的一律不合格。
     */
    List<Target> area(Land land, int fromX, int fromZ, int toX, int toZ) {
        int minX = Math.max(Math.min(fromX, toX), fromX - AREA_SIDE + 1);
        int maxX = Math.min(Math.max(fromX, toX), fromX + AREA_SIDE - 1);
        int minZ = Math.max(Math.min(fromZ, toZ), fromZ - AREA_SIDE + 1);
        int maxZ = Math.min(Math.max(fromZ, toZ), fromZ + AREA_SIDE - 1);
        int room = land.fieldCap() - (int) land.plots().stream().filter(plot -> plot.kind() != MapPlots.Kind.PASTURE)
                .count();
        List<Target> targets = new ArrayList<>();
        for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
            for (int chunkX = minX; chunkX <= maxX; chunkX++) {
                Verdict verdict = land.farm(chunkX, chunkZ);
                if (verdict.fit() && room-- <= 0) {
                    verdict = new Verdict(false, Component.translatable(Siting.Reason.CAP.key()), false);
                }
                targets.add(new Target(chunkX, chunkZ, 1, verdict, null));
            }
        }
        return targets;
    }

    static void planArea(Land land, List<Target> targets) {
        List<Long> chunks = new ArrayList<>();
        for (Target target : targets) {
            if (target.verdict().fit()) chunks.add(ChunkPos.asLong(target.chunkX(), target.chunkZ()));
        }
        land.planMany(chunks);
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
        sprites.blit(PanelDrawing.OVERLAY, boxX, boxY, BOX_W, BOX_H);
        for (Tool each : Tool.values()) {
            PanelDrawing.Look look = each == tool ? PanelDrawing.Look.SELECTED : PanelDrawing.Look.PLAIN;
            PanelDrawing.button(sprites, left(each), buttonY(), BUTTON, BUTTON, look, true, mouseX, mouseY);
            icon(sprites, "tool_" + key(each), left(each), each == tool);
        }
        PanelDrawing.button(sprites, cedeX(), buttonY(), BUTTON, BUTTON, PanelDrawing.Look.DANGER, true, mouseX,
                mouseY);
        icon(sprites, "tool_cede", cedeX(), false);
    }

    private void icon(SpriteBatch sprites, String name, int left, boolean selected) {
        int sunk = selected || PanelDrawing.pressed(left, buttonY(), BUTTON, BUTTON) ? 1 : 0;
        sprites.blit(PanelDrawing.sprite(name), left + 5, buttonY() + 3 + sunk, 16, 16);
    }

    void fills(GuiGraphics graphics) {
        int rule = cedeX() - SPLIT / 2 + 1;
        graphics.fill(rule, buttonY() + 2, rule + 1, boxY + BOX_H - 6, RULE);
    }

    void texts(GuiGraphics graphics, Font font) {
        int labelY = buttonY() + BUTTON + 3;
        for (Tool each : Tool.values()) {
            PanelDrawing.centered(graphics, font, Component.translatable(UI + "tool." + key(each)), left(each) - 2,
                    left(each) + BUTTON + 2, labelY, each == tool ? PanelDrawing.ACCENT : PanelDrawing.TEXT);
        }
        PanelDrawing.centered(graphics, font, Component.translatable(EarthSpiritScreen.UI + "cede"), cedeX() - 2,
                cedeX() + BUTTON + 2, labelY, PanelDrawing.TEXT);
    }

    /** 拿着工具时工具栏上方的操作提示；画图与写字两步共用这份排版。 */
    void hint(@Nullable SpriteBatch sprites, @Nullable GuiGraphics graphics, Font font) {
        Tool active = tool;
        if (active == null) return;
        Component name = Component.translatable(UI + "tool." + key(active));
        Component left = Component.translatable(UI + "key.left");
        Component action = Component.translatable(UI + "hint." + hintKey(active));
        Component right = Component.translatable(UI + "key.right");
        Component cancel = Component.translatable(UI + "hint.cancel");
        int width = 6 + font.width(name) + 6 + PanelDrawing.keycapWidth(font, left) + 3 + font.width(action) + 6
                + PanelDrawing.keycapWidth(font, right) + 3 + font.width(cancel) + 6;
        int x = boxX + (BOX_W - width) / 2;
        int y = boxY - 5 - 16;
        if (sprites != null) sprites.blit(PanelDrawing.CHIP, x, y, width, 16);
        x += 6;
        if (graphics != null) graphics.drawString(font, name, x, y + 4, PanelDrawing.ACCENT, false);
        x += font.width(name) + 6;
        x = MapHud.keyPhrase(sprites, graphics, font, left, action, x, y) + 6;
        MapHud.keyPhrase(sprites, graphics, font, right, cancel, x, y);
    }

    private String hintKey(Tool each) {
        if (each != Tool.MOVE) return key(each);
        return moving == NONE ? "move_pick" : "move_place";
    }

    boolean tip(MapPage page, GuiGraphics graphics, int mouseX, int mouseY) {
        if (isOverCede(mouseX, mouseY)) {
            return page.tipAt(graphics, cedeX(), buttonY(), BUTTON, BUTTON, mouseX, mouseY,
                    Component.translatable(EarthSpiritScreen.TIP + "map_cede"));
        }
        Tool each = at(mouseX, mouseY);
        if (each == null) return false;
        return page.tipAt(graphics, left(each), buttonY(), BUTTON, BUTTON, mouseX, mouseY,
                Component.translatable(TIP + key(each)));
    }

    private static String key(Tool each) { return each.name().toLowerCase(Locale.ROOT); }
    //endregion
}
