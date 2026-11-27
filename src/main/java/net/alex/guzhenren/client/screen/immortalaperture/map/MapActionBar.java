package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.List;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmState;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldKind;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 选中田后的底部操作栏（{@code overlay} 底，顶替工具栏）：种植（一排 {@link CropKeys}）、土质（普通 / 蛊植）、催熟、收获、
 * 拆除，组下写组名，组间竖线隔开；上方一枚 {@code chip} 写选区按键「[Shift] 加选 [Ctrl+A] 全选 [Esc] 取消选择」。
 * <ul>
 *   <li>数量与需 / 有写在选区卡（{@link MapSelectionCard}），精确消耗在悬停提示；不够时键失效（种植与拆除不失效）。</li>
 *   <li>土质与拆除两步：第一次点键变危险色并写「确认」，再点才发；选区一变（{@link MapSelection#revision}）或点别处即撤销。
 *       土质两个方向都不退腐泥冻土，拆除收不回耕地，所以都要确认。</li>
 *   <li>只选了规划田时只有拆除（= 取消规划）可点。</li>
 * </ul>
 */

final class MapActionBar {

    private static final int KEY = CropKeys.SIZE;
    private static final int ICON_W = 22;
    private static final int SOIL_W = 30;
    private static final int SIDE = 8;
    private static final int SPLIT = 12;
    private static final int BOTTOM = 7;
    private static final int BOX_H = 5 + KEY + 3 + 9 + 5;
    private static final int RULE = 0xFF4A4045;
    private static final String UI = EarthSpiritScreen.UI + "map.act.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_act_";
    private static final String CONFIRM = EarthSpiritScreen.UI + "map.card.confirm";
    private static final int CROPS_X = SIDE;
    private static final int SOIL_X = CROPS_X + CropKeys.width() + SPLIT;
    private static final int RIPEN_X = SOIL_X + 2 * SOIL_W + 2 + SPLIT;
    private static final int HARVEST_X = RIPEN_X + ICON_W + SPLIT;
    private static final int CLEAR_X = HARVEST_X + ICON_W + SPLIT;
    private static final int BOX_W = CLEAR_X + ICON_W + SIDE;

    private enum Pending { NONE, PLAIN, GU_PLANT, CLEAR }

    private enum Key { PLAIN, GU_PLANT, RIPEN, HARVEST, CLEAR }

    private int boxX;
    private int boxY;
    private Pending pending = Pending.NONE;
    private int seenRevision;

    void layout(int width, int height) {
        boxX = (width - BOX_W) / 2;
        boxY = height - BOTTOM - BOX_H;
    }

    boolean contains(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, boxX, boxY, BOX_W, BOX_H);
    }

    private int keyY() { return boxY + 5; }

    private int left(Key key) {
        return boxX + switch (key) {
            case PLAIN -> SOIL_X;
            case GU_PLANT -> SOIL_X + SOIL_W + 2;
            case RIPEN -> RIPEN_X;
            case HARVEST -> HARVEST_X;
            case CLEAR -> CLEAR_X;
        };
    }

    private static int width(Key key) { return key == Key.PLAIN || key == Key.GU_PLANT ? SOIL_W : ICON_W; }

    private @Nullable Key at(double mouseX, double mouseY) {
        for (Key key : Key.values()) {
            if (Widgets.isOver(mouseX, mouseY, left(key), keyY(), width(key), KEY)) return key;
        }
        return null;
    }

    private void sync(MapSelection selection) {
        if (selection.revision() == seenRevision) return;
        seenRevision = selection.revision();
        pending = Pending.NONE;
    }

    private static boolean live(Key key, FieldTotals totals, MapTools.Land land) {
        return switch (key) {
            case PLAIN -> totals.guPlant() > 0;
            case GU_PLANT -> totals.plain() > 0 && land.mud() >= totals.mudNeed();
            case RIPEN -> totals.growing() > 0 && land.boneMeal() > 0;
            case HARVEST -> totals.mature() > 0;
            case CLEAR -> true;
        };
    }

    private PanelDrawing.Look look(Key key, FieldTotals totals) {
        return switch (key) {
            case PLAIN -> pending == Pending.PLAIN ? PanelDrawing.Look.DANGER
                    : totals.fields() > 0 && totals.guPlant() == 0 ? PanelDrawing.Look.SELECTED
                    : PanelDrawing.Look.PLAIN;
            case GU_PLANT -> pending == Pending.GU_PLANT ? PanelDrawing.Look.DANGER
                    : totals.fields() > 0 && totals.plain() == 0 ? PanelDrawing.Look.SELECTED
                    : PanelDrawing.Look.PLAIN;
            case CLEAR -> PanelDrawing.Look.DANGER;
            default -> PanelDrawing.Look.PLAIN;
        };
    }

    //region Drawing [绘制]
    void sprites(SpriteBatch sprites, FieldTotals totals, MapTools.Land land, MapSelection selection, Font font,
                 double mouseX, double mouseY) {
        sync(selection);
        sprites.blit(PanelDrawing.OVERLAY, boxX, boxY, BOX_W, BOX_H);
        CropKeys.sprites(sprites, boxX + CROPS_X, keyY(), totals.crop(), totals.fields() > 0, mouseX, mouseY);
        for (Key key : Key.values()) {
            PanelDrawing.Look look = look(key, totals);
            boolean live = live(key, totals, land) && (totals.fields() > 0 || key == Key.CLEAR);
            PanelDrawing.button(sprites, left(key), keyY(), width(key), KEY, look, live || look
                    == PanelDrawing.Look.SELECTED, mouseX, mouseY);
            String icon = switch (key) {
                case RIPEN -> "act_ripen";
                case HARVEST -> "act_harvest";
                case CLEAR -> "tool_remove";
                default -> null;
            };
            if (icon != null) {
                int sunk = !live || PanelDrawing.pressed(left(key), keyY(), width(key), KEY) ? 1 : 0;
                sprites.blit(PanelDrawing.sprite(icon), left(key) + 3, keyY() + 2 + sunk, 16, 16,
                        live ? 0xFFFFFFFF : PanelDrawing.DIM_ICON);
            }
        }
        hint(sprites, null, font);
    }

    void fills(GuiGraphics graphics) {
        for (int x : new int[] {SOIL_X, RIPEN_X, HARVEST_X, CLEAR_X}) {
            int rule = boxX + x - SPLIT / 2;
            graphics.fill(rule, keyY() + 2, rule + 1, boxY + BOX_H - 6, RULE);
        }
    }

    void texts(GuiGraphics graphics, Font font, FieldTotals totals, MapTools.Land land) {
        CropKeys.texts(graphics, font, boxX + CROPS_X, keyY(), totals.crop(), totals.fields() > 0, land::seeds);
        for (Key key : new Key[] {Key.PLAIN, Key.GU_PLANT}) {
            PanelDrawing.Look look = look(key, totals);
            boolean confirming = pending == (key == Key.PLAIN ? Pending.PLAIN : Pending.GU_PLANT);
            Component text = confirming ? Component.translatable(CONFIRM)
                    : Component.translatable(EarthSpiritScreen.UI + "map.card.kind_"
                            + (key == Key.PLAIN ? "plain" : "gu_plant"));
            PanelDrawing.label(graphics, font, text, left(key), keyY(), SOIL_W, KEY, look,
                    look == PanelDrawing.Look.SELECTED || live(key, totals, land) && totals.fields() > 0);
        }
        int labelY = keyY() + KEY + 3;
        group(graphics, font, UI + "plant", boxX + CROPS_X, CropKeys.width(), labelY, false);
        group(graphics, font, UI + "soil", boxX + SOIL_X, 2 * SOIL_W + 2, labelY, false);
        group(graphics, font, UI + "ripen", boxX + RIPEN_X, ICON_W, labelY, false);
        group(graphics, font, UI + "harvest", boxX + HARVEST_X, ICON_W, labelY, false);
        group(graphics, font, pending == Pending.CLEAR ? CONFIRM : UI + "clear", boxX + CLEAR_X, ICON_W, labelY,
                pending == Pending.CLEAR);
        hint(null, graphics, font);
    }

    private static void group(GuiGraphics graphics, Font font, String key, int left, int width, int y,
                              boolean accent) {
        PanelDrawing.centered(graphics, font, Component.translatable(key), left - 4, left + width + 4, y,
                accent ? PanelDrawing.ACCENT : PanelDrawing.TEXT);
    }

    /** 操作栏上方的选区按键提示；画图与写字两步共用这份排版。 */
    private void hint(@Nullable SpriteBatch sprites, @Nullable GuiGraphics graphics, Font font) {
        String[][] phrases = {{"shift", "add"}, {"select_all", "select_all"}, {"esc", "deselect"}};
        int width = 6;
        for (String[] phrase : phrases) {
            width += PanelDrawing.keycapWidth(font, key(phrase[0])) + 3 + font.width(hintText(phrase[1])) + 8;
        }
        width -= 2;
        int x = boxX + (BOX_W - width) / 2;
        int y = boxY - 5 - 16;
        if (sprites != null) sprites.blit(PanelDrawing.CHIP, x, y, width, 16);
        x += 6;
        for (String[] phrase : phrases) {
            x = MapHud.keyPhrase(sprites, graphics, font, key(phrase[0]), hintText(phrase[1]), x, y) + 8;
        }
    }

    private static Component key(String name) {
        return Component.translatable(EarthSpiritScreen.UI + "map.key." + name);
    }

    private static Component hintText(String name) {
        return Component.translatable(EarthSpiritScreen.UI + "map.hint." + name);
    }

    boolean tip(MapPage page, GuiGraphics graphics, FieldTotals totals, MapTools.Land land, int mouseX,
                int mouseY) {
        int crop = CropKeys.at(boxX + CROPS_X, keyY(), mouseX, mouseY);
        if (crop != CropKeys.NONE) {
            Component text = crop == FarmState.AUTO_CROP ? Component.translatable(TIP + "crop_auto")
                    : Component.translatable(TIP + "crop", CropKeys.name(crop), land.seeds(crop));
            return page.tipAt(graphics, CropKeys.left(boxX + CROPS_X, crop), keyY(), KEY, KEY, mouseX, mouseY, text);
        }
        Key key = at(mouseX, mouseY);
        if (key == null) return false;
        Component text = switch (key) {
            case PLAIN -> Component.translatable(TIP + "plain");
            case GU_PLANT -> Component.translatable(TIP + "gu_plant", FieldTotals.cost(), totals.mudNeed(),
                    land.mud());
            case RIPEN -> Component.translatable(TIP + "ripen", totals.growing(), land.boneMeal());
            case HARVEST -> Component.translatable(TIP + "harvest", totals.mature());
            case CLEAR -> Component.translatable(TIP + "clear");
        };
        return page.tipAt(graphics, left(key), keyY(), width(key), KEY, mouseX, mouseY, text);
    }
    //endregion

    //region Input [输入]
    void click(MapTools.Land land, MapSelection selection, FieldTotals totals, double mouseX, double mouseY) {
        sync(selection);
        Pending was = pending;
        pending = Pending.NONE;
        List<Long> chunks = selection.chunks();
        int crop = CropKeys.at(boxX + CROPS_X, keyY(), mouseX, mouseY);
        if (crop != CropKeys.NONE) {
            if (totals.fields() > 0) land.sow(chunks, crop);
            return;
        }
        Key key = at(mouseX, mouseY);
        if (key == null || totals.fields() == 0 && key != Key.CLEAR || !live(key, totals, land)) return;
        switch (key) {
            case PLAIN -> confirm(was, Pending.PLAIN, () -> land.retype(chunks, FieldKind.PLAIN.ordinal()));
            case GU_PLANT -> confirm(was, Pending.GU_PLANT, () -> land.retype(chunks, FieldKind.GU_PLANT.ordinal()));
            case RIPEN -> land.ripen(chunks);
            case HARVEST -> land.harvest(chunks);
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

    void cancel() { pending = Pending.NONE; }
    //endregion
}
