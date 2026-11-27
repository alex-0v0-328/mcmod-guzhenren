package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.List;
import java.util.Objects;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdRules;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.MapPart;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandHostingJob;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * 选区卡：固定在地图左下（按键提示上方），写选区的状态；田的操作在底部操作栏 {@link MapActionBar}，牧场的设置与按钮在卡上。
 * 原先点占地在光标旁弹卡，挡地图且一次只看一块（Alex 定改为固定检视卡）。
 * <ul>
 *   <li>田（可多选）：标题「已选 N 块田」（含规划田时补「· 规划 M」）；已种 / 耕地格与熟株；按已种株加权的生长进度条；骨粉
 *       「需 未熟株 / 有 库藏」、腐泥冻土「需 普通田 × 192 / 有 库藏」，不够时字标警示色；蛊植田数 / 田数。只选了规划田
 *       时只写「等地灵开垦」。</li>
 *   <li>牧场（单选）：栏内头数、自动收拢开关、收拢种类列表（每行一个开关，一屏 {@value #TYPE_ROWS} 行，多了滚轮翻；关掉 =
 *       这个牧场不收这种）、「收拢」「迁移」「拆除」。</li>
 * </ul>
 */

final class MapSelectionCard {

    private static final int WIDTH = 160;
    private static final int LEFT = 6;
    private static final int PAD = 6;
    private static final int TEXT_X = 26;
    private static final int LINE = 12;
    private static final int GROWTH_Y = 30;
    private static final int GROWTH_X = 36;
    private static final int GROWTH_W = 88;
    private static final int BONE_Y = GROWTH_Y + LINE;
    private static final int MUD_Y = BONE_Y + LINE;
    private static final int KIND_Y = MUD_Y + LINE;
    private static final int FIELD_HEIGHT = KIND_Y + 9 + PAD;
    private static final int PLANNED_HEIGHT = 30;
    private static final int BUTTON_W = 46;
    private static final int BUTTON_H = 16;
    private static final int BUTTON_GAP = 4;
    private static final int AUTO_Y = 32;
    private static final int TYPES_LABEL_Y = 46;
    private static final int TYPES_Y = 56;
    private static final int TYPE_ROW_H = 12;
    private static final int TYPE_ROWS = 4;
    private static final int PASTURE_HEIGHT = TYPES_Y + TYPE_ROWS * TYPE_ROW_H + 4 + BUTTON_H + PAD;
    private static final String UI = EarthSpiritScreen.UI + "map.card.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_card_";
    private static final String[] PEN_BUTTONS = {"gather", "move", "remove"};
    private int bottom;
    private int top;
    private int height;
    private int typeOffset;
    private @Nullable Long shownPasture;

    void layout(int bottom) { this.bottom = bottom; }

    private static int height(MapSelection selection, FieldTotals totals) {
        if (selection.isEmpty()) return 0;
        if (!selection.hasFields()) return PASTURE_HEIGHT;
        return totals.fields() == 0 ? PLANNED_HEIGHT : FIELD_HEIGHT;
    }

    /** 每帧先排版：按选区定卡高与卡顶；换了牧场就把种类列表滚回顶。 */
    void place(MapSelection selection, FieldTotals totals, MapPlots.@Nullable Plot pasture) {
        height = height(selection, totals);
        top = bottom - height;
        Long key = pasture == null ? null : ChunkPos.asLong(pasture.chunkX(), pasture.chunkZ());
        if (!Objects.equals(key, shownPasture)) typeOffset = 0;
        shownPasture = key;
    }

    boolean contains(double mouseX, double mouseY) {
        return height > 0 && Widgets.isOver(mouseX, mouseY, LEFT, top, WIDTH, height);
    }

    private int buttonLeft(int index) { return LEFT + PAD + index * (BUTTON_W + BUTTON_GAP); }

    private int buttonTop() { return top + height - PAD - BUTTON_H; }

    private static Component typeName(ResourceLocation type) {
        return BuiltInRegistries.ENTITY_TYPE.get(type).getDescription();
    }

    //region Drawing [绘制]
    void sprites(SpriteBatch sprites, MapSelection selection, FieldTotals totals, MapPlots.@Nullable Plot pasture,
                 MapTools.Land land, double mouseX, double mouseY) {
        if (height == 0) return;
        sprites.blit(PanelDrawing.POPUP, LEFT, top, WIDTH, height);
        sprites.blit(PanelDrawing.sprite(selection.hasFields() ? "tool_farm" : "tool_pasture"), LEFT + PAD, top + 5,
                16, 16);
        if (selection.hasFields()) {
            if (totals.fields() == 0) return;
            sprites.blit(PanelDrawing.BAR_BACKGROUND, LEFT + GROWTH_X, top + GROWTH_Y + 1, GROWTH_W, 6);
            int filled = (GROWTH_W - 2) * Math.clamp(totals.growth(), 0, 100) / 100;
            if (filled > 0) {
                sprites.blit(PanelDrawing.BAR_FILL, LEFT + GROWTH_X + 1, top + GROWTH_Y + 2, filled, 4,
                        PanelDrawing.ACCENT);
            }
            return;
        }
        MapPart.Section.Pen pen = pasture == null ? null : land.pen(pasture.index());
        if (pen == null) return;
        for (int i = 0; i < PEN_BUTTONS.length; i++) {
            PanelDrawing.Look look = "remove".equals(PEN_BUTTONS[i]) ? PanelDrawing.Look.DANGER
                    : PanelDrawing.Look.PLAIN;
            PanelDrawing.button(sprites, buttonLeft(i), buttonTop(), BUTTON_W, BUTTON_H, look, true, mouseX, mouseY);
        }
        PanelDrawing.toggle(sprites, LEFT + PAD, top + AUTO_Y, pen.autoGather(), true, mouseX, mouseY);
        List<ResourceLocation> types = land.animals();
        typeOffset = PanelDrawing.clampScroll(typeOffset, TYPE_ROWS, types.size());
        sprites.blit(PanelDrawing.INSET, LEFT + PAD, top + TYPES_Y, WIDTH - 2 * PAD, TYPE_ROWS * TYPE_ROW_H);
        for (int i = 0; i < TYPE_ROWS && typeOffset + i < types.size(); i++) {
            int rowTop = top + TYPES_Y + i * TYPE_ROW_H;
            PanelDrawing.toggle(sprites, LEFT + PAD + 2, rowTop + 1, pen.gathers(types.get(typeOffset + i)), true,
                    mouseX, mouseY);
        }
        if (types.size() > TYPE_ROWS) {
            PanelDrawing.scrollbar(sprites, LEFT + WIDTH - PAD - 6, top + TYPES_Y, TYPE_ROWS * TYPE_ROW_H,
                    typeOffset, TYPE_ROWS, types.size(), false);
        }
    }

    void texts(GuiGraphics graphics, Font font, MapSelection selection, FieldTotals totals,
               MapPlots.@Nullable Plot pasture, MapTools.Land land) {
        if (height == 0) return;
        if (selection.hasFields()) {
            fieldTexts(graphics, font, totals, land);
            return;
        }
        if (pasture == null) return;
        graphics.drawString(font, Component.translatable(EarthSpiritScreen.UI + "map.tool.pasture"), LEFT + TEXT_X,
                top + 5, PanelDrawing.ACCENT, false);
        graphics.drawString(font, Component.translatable(UI + "pasture_state", pasture.penned()), LEFT + TEXT_X,
                top + 15, PanelDrawing.LEGEND, false);
        for (int i = 0; i < PEN_BUTTONS.length; i++) {
            String key = switch (PEN_BUTTONS[i]) {
                case "gather" -> EarthSpiritScreen.UI + "gather";
                case "move" -> EarthSpiritScreen.UI + "map.tool.move";
                default -> EarthSpiritScreen.UI + "map.act.clear";
            };
            PanelDrawing.label(graphics, font, Component.translatable(key), buttonLeft(i), buttonTop(), BUTTON_W,
                    BUTTON_H, true);
        }
        MapPart.Section.Pen pen = land.pen(pasture.index());
        if (pen == null) return;
        graphics.drawString(font, Component.translatable(UI + "auto_gather"), LEFT + PAD + PanelDrawing.SWITCH_W + 4,
                top + AUTO_Y + 1, PanelDrawing.TEXT, false);
        graphics.drawString(font, Component.translatable(UI + "gather_types"), LEFT + PAD, top + TYPES_LABEL_Y,
                PanelDrawing.LEGEND, false);
        List<ResourceLocation> types = land.animals();
        if (types.isEmpty()) {
            graphics.drawString(font, Component.translatable(UI + "no_animals"), LEFT + PAD + 4, top + TYPES_Y + 2,
                    PanelDrawing.LEGEND, false);
            return;
        }
        for (int i = 0; i < TYPE_ROWS && typeOffset + i < types.size(); i++) {
            ResourceLocation type = types.get(typeOffset + i);
            graphics.drawString(font, typeName(type), LEFT + PAD + PanelDrawing.SWITCH_W + 6,
                    top + TYPES_Y + i * TYPE_ROW_H + 2, pen.gathers(type) ? PanelDrawing.TEXT : PanelDrawing.DEAD,
                    false);
        }
    }

    private void fieldTexts(GuiGraphics graphics, Font font, FieldTotals totals, MapTools.Land land) {
        Component title = totals.planned() == 0 ? Component.translatable(UI + "fields", totals.fields())
                : totals.fields() == 0 ? Component.translatable(UI + "plans", totals.planned())
                : Component.translatable(UI + "fields_plans", totals.fields(), totals.planned());
        graphics.drawString(font, title, LEFT + TEXT_X, top + 5, PanelDrawing.ACCENT, false);
        if (totals.fields() == 0) {
            graphics.drawString(font, Component.translatable(UI + "planned_state"), LEFT + TEXT_X, top + 15,
                    PanelDrawing.LEGEND, false);
            return;
        }
        graphics.drawString(font, Component.translatable(UI + "field_state", totals.planted(), totals.cells(),
                totals.mature()), LEFT + TEXT_X, top + 15, PanelDrawing.LEGEND, false);
        graphics.drawString(font, Component.translatable(UI + "growth"), LEFT + PAD, top + GROWTH_Y,
                PanelDrawing.LEGEND, false);
        PanelDrawing.right(graphics, font, Math.clamp(totals.growth(), 0, 100) + "%", LEFT + WIDTH - PAD,
                top + GROWTH_Y, PanelDrawing.TEXT);
        cost(graphics, font, Items.BONE_MEAL.getDescription(), BONE_Y, totals.growing(), land.boneMeal());
        cost(graphics, font, ModItems.ROTTEN_FROZEN_MUD.get().getDescription(), MUD_Y, totals.mudNeed(), land.mud());
        graphics.drawString(font, Component.translatable(UI + "kind_gu_plant"), LEFT + PAD, top + KIND_Y,
                PanelDrawing.LEGEND, false);
        PanelDrawing.right(graphics, font, totals.guPlant() + " / " + totals.fields(), LEFT + WIDTH - PAD,
                top + KIND_Y, PanelDrawing.TEXT);
    }

    private void cost(GuiGraphics graphics, Font font, Component label, int y, long need, long have) {
        graphics.drawString(font, label, LEFT + PAD, top + y, PanelDrawing.LEGEND, false);
        Component value = Component.translatable(UI + "need_have", PanelDrawing.count(need),
                PanelDrawing.count(have));
        PanelDrawing.right(graphics, font, value, LEFT + WIDTH - PAD, top + y,
                need > have ? PanelDrawing.WARN : PanelDrawing.TEXT);
    }

    boolean tip(MapPage page, GuiGraphics graphics, MapSelection selection, int mouseX, int mouseY) {
        if (height == 0 || selection.hasFields()) return false;
        if (page.tipAt(graphics, buttonLeft(0), buttonTop(), BUTTON_W, BUTTON_H, mouseX, mouseY,
                Component.translatable(EarthSpiritScreen.TIP + "gather", HerdRules.GATHER_LIMIT))) {
            return true;
        }
        return page.tipAt(graphics, LEFT + PAD, top + AUTO_Y, WIDTH - 2 * PAD, PanelDrawing.SWITCH_H, mouseX, mouseY,
                Component.translatable(TIP + "auto_gather", LandHostingJob.INTERVAL_TICKS / Ticks.SECOND,
                        HerdRules.GATHER_LIMIT));
    }
    //endregion

    //region Input [输入]
    /** 点在卡上：牧场的开关、种类与按钮生效；返回真即吃掉这次点击（点在卡上空白处也吃掉）。 */
    boolean click(MapTools.Land land, MapTools tools, MapSelection selection, MapPlots.@Nullable Plot pasture,
                  double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) return false;
        if (selection.hasFields() || pasture == null) return true;
        for (int i = 0; i < PEN_BUTTONS.length; i++) {
            if (!Widgets.isOver(mouseX, mouseY, buttonLeft(i), buttonTop(), BUTTON_W, BUTTON_H)) continue;
            switch (PEN_BUTTONS[i]) {
                case "gather" -> land.gather(pasture.index());
                case "move" -> {
                    selection.clear();
                    tools.startMove(pasture.index());
                }
                default -> {
                    land.removePasture(pasture.index());
                    selection.clear();
                }
            }
            return true;
        }
        MapPart.Section.Pen pen = land.pen(pasture.index());
        if (pen == null) return true;
        if (Widgets.isOver(mouseX, mouseY, LEFT + PAD, top + AUTO_Y, WIDTH - 2 * PAD, PanelDrawing.SWITCH_H)) {
            land.setAutoGather(pasture.index(), !pen.autoGather());
            return true;
        }
        List<ResourceLocation> types = land.animals();
        for (int i = 0; i < TYPE_ROWS && typeOffset + i < types.size(); i++) {
            int rowTop = top + TYPES_Y + i * TYPE_ROW_H;
            if (!Widgets.isOver(mouseX, mouseY, LEFT + PAD, rowTop, WIDTH - 2 * PAD - 6, TYPE_ROW_H)) continue;
            ResourceLocation type = types.get(typeOffset + i);
            land.setGatherType(pasture.index(), type, !pen.gathers(type));
            return true;
        }
        return true;
    }

    boolean scroll(MapTools.Land land, MapSelection selection, double mouseX, double mouseY, double delta) {
        if (!contains(mouseX, mouseY)) return false;
        if (!selection.hasFields()
                && Widgets.isOver(mouseX, mouseY, LEFT + PAD, top + TYPES_Y, WIDTH - 2 * PAD, TYPE_ROWS * TYPE_ROW_H)) {
            typeOffset = PanelDrawing.clampScroll(typeOffset - (int) Math.signum(delta), TYPE_ROWS,
                    land.animals().size());
        }
        return true;
    }
    //endregion
}
