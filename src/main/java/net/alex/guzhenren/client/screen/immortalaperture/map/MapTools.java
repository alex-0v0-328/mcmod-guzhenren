package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapOverlay;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapPick;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.HerdPart;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.MapPart;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页底部正中的功能栏（{@code overlay} 底，Alex 定）：耕种（拿开垦工具，1 区块一片田）、放牧（拿开拓牧场工具，2×2 区块）、
 * 移动（搬迁田或牧场）三枚工具键，竖线隔开的选择、全选，再隔开的危险键「割弃」；宽档（同对象栏，{@link MapObjectBar}）键
 * {@value #WIDE_BUTTON}、键下写名字，紧凑档键 {@value #COMPACT_BUTTON} 不写名字。默认是选择模式（不拿工具，见
 * {@link MapSelection}）；按住哪个工具，光标下就预览那块占地，合格描绿、不合格描红并在提示里写原因，左键执行（目标还在迷雾
 * 里就改为探明，Alex 定），右键或 Esc 退回选择。
 * <ul>
 *   <li>占地数据、预判与发操作都经 {@link Land}（由 {@link MapToolLand} 用 {@code MapPart.Section} 与 {@code Siting}
 *       实现），本类只管工具状态、目标换算、绘制与命中。</li>
 *   <li>移动分两步：先点要搬的田、规划田或牧场，再点新址；预判时不算它自己原来的区块。搬田 = 旧田拆掉、新址进规划队列（
 *       作物选择带过去），见 {@code FarmPart.MoveField}。</li>
 *   <li>开垦可拖框（{@link #area}）：按住左键拖出区块矩形，逐块描绿 / 红，松手把合格的一次交上去（{@link #planArea}）；
 *       拿着开垦工具时左键拖动不再平移地图（WASD 与右键拖动照常）。</li>
 *   <li>田的轮廓描整区块，牧场描栅栏所在的 30×30 周界（{@link Siting#penMin} 到 {@link Siting#penMax}）。</li>
 *   <li>功能栏上方一枚 {@code chip} 写按键短语：浏览时是视角操作（WASD、滚轮、Q/E、V、H、右键，Alex 定都并在这里），拿着
 *       工具时写「工具名 [左键] 动作 [右键] 取消」；当前工具与选择模式是选中键、名字金色；选工具只靠点击，数字键仍切页签。</li>
 * </ul>
 */

final class MapTools {

    private static final int WIDE_BUTTON = 26;
    private static final int COMPACT_BUTTON = 22;
    private static final int SIDE = 6;
    private static final int GAP = 4;
    private static final int BOTTOM = 6;
    private static final String[][] BROWSE_HINT = {{"wasd", "pan"}, {"wheel", "zoom"}, {"qe", "rotate"},
            {"v", "overhead"}, {"h", "home"}, {"right", "teleport"}};
    private static final int RULE = 0xFF4A4045;
    private static final String UI = EarthSpiritScreen.UI + "map.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_tool_";
    static final int AREA_SIDE = 8;

    enum Tool { FARM, PASTURE, MOVE }

    /** 功能栏上的键，按排列顺序；{@link #group} 一变就隔一道竖线。 */
    enum Key {
        FARM(0, "tool_farm"), PASTURE(0, "tool_pasture"), MOVE(0, "tool_move"), SELECT(1, "tool_select"),
        SELECT_ALL(1, "tool_select_all"), CEDE(2, "tool_cede");

        private final int group;
        private final String icon;

        Key(int group, String icon) {
            this.group = group;
            this.icon = icon;
        }

        String id() { return name().toLowerCase(Locale.ROOT); }
    }

    /** 预判结果：不合格时 {@code reason} 写给玩家看；{@code fog} 为真即目标还在迷雾里，左键改为探明。 */
    record Verdict(boolean fit, Component reason, boolean fog) {

        static final Verdict FIT = new Verdict(true, Component.empty(), false);
    }

    /** 光标下的工具目标：以 {@code size}×{@code size} 区块计，{@code plot} 是被点中的已有占地（移动第一步）。 */
    record Target(int chunkX, int chunkZ, int size, Verdict verdict, MapPlots.@Nullable Plot plot) {}

    /** 地图页提供给功能栏与对象栏的经营数据与操作。 */
    interface Land {

        List<MapPlots.Plot> plots();

        int fieldCap();

        int pastureCap();

        MapPart.Section.@Nullable Field field(int index);

        HerdPart.@Nullable Row pen(int index);

        long mud();

        long boneMeal();

        long seeds(int crop);

        List<HerdPart.Kind> animals();

        Verdict farm(int chunkX, int chunkZ);

        Verdict moveField(int fromX, int fromZ, int chunkX, int chunkZ);

        Verdict pasture(int chunkX, int chunkZ, int ignoredIndex);

        void plan(int chunkX, int chunkZ);

        void planMany(List<Long> chunks);

        void sow(List<Long> chunks, int crop);

        void retype(List<Long> chunks, int kind);

        void ripen(List<Long> chunks);

        void harvest(List<Long> chunks);

        void uproot(List<Long> chunks);

        void clear(List<Long> chunks);

        void sendMoveField(int fromX, int fromZ, int chunkX, int chunkZ);

        void placePasture(int chunkX, int chunkZ);

        void movePasture(int index, int chunkX, int chunkZ);

        void removePasture(int index);

        void gather(int index);

        void setAnimal(int index, Optional<ResourceLocation> animal);

        void setFloor(int index, int floor);

        void slaughter(int index);

        void reveal(int chunkX, int chunkZ, int side);

        void teleport(int x, int z);
    }

    private @Nullable Tool tool;
    private MapPlots.@Nullable Plot moving;
    private boolean wide;
    private int button;
    private int split;
    private int boxX;
    private int boxY;
    private int boxW;
    private int boxH;
    private final int[] lefts = new int[Key.values().length];

    void layout(int width, int height) {
        wide = MapObjectBar.wide(width, height);
        button = wide ? WIDE_BUTTON : COMPACT_BUTTON;
        split = wide ? 12 : 8;
        int x = SIDE;
        for (Key key : Key.values()) {
            if (key.ordinal() > 0) x += Key.values()[key.ordinal() - 1].group == key.group ? GAP : split;
            lefts[key.ordinal()] = x;
            x += button;
        }
        boxW = x + SIDE;
        boxH = wide ? 5 + button + 3 + 9 + 3 : 4 + button + 4;
        boxX = (width - boxW) / 2;
        boxY = height - BOTTOM - boxH;
    }

    /** 功能栏连同上方提示条的顶边；对象栏与图例据此让位。 */
    int boxTop() { return boxY - 5 - MapHud.CHIP_H; }

    int top() { return boxY; }

    int boxWidth() { return boxW; }

    /** 功能栏的 x、y、宽、高（排版自检用）。 */
    int[] box() { return new int[] {boxX, boxY, boxW, boxH}; }

    boolean contains(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, boxX, boxY, boxW, boxH);
    }

    @Nullable Tool active() { return tool; }

    void cancel() {
        tool = null;
        moving = null;
    }

    private int left(Key key) { return boxX + lefts[key.ordinal()]; }

    private int buttonY() { return boxY + (wide ? 5 : 4); }

    @Nullable Key at(double mouseX, double mouseY) {
        for (Key key : Key.values()) {
            if (Widgets.isOver(mouseX, mouseY, left(key), buttonY(), button, button)) return key;
        }
        return null;
    }

    private static @Nullable Tool toolOf(Key key) {
        return switch (key) {
            case FARM -> Tool.FARM;
            case PASTURE -> Tool.PASTURE;
            case MOVE -> Tool.MOVE;
            default -> null;
        };
    }

    void press(Tool each) {
        moving = null;
        tool = tool == each ? null : each;
    }

    //region Targets [目标]
    @Nullable Target target(Land land, MapPick.@Nullable Hit hover) {
        if (tool == null || hover == null) return null;
        int chunkX = hover.x() >> 4;
        int chunkZ = hover.z() >> 4;
        return switch (tool) {
            case FARM -> new Target(chunkX, chunkZ, 1, land.farm(chunkX, chunkZ), null);
            case PASTURE -> pasture(land, hover, -1);
            case MOVE -> moveTarget(land, hover, chunkX, chunkZ);
        };
    }

    private @Nullable Target moveTarget(Land land, MapPick.Hit hover, int chunkX, int chunkZ) {
        MapPlots.Plot from = moving;
        if (from == null) {
            MapPlots.Plot plot = MapPlots.at(land.plots(), chunkX, chunkZ);
            return plot == null ? null : new Target(plot.chunkX(), plot.chunkZ(), plot.size(), Verdict.FIT, plot);
        }
        if (from.kind() == MapPlots.Kind.PASTURE) return pasture(land, hover, from.index());
        return new Target(chunkX, chunkZ, 1, land.moveField(from.chunkX(), from.chunkZ(), chunkX, chunkZ), null);
    }

    private static Target pasture(Land land, MapPick.Hit hover, int ignored) {
        int chunkX = MapPlots.pastureCorner(hover.x() + 0.5);
        int chunkZ = MapPlots.pastureCorner(hover.z() + 0.5);
        return new Target(chunkX, chunkZ, 2, land.pasture(chunkX, chunkZ, ignored), null);
    }

    void apply(Land land, Target target) {
        if (tool == null) return;
        Verdict verdict = target.verdict();
        switch (tool) {
            case FARM -> {
                if (verdict.fit()) land.plan(target.chunkX(), target.chunkZ());
                else if (verdict.fog()) land.reveal(target.chunkX(), target.chunkZ(), target.size());
            }
            case PASTURE -> {
                if (verdict.fit()) land.placePasture(target.chunkX(), target.chunkZ());
                else if (verdict.fog()) land.reveal(target.chunkX(), target.chunkZ(), target.size());
            }
            case MOVE -> {
                MapPlots.Plot from = moving;
                if (from == null) {
                    moving = target.plot();
                } else if (verdict.fit()) {
                    if (from.kind() == MapPlots.Kind.PASTURE) {
                        land.movePasture(from.index(), target.chunkX(), target.chunkZ());
                    } else {
                        land.sendMoveField(from.chunkX(), from.chunkZ(), target.chunkX(), target.chunkZ());
                    }
                    cancel();
                } else if (verdict.fog()) {
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

    static void outline(MapOverlay overlay, int chunkX, int chunkZ, int size, int y, int colour) {
        if (size == Siting.PEN_CHUNKS) {
            overlay.outline(Siting.penMin(chunkX), Siting.penMin(chunkZ), Siting.penMax(chunkX), Siting.penMax(chunkZ),
                    y, colour);
            return;
        }
        overlay.outline(chunkX * 16, chunkZ * 16, chunkX * 16 + size * 16 - 1, chunkZ * 16 + size * 16 - 1, y, colour);
    }

    int colour(Target target) {
        if (tool == Tool.MOVE && moving == null) return MapOverlay.SELECTED;
        return target.verdict().fit() ? MapOverlay.FIT : MapOverlay.UNFIT;
    }
    //endregion

    //region Drawing [绘制]
    private boolean selected(Key key) {
        Tool each = toolOf(key);
        return each != null ? each == tool : key == Key.SELECT && tool == null;
    }

    void sprites(SpriteBatch sprites, Font font, double mouseX, double mouseY, boolean hint) {
        sprites.blit(Widgets.OVERLAY, boxX, boxY, boxW, boxH);
        for (Key key : Key.values()) {
            Widgets.Look look = key == Key.CEDE ? Widgets.Look.DANGER
                    : selected(key) ? Widgets.Look.SELECTED : Widgets.Look.PLAIN;
            Widgets.button(sprites, left(key), buttonY(), button, button, look, true, mouseX, mouseY);
            int sunk = selected(key) || Widgets.pressed(left(key), buttonY(), button, button) ? 1 : 0;
            int inset = (button - 16) / 2;
            sprites.blit(PanelDrawing.sprite(key.icon), left(key) + inset, buttonY() + inset + sunk, 16, 16);
        }
        if (hint) this.hint(sprites, null, font);
    }

    void fills(GuiGraphics graphics) {
        for (Key key : Key.values()) {
            if (key.ordinal() == 0 || Key.values()[key.ordinal() - 1].group == key.group) continue;
            int rule = left(key) - split / 2;
            graphics.fill(rule, buttonY() + 2, rule + 1, buttonY() + button - 2, RULE);
        }
    }

    void texts(GuiGraphics graphics, Font font, boolean hint) {
        if (hint) this.hint(null, graphics, font);
        if (!wide) return;
        int labelY = buttonY() + button + 3;
        for (Key key : Key.values()) {
            Widgets.centered(graphics, font, Component.translatable(UI + "tool." + key.id()), left(key) - 2,
                    left(key) + button + 2, labelY, selected(key) ? ModPalette.ACCENT : ModPalette.TEXT);
        }
    }

    /** 功能栏上方的按键提示条：浏览时是视角操作，拿着工具时写工具操作；画图与写字两步共用这份排版。 */
    void hint(@Nullable SpriteBatch sprites, @Nullable GuiGraphics graphics, Font font) {
        Tool active = tool;
        if (active == null) {
            MapHud.hintChip(sprites, graphics, font, BROWSE_HINT, boxX + boxW / 2, boxTop());
            return;
        }
        Component name = Component.translatable(UI + "tool." + Key.values()[active.ordinal()].id());
        Component left = Component.translatable(UI + "key.left");
        Component action = Component.translatable(UI + hintKey(active));
        Component right = Component.translatable(UI + "key.right");
        Component cancel = Component.translatable(EarthSpiritScreen.UI + "cancel");
        int width = 6 + font.width(name) + 6 + Widgets.keycapWidth(font, left) + 3 + font.width(action) + 6
                + Widgets.keycapWidth(font, right) + 3 + font.width(cancel) + 6;
        int x = boxX + (boxW - width) / 2;
        int y = boxTop();
        if (sprites != null) sprites.blit(Widgets.CHIP, x, y, width, MapHud.CHIP_H);
        x += 6;
        if (graphics != null) graphics.drawString(font, name, x, y + 4, ModPalette.ACCENT, false);
        x += font.width(name) + 6;
        x = MapHud.keyPhrase(sprites, graphics, font, left, action, x, y) + 6;
        MapHud.keyPhrase(sprites, graphics, font, right, cancel, x, y);
    }

    private String hintKey(Tool each) {
        return switch (each) {
            case FARM -> "hint.plan";
            case PASTURE -> "hint.pasture";
            case MOVE -> moving == null ? "hint.move_pick" : "hint.move_place";
        };
    }

    boolean tip(MapPage page, GuiGraphics graphics, int mouseX, int mouseY) {
        Key key = at(mouseX, mouseY);
        if (key == null) return false;
        return page.tipAt(graphics, left(key), buttonY(), button, button, mouseX, mouseY,
                Component.translatable(TIP + key.id()));
    }
    //endregion
}
