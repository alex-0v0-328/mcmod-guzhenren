package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.List;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldKind;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 田的对象栏：选中田（或规划田）时的四枚键（Alex 定）：种植（弹作物格，点一种就给所选各田定作物并补种）、收获、施肥（弹两格：
 * 骨粉催熟、腐泥冻土改蛊植田）、清除（弹两行：清除作物、拆除耕地）。表头左写「已选 N 块田」、右写「已种 / 熟 / 生长 %」，下一
 * 条生长条；上方一枚提示条写「[Shift] 加选 [Ctrl+A] 全选 [Esc] 取消选择」，弹窗开着时不画。
 * <ul>
 *   <li>作物格：田里有别的作物时那种作物失效（{@link FieldTotals#canPick}），要先清除作物；每行 {@value CropKeys#COLUMNS}
 *       个、可见 {@value #PICK_ROWS} 行，多了滚轮翻。选区一变就收起弹窗。</li>
 *   <li>需 / 有写在悬停提示，不够或无事可做的键与格失效；只选了规划田时只有清除（拆除 = 取消规划）可点。</li>
 *   <li>腐泥冻土与两行清除都两步：第一次点变危险色写「确认」，再点才发；点别处或选区一变即撤销。改蛊植田不可退回（Alex 定取
 *       消「改为普通田」）。</li>
 *   <li>图标一图一概念：种植 {@code act_plant}、收获镰刀 {@code act_harvest}、施肥 {@code act_ripen}、清除 {@code tool_remove}；
 *       作物、骨粉、腐泥冻土用物品本身的图。</li>
 * </ul>
 */

final class MapFieldBar extends MapObjectBar {

    private static final int PLANT = 0;
    private static final int HARVEST = 1;
    private static final int FERTILIZE = 2;
    private static final int CLEAR = 3;
    private static final String[] KEYS = {"plant", "harvest", "fertilize", "clear"};
    private static final String[] ICONS = {"act_plant", "act_harvest", "act_ripen", "tool_remove"};
    private static final int PICK_ROWS = 2;
    private static final int SLOT = CropKeys.SIZE;
    private static final int CLEAR_W = 104;
    private static final String UI = EarthSpiritScreen.UI + "map.act.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_act_";
    private static final String CONFIRM = EarthSpiritScreen.UI + "map.card.confirm";
    private static final String[][] SELECT_HINT = {{"shift", "add"}, {"select_all", "select_all"},
            {"esc", "deselect"}};

    private enum Pop { NONE, CROPS, FERTILIZE, CLEAR }

    private enum Pending { NONE, MUD, UPROOT, REMOVE }

    private Pop pop = Pop.NONE;
    private Pending pending = Pending.NONE;
    private int pickRow;
    private int seenRevision = -1;
    private int screenWidth;
    private final MapTools.Land land;
    private FieldTotals totals = FieldTotals.NONE;

    MapFieldBar(MapTools.Land land) { this.land = land; }

    @Override
    void layout(int width, int height, int above) {
        super.layout(width, height, above);
        screenWidth = width;
    }

    boolean picking() { return pop != Pop.NONE; }

    void cancel() {
        pop = Pop.NONE;
        pending = Pending.NONE;
    }

    boolean contains(double mouseX, double mouseY) {
        return barContains(mouseX, mouseY) || pop != Pop.NONE && popup().contains(mouseX, mouseY);
    }

    private void sync(MapSelection selection, FieldTotals totals) {
        this.totals = totals;
        if (selection.revision() == seenRevision) return;
        seenRevision = selection.revision();
        cancel();
        pickRow = 0;
    }

    //region Keys [键]
    @Override
    protected int keys() { return KEYS.length; }

    @Override
    protected Face face(int key) {
        boolean on = pop == popOf(key);
        Widgets.Look look = on ? Widgets.Look.SELECTED : Widgets.Look.PLAIN;
        int corner = key == HARVEST ? totals.mature() : -1;
        return new Face(ICONS[key], null, look, live(key), corner, Component.translatable(UI + KEYS[key]));
    }

    private boolean live(int key) {
        if (totals.fields() == 0) return key == CLEAR;
        return switch (key) {
            case HARVEST -> totals.mature() > 0;
            case FERTILIZE -> boneLive() || mudLive();
            default -> true;
        };
    }

    private static Pop popOf(int key) {
        return switch (key) {
            case PLANT -> Pop.CROPS;
            case FERTILIZE -> Pop.FERTILIZE;
            case CLEAR -> Pop.CLEAR;
            default -> Pop.NONE;
        };
    }

    private boolean boneLive() { return totals.growing() > 0 && land.boneMeal() > 0; }

    private boolean mudLive() { return totals.plain() > 0 && land.mud() >= totals.mudNeed(); }

    private boolean uprootLive() { return totals.planted() > 0; }
    //endregion

    //region Popups [弹窗]
    private static int pickRows() { return Math.min(PICK_ROWS, CropKeys.rows()); }

    private static boolean pickScrolls() { return CropKeys.rows() > PICK_ROWS; }

    private MapPopup.Box popup() {
        return switch (pop) {
            case CROPS -> MapPopup.above(keyCentre(PLANT), top(), Math.max(80, CropKeys.width() + (pickScrolls() ? 8 : 0)
                    + 2 * MapPopup.PAD), CropKeys.height(pickRows()), screenWidth);
            case FERTILIZE -> MapPopup.above(keyCentre(FERTILIZE), top(), 80, SLOT, screenWidth);
            case CLEAR -> MapPopup.above(keyCentre(CLEAR), top(), CLEAR_W, MapPopup.rows(2), screenWidth);
            case NONE -> new MapPopup.Box(0, 0, 0, 0);
        };
    }

    private int slotX(MapPopup.Box box, int slot) { return box.innerX() + slot * (SLOT + 2); }

    private int slotAt(MapPopup.Box box, double mouseX, double mouseY) {
        for (int slot = 0; slot < 2; slot++) {
            if (Widgets.isOver(mouseX, mouseY, slotX(box, slot), box.innerY(), SLOT, SLOT)) return slot;
        }
        return -1;
    }

    private static Component clearRow(int row) {
        return Component.translatable(UI + (row == 0 ? "uproot" : "remove"));
    }
    //endregion

    //region Drawing [绘制]
    void sprites(SpriteBatch sprites, FieldTotals totals, MapSelection selection, Font font, double mouseX,
                 double mouseY) {
        sync(selection, totals);
        barSprites(sprites, mouseX, mouseY);
        if (pop == Pop.NONE) {
            MapHud.hintChip(sprites, null, font, SELECT_HINT, left() + WIDTH / 2, hintTop());
            return;
        }
        MapPopup.Box box = popup();
        MapPopup.frame(sprites, box);
        switch (pop) {
            case CROPS -> {
                CropKeys.sprites(sprites, box.innerX(), box.innerY(), pickRow, pickRows(), totals.crop(),
                        totals::canPick, mouseX, mouseY);
                if (pickScrolls()) {
                    Widgets.scrollbar(sprites, box.innerX() + CropKeys.width() + 2, box.innerY(),
                            CropKeys.height(pickRows()), pickRow, pickRows(), CropKeys.rows(), false);
                }
            }
            case FERTILIZE -> {
                Widgets.button(sprites, slotX(box, 0), box.innerY(), SLOT, SLOT, Widgets.Look.PLAIN,
                        boneLive(), mouseX, mouseY);
                Widgets.button(sprites, slotX(box, 1), box.innerY(), SLOT, SLOT,
                        pending == Pending.MUD ? Widgets.Look.DANGER : Widgets.Look.PLAIN, mudLive(), mouseX,
                        mouseY);
            }
            case CLEAR -> {
                for (int row = 0; row < 2; row++) {
                    boolean confirming = pending == (row == 0 ? Pending.UPROOT : Pending.REMOVE);
                    boolean live = row == 1 || uprootLive();
                    Widgets.button(sprites, box.innerX(), MapPopup.rowY(box, row), box.innerWidth(),
                            MapPopup.ROW_H, confirming ? Widgets.Look.DANGER : Widgets.Look.GHOST, live,
                            mouseX, mouseY);
                }
            }
            case NONE -> {}
        }
    }

    void fills(GuiGraphics graphics, FieldTotals totals) { barFills(graphics, totals.fields() > 0 ? totals.growth() : -1); }

    void texts(GuiGraphics graphics, Font font, FieldTotals totals) {
        Component title = totals.planned() == 0 ? Component.translatable(EarthSpiritScreen.UI + "map.card.fields",
                totals.fields())
                : totals.fields() == 0 ? Component.translatable(EarthSpiritScreen.UI + "map.card.plans",
                totals.planned())
                : Component.translatable(EarthSpiritScreen.UI + "map.card.fields_plans", totals.fields(),
                totals.planned());
        Component state = totals.fields() == 0 ? Component.translatable(EarthSpiritScreen.UI + "map.card.planned_state")
                : Component.translatable(EarthSpiritScreen.UI + "map.dock.state", totals.planted(), totals.cells(),
                totals.mature(), Math.clamp(totals.growth(), 0, 100));
        barTexts(graphics, font, title, state);
        if (pop == Pop.NONE) {
            MapHud.hintChip(null, graphics, font, SELECT_HINT, left() + WIDTH / 2, hintTop());
            return;
        }
        MapPopup.Box box = popup();
        switch (pop) {
            case CROPS -> {
                MapPopup.title(graphics, font, box, Component.translatable(UI + "pick_title"),
                        Component.translatable(UI + "pick_count", CropKeys.count()));
                CropKeys.texts(graphics, font, box.innerX(), box.innerY(), pickRow, pickRows(), land::seeds);
            }
            case FERTILIZE -> {
                MapPopup.title(graphics, font, box, Component.translatable(UI + "fertilize"), Component.empty());
                item(graphics, font, new ItemStack(Items.BONE_MEAL), slotX(box, 0), box.innerY(), land.boneMeal());
                item(graphics, font, new ItemStack(ModItems.ROTTEN_FROZEN_MUD.get()), slotX(box, 1), box.innerY(),
                        land.mud());
            }
            case CLEAR -> {
                MapPopup.title(graphics, font, box, Component.translatable(UI + "clear"), Component.empty());
                for (int row = 0; row < 2; row++) {
                    boolean confirming = pending == (row == 0 ? Pending.UPROOT : Pending.REMOVE);
                    Component text = confirming ? Component.translatable(CONFIRM).copy().append(" ")
                            .append(clearRow(row)) : clearRow(row);
                    Widgets.label(graphics, font, text, box.innerX(), MapPopup.rowY(box, row), box.innerWidth(),
                            MapPopup.ROW_H, confirming ? Widgets.Look.DANGER : Widgets.Look.GHOST,
                            row == 1 || uprootLive());
                }
            }
            case NONE -> {}
        }
    }

    private static void item(GuiGraphics graphics, Font font, ItemStack stack, int left, int top, long count) {
        graphics.renderItem(stack, left + 2, top + 2);
        String text = PanelDrawing.count(count);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.drawString(font, text, left + SLOT - 1 - font.width(text), top + SLOT - 9,
                count > 0 ? ModPalette.TEXT : ModPalette.TEXT_DEAD, true);
        graphics.pose().popPose();
    }

    boolean tip(MapPage page, GuiGraphics graphics, int mouseX, int mouseY) {
        if (pop != Pop.NONE && popup().contains(mouseX, mouseY)) return popupTip(page, graphics, mouseX, mouseY);
        int key = keyAt(mouseX, mouseY);
        if (key < 0) return false;
        Component text = switch (key) {
            case HARVEST -> Component.translatable(TIP + "harvest", totals.mature());
            default -> Component.translatable(TIP + KEYS[key]);
        };
        return keyTip(page, graphics, key, mouseX, mouseY, text);
    }

    private boolean popupTip(MapPage page, GuiGraphics graphics, int mouseX, int mouseY) {
        MapPopup.Box box = popup();
        switch (pop) {
            case CROPS -> {
                int crop = CropKeys.at(box.innerX(), box.innerY(), pickRow, pickRows(), mouseX, mouseY);
                int[] corner = crop == CropKeys.NONE ? null
                        : CropKeys.corner(box.innerX(), box.innerY(), crop, pickRow, pickRows());
                if (corner == null) return false;
                Component text = totals.canPick(crop)
                        ? Component.translatable(TIP + "crop", CropKeys.name(crop), land.seeds(crop))
                        : Component.translatable(TIP + "crop_taken", CropKeys.name(crop));
                return page.tipAt(graphics, corner[0], corner[1], CropKeys.SIZE, CropKeys.SIZE, mouseX, mouseY, text);
            }
            case FERTILIZE -> {
                int slot = slotAt(box, mouseX, mouseY);
                if (slot < 0) return false;
                Component text = slot == 0 ? Component.translatable(TIP + "ripen", totals.growing(), land.boneMeal())
                        : Component.translatable(TIP + "gu_plant", FieldTotals.cost(), totals.mudNeed(), land.mud());
                return page.tipAt(graphics, slotX(box, slot), box.innerY(), SLOT, SLOT, mouseX, mouseY, text);
            }
            case CLEAR -> {
                int row = MapPopup.rowAt(box, 2, box.innerWidth(), mouseX, mouseY);
                if (row < 0) return false;
                Component text = row == 0 ? Component.translatable(TIP + "uproot", totals.planted())
                        : Component.translatable(TIP + "remove");
                return page.tipAt(graphics, box.innerX(), MapPopup.rowY(box, row), box.innerWidth(), MapPopup.ROW_H,
                        mouseX, mouseY, text);
            }
            case NONE -> {
                return false;
            }
        }
        return false;
    }
    //endregion

    //region Input [输入]
    void click(MapSelection selection, FieldTotals totals, double mouseX, double mouseY) {
        sync(selection, totals);
        Pending was = pending;
        pending = Pending.NONE;
        List<Long> chunks = selection.chunks();
        if (pop != Pop.NONE && popup().contains(mouseX, mouseY)) {
            clickPopup(selection, chunks, was, mouseX, mouseY);
            return;
        }
        int key = keyAt(mouseX, mouseY);
        if (key < 0 || !live(key)) {
            pop = Pop.NONE;
            return;
        }
        Pop wanted = popOf(key);
        if (wanted != Pop.NONE) {
            pop = pop == wanted ? Pop.NONE : wanted;
            if (pop == Pop.CROPS) {
                pickRow = totals.crop() < 0 ? 0
                        : Widgets.clampScroll(totals.crop() / CropKeys.COLUMNS, pickRows(), CropKeys.rows());
            }
            return;
        }
        pop = Pop.NONE;
        if (key == HARVEST) land.harvest(chunks);
    }

    private void clickPopup(MapSelection selection, List<Long> chunks, Pending was, double mouseX, double mouseY) {
        MapPopup.Box box = popup();
        switch (pop) {
            case CROPS -> {
                int crop = CropKeys.at(box.innerX(), box.innerY(), pickRow, pickRows(), mouseX, mouseY);
                if (crop == CropKeys.NONE || !totals.canPick(crop)) return;
                land.sow(chunks, crop);
                pop = Pop.NONE;
            }
            case FERTILIZE -> {
                int slot = slotAt(box, mouseX, mouseY);
                if (slot == 0 && boneLive()) {
                    land.ripen(chunks);
                    pop = Pop.NONE;
                } else if (slot == 1 && mudLive()) {
                    confirm(was, Pending.MUD, () -> land.retype(chunks, FieldKind.GU_PLANT.ordinal()));
                }
            }
            case CLEAR -> {
                int row = MapPopup.rowAt(box, 2, box.innerWidth(), mouseX, mouseY);
                if (row == 0 && uprootLive()) {
                    confirm(was, Pending.UPROOT, () -> land.uproot(chunks));
                } else if (row == 1) {
                    confirm(was, Pending.REMOVE, () -> {
                        land.clear(chunks);
                        selection.clear();
                    });
                }
            }
            case NONE -> {}
        }
    }

    private void confirm(Pending was, Pending step, Runnable action) {
        if (was == step) {
            action.run();
            pop = Pop.NONE;
        } else {
            pending = step;
        }
    }

    /** 滚轮在作物格上：翻行；返回真即吃掉。 */
    boolean scroll(double mouseX, double mouseY, double delta) {
        if (pop != Pop.CROPS || !popup().contains(mouseX, mouseY)) return false;
        pickRow = Widgets.clampScroll(pickRow - (int) Math.signum(delta), pickRows(), CropKeys.rows());
        return true;
    }
    //endregion
}
