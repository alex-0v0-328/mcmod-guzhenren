package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldKind;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 田坞：选中田后的底部操作栏（{@code overlay} 底，顶替工具栏，Alex 定取代左下选区卡）。表头一行左写「已选 N 块田」、右写
 * 「已种 / 熟 / 生长 %」，下面一条生长条；再下一排键分三组，组间竖线隔开：种收（当前作物、种植、催熟、收获）、土质（普通田、
 * 蛊植田）、清理（清除作物、拆除）。上方一枚 {@code chip} 写选区按键「[Shift] 加选 [Ctrl+A] 全选 [Esc] 取消选择」。
 * <ul>
 *   <li>两档（{@link #layout}）：界面宽不到 {@value #WIDE_MIN_W} 或高不到 {@value #WIDE_MIN_H} 用紧凑档（组距
 *       {@value #COMPACT_SPLIT}、不写组名），否则宽档（组距 {@value #WIDE_SPLIT}、键下写组名）。缩放 6 的 2560×1600 即紧凑档。</li>
 *   <li>种植两步（Alex 定）：当前作物键点开选种弹窗（{@link CropKeys} 网格，每行 {@value CropKeys#COLUMNS} 个、可见
 *       {@value #PICK_ROWS} 行，多了滚轮翻），点一种只选定，再点种植键才扣种子补满空格；田里有别的作物时那种作物失效，要先清除
 *       作物（{@link FieldTotals#canPick}）。选区一变就重选为所选各田共同的作物并收起弹窗。</li>
 *   <li>需 / 有写在悬停提示，不够或无事可做时键失效（拆除不失效）；只选了规划田时只有拆除（= 取消规划）可点。</li>
 *   <li>清除作物、土质与拆除两步：第一次点键变危险色、键上写「确认」，再点才发；选区一变或点别处即撤销。拆除平时不用危险色
 *       （红色只给紧急）。</li>
 *   <li>图标一图一概念（Alex 定稿）：种植 {@code act_plant}（种子 + ↓ 角标，不借开垦的 {@code tool_farm}）、催熟
 *       {@code act_ripen}、收获镰刀 {@code act_harvest}、普通田 {@code soil_plain} 与蛊植田 {@code soil_gu_plant}（同一块田，
 *       后者加蛊角标）、清除作物铲子 {@code act_uproot}（与镰刀分开）、拆除 {@code tool_remove}；只有当前作物键写种子图标。</li>
 * </ul>
 */

final class MapActionBar {

    private static final int KEY = 20;
    private static final int ICON_W = 22;
    private static final int CROP_W = 30;
    private static final int GAP = 2;
    private static final int SIDE = 6;
    private static final int BOTTOM = 6;
    private static final int TITLE_Y = 4;
    private static final int BAR_Y = 15;
    private static final int KEYS_Y = 20;
    private static final int COMPACT_H = KEYS_Y + KEY + 4;
    private static final int WIDE_H = COMPACT_H + 12;
    private static final int COMPACT_SPLIT = 8;
    private static final int WIDE_SPLIT = 12;
    private static final int WIDE_MIN_W = 560;
    private static final int WIDE_MIN_H = 320;
    private static final int PICK_PAD = 6;
    private static final int PICK_ROWS = 2;
    private static final int PICK_TITLE = 12;
    private static final int RULE = 0xFF4A4045;
    private static final int TRACK = 0xFF2A2328;
    private static final String UI = EarthSpiritScreen.UI + "map.act.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_act_";
    private static final String CONFIRM = EarthSpiritScreen.UI + "map.card.confirm";
    private static final String[][] SELECT_HINT = {{"shift", "add"}, {"select_all", "select_all"},
            {"esc", "deselect"}};

    private enum Pending { NONE, UPROOT, PLAIN, GU_PLANT, CLEAR }

    private enum Key { CROP, PLANT, RIPEN, HARVEST, PLAIN, GU_PLANT, UPROOT, CLEAR }

    private enum Group {
        GROW("grow", Key.CROP, Key.HARVEST), SOIL("soil", Key.PLAIN, Key.GU_PLANT), TIDY("tidy", Key.UPROOT, Key.CLEAR);

        private final String name;
        private final Key first;
        private final Key last;

        Group(String name, Key first, Key last) {
            this.name = name;
            this.first = first;
            this.last = last;
        }
    }

    private final int[] lefts = new int[Key.values().length];
    private boolean wide;
    private int boxX;
    private int boxY;
    private int boxW;
    private int boxH;
    private Pending pending = Pending.NONE;
    private int seenRevision = -1;
    private int picked = CropKeys.NONE;
    private boolean picking;
    private int pickRow;

    //region Layout [排版]
    /** 宽档还是紧凑档；工具栏同档。 */
    static boolean wide(int width, int height) { return width >= WIDE_MIN_W && height >= WIDE_MIN_H; }

    void layout(int width, int height) {
        wide = wide(width, height);
        int x = SIDE;
        for (Group group : Group.values()) {
            if (group != Group.GROW) x += split() - GAP;
            for (int key = group.first.ordinal(); key <= group.last.ordinal(); key++) {
                lefts[key] = x;
                x += width(Key.values()[key]) + GAP;
            }
        }
        boxW = x - GAP + SIDE;
        boxH = wide ? WIDE_H : COMPACT_H;
        boxX = (width - boxW) / 2;
        boxY = height - BOTTOM - boxH;
    }

    int boxWidth() { return boxW; }

    /** 田坞的 x、y、宽、高（排版自检用）。 */
    int[] box() { return new int[] {boxX, boxY, boxW, boxH}; }

    /** 田坞连同上方提示条的顶边。 */
    int hintTop() { return boxY - 5 - MapHud.CHIP_H; }

    private int split() { return wide ? WIDE_SPLIT : COMPACT_SPLIT; }

    private static int width(Key key) {
        return switch (key) {
            case CROP -> CROP_W;
            default -> ICON_W;
        };
    }

    private int left(Key key) { return boxX + lefts[key.ordinal()]; }

    private int keyY() { return boxY + KEYS_Y; }

    boolean contains(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, boxX, boxY, boxW, boxH)
                || picking && Widgets.isOver(mouseX, mouseY, pickX(), pickY(), pickW(), pickH());
    }

    private @Nullable Key at(double mouseX, double mouseY) {
        for (Key key : Key.values()) {
            if (Widgets.isOver(mouseX, mouseY, left(key), keyY(), width(key), KEY)) return key;
        }
        return null;
    }

    private static int pickRows() { return Math.min(PICK_ROWS, CropKeys.rows()); }

    private static boolean pickScrolls() { return CropKeys.rows() > PICK_ROWS; }

    private static int pickW() { return PICK_PAD + CropKeys.width() + (pickScrolls() ? 2 + 6 : 0) + PICK_PAD; }

    private static int pickH() { return PICK_PAD + PICK_TITLE + CropKeys.height(pickRows()) + PICK_PAD; }

    private int pickX() { return Math.max(SIDE, left(Key.CROP) - PICK_PAD); }

    private int pickY() { return boxY - 4 - pickH(); }

    private int gridX() { return pickX() + PICK_PAD; }

    private int gridY() { return pickY() + PICK_PAD + PICK_TITLE; }
    //endregion

    //region State [状态]
    private void sync(MapSelection selection, FieldTotals totals) {
        if (selection.revision() == seenRevision) return;
        seenRevision = selection.revision();
        pending = Pending.NONE;
        picked = totals.crop() >= 0 ? totals.crop() : CropKeys.NONE;
        picking = false;
        pickRow = 0;
    }

    boolean picking() { return picking; }

    void cancel() {
        pending = Pending.NONE;
        picking = false;
    }

    private boolean live(Key key, FieldTotals totals, MapTools.Land land) {
        if (totals.fields() == 0) return key == Key.CLEAR;
        return switch (key) {
            case CROP, CLEAR -> true;
            case PLANT -> totals.canSow(picked) && land.seeds(picked) > 0;
            case RIPEN -> totals.growing() > 0 && land.boneMeal() > 0;
            case HARVEST -> totals.mature() > 0;
            case PLAIN -> totals.guPlant() > 0;
            case GU_PLANT -> totals.plain() > 0 && land.mud() >= totals.mudNeed();
            case UPROOT -> totals.planted() > 0;
        };
    }

    private @Nullable Pending pendingOf(Key key) {
        return switch (key) {
            case UPROOT -> Pending.UPROOT;
            case PLAIN -> Pending.PLAIN;
            case GU_PLANT -> Pending.GU_PLANT;
            case CLEAR -> Pending.CLEAR;
            default -> null;
        };
    }

    private boolean confirming(Key key) { return pending != Pending.NONE && pending == pendingOf(key); }

    private PanelDrawing.Look look(Key key, FieldTotals totals) {
        if (confirming(key)) return PanelDrawing.Look.DANGER;
        return switch (key) {
            case CROP -> picked == CropKeys.NONE ? PanelDrawing.Look.GHOST : PanelDrawing.Look.PLAIN;
            case PLAIN -> totals.fields() > 0 && totals.guPlant() == 0 ? PanelDrawing.Look.SELECTED
                    : PanelDrawing.Look.PLAIN;
            case GU_PLANT -> totals.fields() > 0 && totals.plain() == 0 ? PanelDrawing.Look.SELECTED
                    : PanelDrawing.Look.PLAIN;
            default -> PanelDrawing.Look.PLAIN;
        };
    }

    private static @Nullable String icon(Key key) {
        return switch (key) {
            case PLANT -> "act_plant";
            case PLAIN -> "soil_plain";
            case GU_PLANT -> "soil_gu_plant";
            case RIPEN -> "act_ripen";
            case HARVEST -> "act_harvest";
            case UPROOT -> "act_uproot";
            case CLEAR -> "tool_remove";
            default -> null;
        };
    }
    //endregion

    //region Drawing [绘制]
    void sprites(SpriteBatch sprites, FieldTotals totals, MapTools.Land land, MapSelection selection, Font font,
                 double mouseX, double mouseY) {
        sync(selection, totals);
        sprites.blit(PanelDrawing.OVERLAY, boxX, boxY, boxW, boxH);
        for (Key key : Key.values()) {
            PanelDrawing.Look look = look(key, totals);
            boolean live = live(key, totals, land);
            PanelDrawing.button(sprites, left(key), keyY(), width(key), KEY, look,
                    live || look == PanelDrawing.Look.SELECTED, mouseX, mouseY);
            String icon = confirming(key) ? null : icon(key);
            if (icon == null) continue;
            int sunk = !live || PanelDrawing.pressed(left(key), keyY(), width(key), KEY) ? 1 : 0;
            sprites.blit(PanelDrawing.sprite(icon), left(key) + 3, keyY() + 2 + sunk, 16, 16,
                    live ? 0xFFFFFFFF : PanelDrawing.DIM_ICON);
        }
        if (picking) {
            sprites.blit(PanelDrawing.POPUP, pickX(), pickY(), pickW(), pickH());
            CropKeys.sprites(sprites, gridX(), gridY(), pickRow, pickRows(), picked, totals::canPick, mouseX, mouseY);
            if (pickScrolls()) {
                PanelDrawing.scrollbar(sprites, gridX() + CropKeys.width() + 2, gridY(), CropKeys.height(pickRows()),
                        pickRow, pickRows(), CropKeys.rows(), false);
            }
        } else {
            MapHud.hintChip(sprites, null, font, SELECT_HINT, boxX + boxW / 2, hintTop());
        }
    }

    void fills(GuiGraphics graphics, FieldTotals totals) {
        int barLeft = boxX + SIDE;
        int barRight = boxX + boxW - SIDE;
        graphics.fill(barLeft, boxY + BAR_Y, barRight, boxY + BAR_Y + 2, TRACK);
        int filled = (barRight - barLeft) * Math.clamp(totals.growth(), 0, 100) / 100;
        if (totals.fields() > 0 && filled > 0) {
            graphics.fill(barLeft, boxY + BAR_Y, barLeft + filled, boxY + BAR_Y + 2, PanelDrawing.ACCENT);
        }
        for (Group group : Group.values()) {
            if (group == Group.GROW) continue;
            int rule = left(group.first) - split() / 2 + GAP / 2;
            graphics.fill(rule, keyY() + 2, rule + 1, keyY() + KEY - 2, RULE);
        }
    }

    void texts(GuiGraphics graphics, Font font, FieldTotals totals, MapTools.Land land) {
        Component title = totals.planned() == 0 ? Component.translatable(EarthSpiritScreen.UI + "map.card.fields",
                totals.fields())
                : totals.fields() == 0 ? Component.translatable(EarthSpiritScreen.UI + "map.card.plans",
                totals.planned())
                : Component.translatable(EarthSpiritScreen.UI + "map.card.fields_plans", totals.fields(),
                totals.planned());
        graphics.drawString(font, title, boxX + SIDE, boxY + TITLE_Y, PanelDrawing.ACCENT, false);
        Component state = totals.fields() == 0 ? Component.translatable(EarthSpiritScreen.UI + "map.card.planned_state")
                : Component.translatable(EarthSpiritScreen.UI + "map.dock.state", totals.planted(), totals.cells(),
                totals.mature(), Math.clamp(totals.growth(), 0, 100));
        PanelDrawing.right(graphics, font, state, boxX + boxW - SIDE, boxY + TITLE_Y, PanelDrawing.LEGEND);
        for (Key key : Key.values()) {
            PanelDrawing.Look look = look(key, totals);
            boolean live = live(key, totals, land) || look == PanelDrawing.Look.SELECTED;
            if (confirming(key)) {
                PanelDrawing.label(graphics, font, Component.translatable(CONFIRM), left(key), keyY(), width(key), KEY,
                        look, true);
            } else if (key == Key.CROP) {
                cropKey(graphics, font, land, live);
            }
        }
        if (wide) {
            int labelY = keyY() + KEY + 3;
            for (Group group : Group.values()) {
                int from = left(group.first);
                int to = left(group.last) + width(group.last);
                PanelDrawing.centered(graphics, font, Component.translatable(UI + group.name), from, to, labelY,
                        PanelDrawing.TEXT);
            }
        }
        if (picking) {
            graphics.drawString(font, Component.translatable(UI + "pick_title"), pickX() + PICK_PAD,
                    pickY() + PICK_PAD, PanelDrawing.ACCENT, false);
            PanelDrawing.right(graphics, font, Component.translatable(UI + "pick_count", CropKeys.count()),
                    pickX() + pickW() - PICK_PAD, pickY() + PICK_PAD, PanelDrawing.LEGEND);
            CropKeys.texts(graphics, font, gridX(), gridY(), pickRow, pickRows(), land::seeds);
        } else {
            MapHud.hintChip(null, graphics, font, SELECT_HINT, boxX + boxW / 2, hintTop());
        }
    }

    /** 当前作物键：选定了就画种子图标与库藏数，没选写一个 ▾（虚线空键）。 */
    private void cropKey(GuiGraphics graphics, Font font, MapTools.Land land, boolean live) {
        int left = left(Key.CROP);
        Component arrow = Component.literal("▾");
        if (picked == CropKeys.NONE) {
            PanelDrawing.label(graphics, font, arrow, left, keyY(), CROP_W, KEY, PanelDrawing.Look.GHOST, live);
            return;
        }
        CropKeys.seed(graphics, font, picked, left, keyY(), CROP_W, land.seeds(picked));
        graphics.drawString(font, arrow, left + CROP_W - 2 - font.width(arrow), keyY() + 2, PanelDrawing.LEGEND,
                false);
    }

    boolean tip(MapPage page, GuiGraphics graphics, FieldTotals totals, MapTools.Land land, int mouseX,
                int mouseY) {
        if (picking) {
            int crop = CropKeys.at(gridX(), gridY(), pickRow, pickRows(), mouseX, mouseY);
            if (crop != CropKeys.NONE) {
                int[] corner = CropKeys.corner(gridX(), gridY(), crop, pickRow, pickRows());
                Component text = totals.canPick(crop)
                        ? Component.translatable(TIP + "crop", CropKeys.name(crop), land.seeds(crop))
                        : Component.translatable(TIP + "crop_taken", CropKeys.name(crop));
                return corner != null && page.tipAt(graphics, corner[0], corner[1], CropKeys.SIZE, CropKeys.SIZE,
                        mouseX, mouseY, text);
            }
        }
        Key key = at(mouseX, mouseY);
        if (key == null) return false;
        Component text = switch (key) {
            case CROP -> picked == CropKeys.NONE ? Component.translatable(TIP + "pick")
                    : Component.translatable(TIP + "picked", CropKeys.name(picked), land.seeds(picked));
            case PLANT -> picked == CropKeys.NONE ? Component.translatable(TIP + "plant_pick")
                    : Component.translatable(TIP + "plant", CropKeys.name(picked), land.seeds(picked));
            case RIPEN -> Component.translatable(TIP + "ripen", totals.growing(), land.boneMeal());
            case HARVEST -> Component.translatable(TIP + "harvest", totals.mature());
            case PLAIN -> Component.translatable(TIP + "plain");
            case GU_PLANT -> Component.translatable(TIP + "gu_plant", FieldTotals.cost(), totals.mudNeed(),
                    land.mud());
            case UPROOT -> Component.translatable(TIP + "uproot", totals.planted());
            case CLEAR -> Component.translatable(TIP + "clear");
        };
        return page.tipAt(graphics, left(key), keyY(), width(key), KEY, mouseX, mouseY, text);
    }
    //endregion

    //region Input [输入]
    void click(MapTools.Land land, MapSelection selection, FieldTotals totals, double mouseX, double mouseY) {
        sync(selection, totals);
        Pending was = pending;
        pending = Pending.NONE;
        if (picking && Widgets.isOver(mouseX, mouseY, pickX(), pickY(), pickW(), pickH())) {
            int crop = CropKeys.at(gridX(), gridY(), pickRow, pickRows(), mouseX, mouseY);
            if (crop != CropKeys.NONE && totals.canPick(crop)) {
                picked = crop;
                picking = false;
            }
            return;
        }
        Key key = at(mouseX, mouseY);
        if (key != Key.CROP) picking = false;
        if (key == null || !live(key, totals, land)) return;
        List<Long> chunks = selection.chunks();
        switch (key) {
            case CROP -> {
                picking = !picking;
                pickRow = picked == CropKeys.NONE ? 0 : PanelDrawing.clampScroll(picked / CropKeys.COLUMNS,
                        pickRows(), CropKeys.rows());
            }
            case PLANT -> land.sow(chunks, picked);
            case RIPEN -> land.ripen(chunks);
            case HARVEST -> land.harvest(chunks);
            case PLAIN -> confirm(was, Pending.PLAIN, () -> land.retype(chunks, FieldKind.PLAIN.ordinal()));
            case GU_PLANT -> confirm(was, Pending.GU_PLANT, () -> land.retype(chunks, FieldKind.GU_PLANT.ordinal()));
            case UPROOT -> confirm(was, Pending.UPROOT, () -> {
                land.uproot(chunks);
                picked = CropKeys.NONE;
            });
            case CLEAR -> confirm(was, Pending.CLEAR, () -> {
                land.clear(chunks);
                selection.clear();
            });
        }
    }

    private void confirm(Pending was, Pending step, Runnable action) {
        if (was == step) action.run();
        else pending = step;
    }

    /** 滚轮在选种弹窗上：翻行；返回真即吃掉。 */
    boolean scroll(double mouseX, double mouseY, double delta) {
        if (!picking || !Widgets.isOver(mouseX, mouseY, pickX(), pickY(), pickW(), pickH())) return false;
        pickRow = PanelDrawing.clampScroll(pickRow - (int) Math.signum(delta), pickRows(), CropKeys.rows());
        return true;
    }
    //endregion
}
