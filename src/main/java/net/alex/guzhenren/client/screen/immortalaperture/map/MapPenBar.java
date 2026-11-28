package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdRules;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.HerdPart;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 牧场的对象栏：选中一个牧场时的四枚键（Alex 定，一个牧场只养一种）：指定动物（弹竖列表：动物名与福地头数，文字不用刷怪蛋）、
 * 宰杀（弹数量窗：下限 −/+，现在会宰几头与「宰杀」）、收拢（把福地里栏外的这种收进来，角上写栏外头数）、清除（弹一行「确认
 * 拆除牧场」）。表头左写「牧场 · 动物」、右写「栏内 N 头 · 下限 M」。
 * <ul>
 *   <li>下限 0 = 不自动宰也不宰；下限大于 0 时托管每轮多了宰、少了喂（Alex 定）；−/+ 每次 1，Shift 一次
 *       {@value #SHIFT_STEP}。下限随点随发，服务端回来之前先显示刚设的数。换动物时服务端把下限清零。</li>
 *   <li>动物列表 = 福地里已加载的可支使种类（捕食者除外）；可见 {@value #LIST_ROWS} 行，多了滚轮翻；当前指定的种类即使眼下
 *       一只没有也列出来。</li>
 *   <li>牧场的迁移在功能栏「移动」；选区一变就收起弹窗。</li>
 * </ul>
 */

final class MapPenBar extends MapObjectBar {

    private static final int ANIMAL = 0;
    private static final int SLAUGHTER = 1;
    private static final int GATHER = 2;
    private static final int REMOVE = 3;
    private static final String[] KEYS = {"animal", "slaughter", "gather", "remove"};
    private static final String[] ICONS = {null, "act_slaughter", "act_gather", "tool_remove"};
    private static final int LIST_ROWS = 7;
    private static final int LIST_W = 100;
    private static final int COUNT_W = 112;
    private static final int STEP = 14;
    private static final int SHIFT_STEP = 10;
    private static final int NONE = -1;
    private static final String UI = EarthSpiritScreen.UI + "map.pen.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_pen_";
    private static final String[][] SELECT_HINT = {{"esc", "deselect"}};

    private enum Pop { NONE, ANIMAL, SLAUGHTER, REMOVE }

    private final MapTools.Land land;
    private Pop pop = Pop.NONE;
    private int listRow;
    private int seenIndex = NONE;
    private int sentFloor = NONE;
    private int screenWidth;
    private HerdPart.@Nullable Row row;

    MapPenBar(MapTools.Land land) { this.land = land; }

    @Override
    void layout(int width, int height, int above) {
        super.layout(width, height, above);
        screenWidth = width;
    }

    boolean picking() { return pop != Pop.NONE; }

    void cancel() { pop = Pop.NONE; }

    boolean contains(double mouseX, double mouseY) {
        return barContains(mouseX, mouseY) || pop != Pop.NONE && popup().contains(mouseX, mouseY);
    }

    private void sync(MapPlots.Plot pasture) {
        row = land.pen(pasture.index());
        if (pasture.index() != seenIndex) {
            seenIndex = pasture.index();
            pop = Pop.NONE;
            listRow = 0;
            sentFloor = NONE;
        }
        if (row != null && row.floor() == sentFloor) sentFloor = NONE;
    }

    private @Nullable ResourceLocation animal() { return row == null ? null : row.animal().orElse(null); }

    private int floor() { return sentFloor != NONE ? sentFloor : row == null ? 0 : row.floor(); }

    private static Component name(ResourceLocation type) {
        return BuiltInRegistries.ENTITY_TYPE.get(type).getDescription();
    }

    //region Keys [键]
    @Override
    protected int keys() { return KEYS.length; }

    @Override
    protected Face face(int key) {
        PanelDrawing.Look look = pop == popOf(key) && pop != Pop.NONE ? PanelDrawing.Look.SELECTED
                : PanelDrawing.Look.PLAIN;
        ResourceLocation animal = animal();
        Component label = Component.translatable(UI + KEYS[key]);
        return switch (key) {
            case ANIMAL -> new Face(null, animal == null ? null : name(animal), look, row != null, -1, label);
            case GATHER -> new Face(ICONS[key], null, look, animal != null && row.outside() > 0,
                    row == null ? -1 : row.outside(), label);
            case SLAUGHTER -> new Face(ICONS[key], null, look, animal != null, -1, label);
            default -> new Face(ICONS[key], null, look, row != null, -1, label);
        };
    }

    private static Pop popOf(int key) {
        return switch (key) {
            case ANIMAL -> Pop.ANIMAL;
            case SLAUGHTER -> Pop.SLAUGHTER;
            case REMOVE -> Pop.REMOVE;
            default -> Pop.NONE;
        };
    }
    //endregion

    //region Popups [弹窗]
    /** 列表里的种类：福地里有的，加上当前指定、眼下一只没有的。 */
    private List<HerdPart.Kind> kinds() {
        List<HerdPart.Kind> kinds = new ArrayList<>(land.animals());
        ResourceLocation animal = animal();
        if (animal != null && kinds.stream().noneMatch(kind -> kind.type().equals(animal))) {
            kinds.addFirst(new HerdPart.Kind(animal, 0));
        }
        return kinds;
    }

    private int listRows() { return Math.max(1, Math.min(LIST_ROWS, kinds().size())); }

    private MapPopup.Box popup() {
        return switch (pop) {
            case ANIMAL -> MapPopup.above(keyCentre(ANIMAL), top(), LIST_W, MapPopup.rows(listRows()), screenWidth);
            case SLAUGHTER -> MapPopup.above(keyCentre(SLAUGHTER), top(), COUNT_W, MapPopup.rows(2), screenWidth);
            case REMOVE -> MapPopup.above(keyCentre(REMOVE), top(), LIST_W, MapPopup.rows(1), screenWidth);
            case NONE -> new MapPopup.Box(0, 0, 0, 0);
        };
    }

    private int listWidth(MapPopup.Box box) { return box.innerWidth() - (kinds().size() > LIST_ROWS ? 8 : 0); }

    private int minusX(MapPopup.Box box) { return box.innerX() + 30; }

    private int plusX(MapPopup.Box box) { return minusX(box) + STEP + 24; }

    private int slayW() { return 40; }

    private int slayX(MapPopup.Box box) { return box.x() + box.width() - MapPopup.PAD - slayW(); }
    //endregion

    //region Drawing [绘制]
    void sprites(SpriteBatch sprites, MapPlots.Plot pasture, Font font, double mouseX, double mouseY) {
        sync(pasture);
        barSprites(sprites, mouseX, mouseY);
        if (pop == Pop.NONE) {
            MapHud.hintChip(sprites, null, font, SELECT_HINT, left() + WIDTH / 2, hintTop());
            return;
        }
        MapPopup.Box box = popup();
        MapPopup.frame(sprites, box);
        switch (pop) {
            case ANIMAL -> {
                List<HerdPart.Kind> kinds = kinds();
                for (int at = 0; at < listRows() && listRow + at < kinds.size(); at++) {
                    boolean current = kinds.get(listRow + at).type().equals(animal());
                    PanelDrawing.button(sprites, box.innerX(), MapPopup.rowY(box, at), listWidth(box), MapPopup.ROW_H,
                            current ? PanelDrawing.Look.SELECTED : PanelDrawing.Look.GHOST, true, mouseX, mouseY);
                }
                if (kinds.size() > LIST_ROWS) {
                    PanelDrawing.scrollbar(sprites, box.innerX() + box.innerWidth() - 6, box.innerY(),
                            MapPopup.rows(LIST_ROWS), listRow, LIST_ROWS, kinds.size(), false);
                }
            }
            case SLAUGHTER -> {
                int y = MapPopup.rowY(box, 0);
                PanelDrawing.button(sprites, minusX(box), y, STEP, STEP, PanelDrawing.Look.GHOST, floor() > 0, mouseX,
                        mouseY);
                PanelDrawing.button(sprites, plusX(box), y, STEP, STEP, PanelDrawing.Look.GHOST, true, mouseX, mouseY);
                PanelDrawing.button(sprites, slayX(box), MapPopup.rowY(box, 1), slayW(), MapPopup.ROW_H,
                        PanelDrawing.Look.PLAIN, row != null && row.slaughter() > 0, mouseX, mouseY);
            }
            case REMOVE -> PanelDrawing.button(sprites, box.innerX(), MapPopup.rowY(box, 0), box.innerWidth(),
                    MapPopup.ROW_H, PanelDrawing.Look.DANGER, true, mouseX, mouseY);
            case NONE -> {}
        }
    }

    void texts(GuiGraphics graphics, Font font) {
        ResourceLocation animal = animal();
        Component title = animal == null ? Component.translatable(UI + "title_none")
                : Component.translatable(UI + "title", name(animal));
        Component state = Component.translatable(UI + "state", row == null ? 0 : row.penned(), floor());
        barTexts(graphics, font, title, state);
        if (pop == Pop.NONE) {
            MapHud.hintChip(null, graphics, font, SELECT_HINT, left() + WIDTH / 2, hintTop());
            return;
        }
        MapPopup.Box box = popup();
        switch (pop) {
            case ANIMAL -> {
                MapPopup.title(graphics, font, box, Component.translatable(UI + "animal"),
                        Component.translatable(UI + "land_count"));
                List<HerdPart.Kind> kinds = kinds();
                for (int at = 0; at < listRows() && listRow + at < kinds.size(); at++) {
                    HerdPart.Kind kind = kinds.get(listRow + at);
                    int y = MapPopup.rowY(box, at) + 3;
                    boolean current = kind.type().equals(animal);
                    int colour = current ? PanelDrawing.SELECTED_TEXT : kind.count() > 0 ? PanelDrawing.TEXT
                            : PanelDrawing.DEAD;
                    graphics.drawString(font, name(kind.type()), box.innerX() + 4, y, colour, false);
                    PanelDrawing.right(graphics, font, Integer.toString(kind.count()),
                            box.innerX() + listWidth(box) - 4, y, current ? colour : PanelDrawing.LEGEND);
                }
            }
            case SLAUGHTER -> {
                MapPopup.title(graphics, font, box, Component.translatable(UI + "slaughter_title",
                        animal == null ? Component.empty() : name(animal)), Component.empty());
                int y = MapPopup.rowY(box, 0);
                graphics.drawString(font, Component.translatable(UI + "floor"), box.innerX(), y + 3, PanelDrawing.TEXT,
                        false);
                PanelDrawing.label(graphics, font, Component.literal("−"), minusX(box), y, STEP, STEP,
                        PanelDrawing.Look.GHOST, floor() > 0);
                PanelDrawing.centered(graphics, font, Component.literal(Integer.toString(floor())), minusX(box) + STEP,
                        plusX(box), y + 3, PanelDrawing.TEXT);
                PanelDrawing.label(graphics, font, Component.literal("+"), plusX(box), y, STEP, STEP,
                        PanelDrawing.Look.GHOST, true);
                int now = row == null ? 0 : row.slaughter();
                graphics.drawString(font, Component.translatable(UI + "slaughter_now", now), box.innerX(),
                        MapPopup.rowY(box, 1) + 3, PanelDrawing.LEGEND, false);
                PanelDrawing.label(graphics, font, Component.translatable(UI + "slaughter"), slayX(box),
                        MapPopup.rowY(box, 1), slayW(), MapPopup.ROW_H, PanelDrawing.Look.PLAIN, now > 0);
            }
            case REMOVE -> {
                MapPopup.title(graphics, font, box, Component.translatable(UI + "remove"), Component.empty());
                PanelDrawing.label(graphics, font, Component.translatable(UI + "remove_confirm"), box.innerX(),
                        MapPopup.rowY(box, 0), box.innerWidth(), MapPopup.ROW_H, PanelDrawing.Look.DANGER, true);
            }
            case NONE -> {}
        }
    }

    boolean tip(MapPage page, GuiGraphics graphics, int mouseX, int mouseY) {
        if (pop == Pop.SLAUGHTER) {
            MapPopup.Box box = popup();
            int y = MapPopup.rowY(box, 0);
            if (page.tipAt(graphics, minusX(box), y, plusX(box) + STEP - minusX(box), STEP, mouseX, mouseY,
                    Component.translatable(TIP + "floor", SHIFT_STEP))) {
                return true;
            }
        }
        int key = keyAt(mouseX, mouseY);
        if (key < 0) return false;
        ResourceLocation animal = animal();
        Component text = key == GATHER && animal != null
                ? Component.translatable(TIP + "gather", row == null ? 0 : row.outside(), name(animal),
                HerdRules.GATHER_LIMIT)
                : Component.translatable(TIP + KEYS[key]);
        return keyTip(page, graphics, key, mouseX, mouseY, text);
    }
    //endregion

    //region Input [输入]
    /** 点在对象栏或弹窗上；{@code removed} 在拆除牧场后调（清掉选区）。 */
    void click(MapPlots.Plot pasture, Runnable removed, double mouseX, double mouseY) {
        sync(pasture);
        if (row == null) return;
        int index = pasture.index();
        if (pop != Pop.NONE && popup().contains(mouseX, mouseY)) {
            clickPopup(index, removed, mouseX, mouseY);
            return;
        }
        int key = keyAt(mouseX, mouseY);
        if (key < 0 || !face(key).live()) {
            pop = Pop.NONE;
            return;
        }
        if (key == GATHER) {
            pop = Pop.NONE;
            land.gather(index);
            return;
        }
        Pop wanted = popOf(key);
        pop = pop == wanted ? Pop.NONE : wanted;
        listRow = 0;
    }

    private void clickPopup(int index, Runnable removed, double mouseX, double mouseY) {
        MapPopup.Box box = popup();
        switch (pop) {
            case ANIMAL -> {
                int at = MapPopup.rowAt(box, listRows(), listWidth(box), mouseX, mouseY);
                List<HerdPart.Kind> kinds = kinds();
                if (at < 0 || listRow + at >= kinds.size()) return;
                ResourceLocation type = kinds.get(listRow + at).type();
                if (!type.equals(animal())) land.setAnimal(index, Optional.of(type));
                pop = Pop.NONE;
            }
            case SLAUGHTER -> {
                int y = MapPopup.rowY(box, 0);
                int step = Screen.hasShiftDown() ? SHIFT_STEP : 1;
                if (Widgets.isOver(mouseX, mouseY, minusX(box), y, STEP, STEP) && floor() > 0) {
                    setFloor(index, Math.max(0, floor() - step));
                } else if (Widgets.isOver(mouseX, mouseY, plusX(box), y, STEP, STEP)) {
                    setFloor(index, floor() + step);
                } else if (Widgets.isOver(mouseX, mouseY, slayX(box), MapPopup.rowY(box, 1), slayW(), MapPopup.ROW_H)
                        && row != null && row.slaughter() > 0) {
                    land.slaughter(index);
                }
            }
            case REMOVE -> {
                if (MapPopup.rowAt(box, 1, box.innerWidth(), mouseX, mouseY) == 0) {
                    land.removePasture(index);
                    pop = Pop.NONE;
                    removed.run();
                }
            }
            case NONE -> {}
        }
    }

    private void setFloor(int index, int value) {
        sentFloor = value;
        land.setFloor(index, value);
    }

    /** 滚轮在动物列表上：翻行；返回真即吃掉。 */
    boolean scroll(double mouseX, double mouseY, double delta) {
        if (pop != Pop.ANIMAL || !popup().contains(mouseX, mouseY)) return false;
        listRow = PanelDrawing.clampScroll(listRow - (int) Math.signum(delta), LIST_ROWS, kinds().size());
        return true;
    }
    //endregion
}
