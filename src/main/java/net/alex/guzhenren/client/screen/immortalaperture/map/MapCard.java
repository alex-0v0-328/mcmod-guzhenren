package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.List;
import java.util.Locale;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmRules;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FarmState;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldKind;
import net.alex.guzhenren.gameplay.immortalaperture.farm.FieldPlot;
import net.alex.guzhenren.gameplay.immortalaperture.herd.HerdRules;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.MapPart;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandHostingJob;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 地图上的对象卡片：不拿工具时点田、规划田或牧场，在光标旁弹一张卡写它的状态、设置与按钮（田：作物、田型、拆除；规划田：
 * 取消；牧场：自动收拢、收拢种类、收拢、迁移、拆除）。
 * <ul>
 *   <li>卡片不压暗地图、不独占输入，点卡片外任意处即关；每帧按占地列表重找一次，快照里没了就自动关。卡高随种类定。</li>
 *   <li>田：状态行写「已种 已种株 / 耕地格 · 成熟 熟株」，下面生长进度条（全田作物龄之和 ÷ 满龄之和）；作物一排图标键
 *       （自动 + 各作物的种子图标，右下角写库藏种子数，当前作物用选中样式），点哪个这块田就补种哪个；「套用到全部田」把
 *       所有田的作物改成这块田的。缺种不另行提示，看已种占比（Alex 定）。</li>
 *   <li>田型按钮两步：第一次点变「确认」（危险色），再点才发；点卡片别处或换卡即撤销。两个方向都不返还腐泥冻土，所以都要
 *       确认。普通田在按钮行右边写「腐泥冻土 有 / 要」，不够时字标警示色、田型按钮失效。</li>
 *   <li>收拢种类列表每行一个开关加种类名，一屏 {@value #TYPE_ROWS} 行，多了滚轮翻；关掉 = 这个牧场不收这种。</li>
 *   <li>卡片贴光标右下，越出地图区时翻到左边或上边。</li>
 * </ul>
 */

final class MapCard {

    private static final int WIDTH = 150;
    private static final int PAD = 6;
    private static final int TEXT_X = 26;
    private static final int BUTTON_W = 42;
    private static final int BUTTON_H = 16;
    private static final int BUTTON_GAP = 4;
    private static final int PICK_X = 36;
    private static final int PICK_W = 60;
    private static final int GROWTH_Y = 28;
    private static final int GROWTH_X = 36;
    private static final int GROWTH_W = 80;
    private static final int CROP_Y = 40;
    private static final int CROP_SIZE = 20;
    private static final int CROP_PITCH = 22;
    private static final int KIND_Y = 66;
    private static final int APPLY_W = 70;
    private static final int AUTO_Y = 32;
    private static final int TYPES_LABEL_Y = 46;
    private static final int TYPES_Y = 56;
    private static final int TYPE_ROW_H = 12;
    private static final int TYPE_ROWS = 4;
    private static final int SHORT_HEIGHT = 52;
    private static final int FIELD_HEIGHT = KIND_Y + BUTTON_H + 4 + BUTTON_H + PAD;
    private static final int PASTURE_HEIGHT = TYPES_Y + TYPE_ROWS * TYPE_ROW_H + 4 + BUTTON_H + PAD;
    private static final String UI = EarthSpiritScreen.UI + "map.card.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_card_";
    private static final String GATHER = "gather";
    private static final String APPLY = "apply_all";
    private MapPlots.@Nullable Plot plot;
    private int left;
    private int top;
    private boolean confirming;
    private int typeOffset;

    boolean isOpen() { return plot != null; }

    MapPlots.@Nullable Plot plot() { return plot; }

    void open(MapPlots.Plot plot, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        this.plot = plot;
        confirming = false;
        typeOffset = 0;
        int height = height();
        left = mouseX + 8 + WIDTH > screenWidth ? mouseX - 8 - WIDTH : mouseX + 8;
        top = mouseY + 8 + height > screenHeight ? mouseY - 8 - height : mouseY + 8;
        top = Math.max(EarthSpiritScreen.BAR_H + 2, top);
    }

    void close() { plot = null; }

    private int height() {
        if (plot == null) return SHORT_HEIGHT;
        return switch (plot.kind()) {
            case FIELD -> FIELD_HEIGHT;
            case PLANNED -> SHORT_HEIGHT;
            case PASTURE -> PASTURE_HEIGHT;
        };
    }

    boolean contains(double mouseX, double mouseY) {
        return plot != null && Widgets.isOver(mouseX, mouseY, left, top, WIDTH, height());
    }

    void refresh(List<MapPlots.Plot> plots) {
        if (plot == null) return;
        MapPlots.Plot found = MapPlots.at(plots, plot.chunkX(), plot.chunkZ());
        plot = found != null && found.kind() == plot.kind() ? found : null;
    }

    private List<String> buttons() {
        if (plot == null) return List.of();
        return switch (plot.kind()) {
            case FIELD -> List.of(APPLY, "remove");
            case PLANNED -> List.of("unplan");
            case PASTURE -> List.of(GATHER, "move", "remove");
        };
    }

    private static int buttonWidth(String button) { return APPLY.equals(button) ? APPLY_W : BUTTON_W; }

    private int buttonLeft(int index) {
        List<String> buttons = buttons();
        int x = left + PAD;
        for (int i = 0; i < index && i < buttons.size(); i++) x += buttonWidth(buttons.get(i)) + BUTTON_GAP;
        return x;
    }

    private int buttonWidthAt(int index) {
        List<String> buttons = buttons();
        return index < buttons.size() ? buttonWidth(buttons.get(index)) : BUTTON_W;
    }

    private int cropLeft(int slot) { return left + PAD + slot * CROP_PITCH; }

    /** 作物图标键的第 {@code slot} 个对应的作物下标：0 = 自动，其余依次为 {@link FarmRules#crops()}。 */
    private static int cropOf(int slot) { return slot == 0 ? FarmState.AUTO_CROP : slot - 1; }

    private static int cropSlots() { return FarmRules.crops().size() + 1; }

    private static String shortCount(long value) {
        if (value < 1_000L) return Long.toString(value);
        if (value < 1_000_000L) return value / 1_000L + "k";
        return value / 1_000_000L + "m";
    }

    private int buttonTop() { return top + height() - PAD - BUTTON_H; }

    private static int cost() { return FieldPlot.of(0, 0, 0).farmland(); }

    private static Component cropName(int crop) {
        return crop == FarmState.AUTO_CROP ? Component.translatable(EarthSpiritScreen.UI + "crop_auto")
                : FarmRules.crops().get(crop).getName();
    }

    private static Component typeName(ResourceLocation type) {
        return BuiltInRegistries.ENTITY_TYPE.get(type).getDescription();
    }

    private static boolean kindLive(MapPart.Section.Field field, MapTools.Land land) {
        return FieldKind.byId(field.kind()) == FieldKind.GU_PLANT || land.mud() >= cost();
    }

    //region Drawing [绘制]
    private static Component label(String button) {
        return Component.translatable(GATHER.equals(button) ? EarthSpiritScreen.UI + GATHER : UI + button);
    }

    void sprites(SpriteBatch sprites, MapTools.Land land, double mouseX, double mouseY) {
        MapPlots.Plot shown = plot;
        if (shown == null) return;
        sprites.blit(PanelDrawing.POPUP, left, top, WIDTH, height());
        sprites.blit(PanelDrawing.sprite(shown.kind() == MapPlots.Kind.PASTURE ? "tool_pasture" : "tool_farm"),
                left + PAD, top + 5, 16, 16);
        List<String> buttons = buttons();
        for (int i = 0; i < buttons.size(); i++) {
            PanelDrawing.Look look = "remove".equals(buttons.get(i)) ? PanelDrawing.Look.DANGER
                    : PanelDrawing.Look.PLAIN;
            PanelDrawing.button(sprites, buttonLeft(i), buttonTop(), buttonWidthAt(i), BUTTON_H, look, true, mouseX,
                    mouseY);
        }
        if (shown.kind() == MapPlots.Kind.FIELD) {
            MapPart.Section.Field field = land.field(shown.index());
            if (field == null) return;
            sprites.blit(PanelDrawing.BAR_BACKGROUND, left + GROWTH_X, top + GROWTH_Y + 1, GROWTH_W, 6);
            int filled = (GROWTH_W - 2) * Math.clamp(field.growth(), 0, 100) / 100;
            if (filled > 0) {
                sprites.blit(PanelDrawing.BAR_FILL, left + GROWTH_X + 1, top + GROWTH_Y + 2, filled, 4,
                        PanelDrawing.ACCENT);
            }
            for (int slot = 0; slot < cropSlots(); slot++) {
                PanelDrawing.Look look = cropOf(slot) == field.crop() ? PanelDrawing.Look.SELECTED
                        : PanelDrawing.Look.PLAIN;
                PanelDrawing.button(sprites, cropLeft(slot), top + CROP_Y, CROP_SIZE, CROP_SIZE, look, true, mouseX,
                        mouseY);
            }
            PanelDrawing.Look kind = confirming ? PanelDrawing.Look.DANGER : PanelDrawing.Look.PLAIN;
            PanelDrawing.button(sprites, left + PICK_X, top + KIND_Y, PICK_W, BUTTON_H, kind, kindLive(field, land),
                    mouseX, mouseY);
        } else if (shown.kind() == MapPlots.Kind.PASTURE) {
            MapPart.Section.Pen pen = land.pen(shown.index());
            if (pen == null) return;
            PanelDrawing.toggle(sprites, left + PAD, top + AUTO_Y, pen.autoGather(), true, mouseX, mouseY);
            List<ResourceLocation> types = land.animals();
            typeOffset = PanelDrawing.clampScroll(typeOffset, TYPE_ROWS, types.size());
            sprites.blit(PanelDrawing.INSET, left + PAD, top + TYPES_Y, WIDTH - 2 * PAD, TYPE_ROWS * TYPE_ROW_H);
            for (int i = 0; i < TYPE_ROWS && typeOffset + i < types.size(); i++) {
                int rowTop = top + TYPES_Y + i * TYPE_ROW_H;
                PanelDrawing.toggle(sprites, left + PAD + 2, rowTop + 1, pen.gathers(types.get(typeOffset + i)), true,
                        mouseX, mouseY);
            }
            if (types.size() > TYPE_ROWS) {
                PanelDrawing.scrollbar(sprites, left + WIDTH - PAD - 6, top + TYPES_Y, TYPE_ROWS * TYPE_ROW_H,
                        typeOffset, TYPE_ROWS, types.size(), false);
            }
        }
    }

    boolean tip(MapPage page, GuiGraphics graphics, MapTools.Land land, int mouseX, int mouseY) {
        MapPlots.Plot shown = plot;
        if (shown == null) return false;
        int gather = buttons().indexOf(GATHER);
        if (gather >= 0 && page.tipAt(graphics, buttonLeft(gather), buttonTop(), BUTTON_W, BUTTON_H, mouseX,
                mouseY, Component.translatable(EarthSpiritScreen.TIP + GATHER, HerdRules.GATHER_LIMIT))) {
            return true;
        }
        if (shown.kind() == MapPlots.Kind.FIELD) {
            MapPart.Section.Field field = land.field(shown.index());
            if (field == null) return false;
            for (int slot = 0; slot < cropSlots(); slot++) {
                int crop = cropOf(slot);
                Component text = crop == FarmState.AUTO_CROP ? Component.translatable(TIP + "crop")
                        : Component.translatable(TIP + "crop_seed", cropName(crop), land.seeds(crop));
                if (page.tipAt(graphics, cropLeft(slot), top + CROP_Y, CROP_SIZE, CROP_SIZE, mouseX, mouseY, text)) {
                    return true;
                }
            }
            int apply = buttons().indexOf(APPLY);
            if (apply >= 0 && page.tipAt(graphics, buttonLeft(apply), buttonTop(), APPLY_W, BUTTON_H, mouseX, mouseY,
                    Component.translatable(TIP + "apply_all", cropName(field.crop())))) {
                return true;
            }
            Component kindTip = FieldKind.byId(field.kind()) == FieldKind.GU_PLANT
                    ? Component.translatable(TIP + "kind_plain")
                    : Component.translatable(TIP + "kind_gu_plant", cost());
            return page.tipAt(graphics, left + PICK_X, top + KIND_Y, PICK_W, BUTTON_H, mouseX, mouseY,
                    kindTip);
        }
        if (shown.kind() == MapPlots.Kind.PASTURE) {
            return page.tipAt(graphics, left + PAD, top + AUTO_Y, WIDTH - 2 * PAD, PanelDrawing.SWITCH_H, mouseX,
                    mouseY, Component.translatable(TIP + "auto_gather",
                            LandHostingJob.INTERVAL_TICKS / Ticks.SECOND, HerdRules.GATHER_LIMIT));
        }
        return false;
    }

    void texts(GuiGraphics graphics, Font font, MapTools.Land land) {
        MapPlots.Plot shown = plot;
        if (shown == null) return;
        String kind = shown.kind().name().toLowerCase(Locale.ROOT);
        graphics.drawString(font, Component.translatable(UI + kind), left + TEXT_X, top + 5, PanelDrawing.ACCENT,
                false);
        Component state = switch (shown.kind()) {
            case FIELD -> Component.translatable(UI + "field_state", shown.mature() + shown.growing(), cost(),
                    shown.mature());
            case PLANNED -> Component.translatable(UI + "planned_state");
            case PASTURE -> Component.translatable(UI + "pasture_state", shown.penned());
        };
        graphics.drawString(font, state, left + TEXT_X, top + 15, PanelDrawing.LEGEND, false);
        List<String> buttons = buttons();
        for (int i = 0; i < buttons.size(); i++) {
            PanelDrawing.label(graphics, font, label(buttons.get(i)), buttonLeft(i), buttonTop(), buttonWidthAt(i),
                    BUTTON_H, true);
        }
        if (shown.kind() == MapPlots.Kind.FIELD) fieldTexts(graphics, font, land, shown);
        if (shown.kind() == MapPlots.Kind.PASTURE) pastureTexts(graphics, font, land, shown);
    }

    private void fieldTexts(GuiGraphics graphics, Font font, MapTools.Land land, MapPlots.Plot shown) {
        MapPart.Section.Field field = land.field(shown.index());
        if (field == null) return;
        graphics.drawString(font, Component.translatable(UI + "growth"), left + PAD, top + GROWTH_Y,
                PanelDrawing.LEGEND, false);
        PanelDrawing.right(graphics, font, Math.clamp(field.growth(), 0, 100) + "%", left + WIDTH - PAD,
                top + GROWTH_Y, PanelDrawing.TEXT);
        for (int slot = 0; slot < cropSlots(); slot++) {
            int crop = cropOf(slot);
            int x = cropLeft(slot);
            int y = top + CROP_Y;
            if (crop == FarmState.AUTO_CROP) {
                PanelDrawing.label(graphics, font, Component.translatable(EarthSpiritScreen.UI + "crop_auto"), x, y,
                        CROP_SIZE, CROP_SIZE, field.crop() == crop ? PanelDrawing.Look.SELECTED
                                : PanelDrawing.Look.PLAIN, true);
                continue;
            }
            graphics.renderItem(new ItemStack(FarmRules.crops().get(crop).asItem()), x + 2, y + 2);
            String count = shortCount(land.seeds(crop));
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            graphics.drawString(font, count, x + CROP_SIZE - 1 - font.width(count), y + CROP_SIZE - 9,
                    land.seeds(crop) > 0 ? PanelDrawing.TEXT : PanelDrawing.DEAD, true);
            graphics.pose().popPose();
        }
        graphics.drawString(font, Component.translatable(UI + "kind"), left + PAD, top + KIND_Y + 4,
                PanelDrawing.LEGEND, false);
        FieldKind kind = FieldKind.byId(field.kind());
        Component kindLabel = confirming ? Component.translatable(UI + "confirm")
                : Component.translatable(UI + "kind_" + kind.name().toLowerCase(Locale.ROOT));
        PanelDrawing.label(graphics, font, kindLabel, left + PICK_X, top + KIND_Y, PICK_W, BUTTON_H,
                kindLive(field, land));
        if (kind == FieldKind.PLAIN) {
            Component mud = Component.translatable(UI + "mud", PanelDrawing.count(land.mud()), cost());
            PanelDrawing.right(graphics, font, mud, left + WIDTH - PAD, top + KIND_Y + 4,
                    land.mud() >= cost() ? PanelDrawing.TEXT : PanelDrawing.WARN);
        }
    }

    private void pastureTexts(GuiGraphics graphics, Font font, MapTools.Land land, MapPlots.Plot shown) {
        MapPart.Section.Pen pen = land.pen(shown.index());
        if (pen == null) return;
        graphics.drawString(font, Component.translatable(UI + "auto_gather"), left + PAD + PanelDrawing.SWITCH_W + 4,
                top + AUTO_Y + 1, PanelDrawing.TEXT, false);
        graphics.drawString(font, Component.translatable(UI + "gather_types"), left + PAD, top + TYPES_LABEL_Y,
                PanelDrawing.LEGEND, false);
        List<ResourceLocation> types = land.animals();
        if (types.isEmpty()) {
            graphics.drawString(font, Component.translatable(UI + "no_animals"), left + PAD + 4, top + TYPES_Y + 2,
                    PanelDrawing.LEGEND, false);
            return;
        }
        for (int i = 0; i < TYPE_ROWS && typeOffset + i < types.size(); i++) {
            ResourceLocation type = types.get(typeOffset + i);
            graphics.drawString(font, typeName(type), left + PAD + PanelDrawing.SWITCH_W + 6,
                    top + TYPES_Y + i * TYPE_ROW_H + 2, pen.gathers(type) ? PanelDrawing.TEXT : PanelDrawing.DEAD,
                    false);
        }
    }
    //endregion

    //region Input [输入]
    boolean click(MapTools.Land land, MapTools tools, double mouseX, double mouseY) {
        MapPlots.Plot shown = plot;
        if (shown == null || !contains(mouseX, mouseY)) return false;
        List<String> buttons = buttons();
        for (int i = 0; i < buttons.size(); i++) {
            if (!Widgets.isOver(mouseX, mouseY, buttonLeft(i), buttonTop(), buttonWidthAt(i), BUTTON_H)) continue;
            switch (buttons.get(i)) {
                case GATHER -> land.gather(shown.index());
                case APPLY -> {
                    MapPart.Section.Field field = land.field(shown.index());
                    if (field != null) land.setAllCrops(field.crop());
                    return true;
                }
                case "move" -> tools.startMove(shown.index());
                default -> MapTools.remove(land, shown);
            }
            close();
            return true;
        }
        boolean wasConfirming = confirming;
        confirming = false;
        if (shown.kind() == MapPlots.Kind.FIELD) clickField(land, shown, mouseX, mouseY, wasConfirming);
        if (shown.kind() == MapPlots.Kind.PASTURE) clickPasture(land, shown, mouseX, mouseY);
        return true;
    }

    private void clickField(MapTools.Land land, MapPlots.Plot shown, double mouseX, double mouseY,
                            boolean wasConfirming) {
        MapPart.Section.Field field = land.field(shown.index());
        if (field == null) return;
        for (int slot = 0; slot < cropSlots(); slot++) {
            if (!Widgets.isOver(mouseX, mouseY, cropLeft(slot), top + CROP_Y, CROP_SIZE, CROP_SIZE)) continue;
            if (cropOf(slot) != field.crop()) land.setFieldCrop(shown.chunkX(), shown.chunkZ(), cropOf(slot));
            return;
        }
        if (Widgets.isOver(mouseX, mouseY, left + PICK_X, top + KIND_Y, PICK_W, BUTTON_H)
                && kindLive(field, land)) {
            if (!wasConfirming) {
                confirming = true;
                return;
            }
            FieldKind next = FieldKind.byId(field.kind()) == FieldKind.GU_PLANT ? FieldKind.PLAIN : FieldKind.GU_PLANT;
            land.setFieldKind(shown.chunkX(), shown.chunkZ(), next.ordinal());
        }
    }

    private void clickPasture(MapTools.Land land, MapPlots.Plot shown, double mouseX, double mouseY) {
        MapPart.Section.Pen pen = land.pen(shown.index());
        if (pen == null) return;
        if (Widgets.isOver(mouseX, mouseY, left + PAD, top + AUTO_Y, WIDTH - 2 * PAD, PanelDrawing.SWITCH_H)) {
            land.setAutoGather(shown.index(), !pen.autoGather());
            return;
        }
        List<ResourceLocation> types = land.animals();
        for (int i = 0; i < TYPE_ROWS && typeOffset + i < types.size(); i++) {
            int rowTop = top + TYPES_Y + i * TYPE_ROW_H;
            if (!Widgets.isOver(mouseX, mouseY, left + PAD, rowTop, WIDTH - 2 * PAD - 6, TYPE_ROW_H)) continue;
            ResourceLocation type = types.get(typeOffset + i);
            land.setGatherType(shown.index(), type, !pen.gathers(type));
            return;
        }
    }

    void scroll(MapTools.Land land, double mouseX, double mouseY, double delta) {
        if (plot == null || plot.kind() != MapPlots.Kind.PASTURE
                || !Widgets.isOver(mouseX, mouseY, left + PAD, top + TYPES_Y, WIDTH - 2 * PAD, TYPE_ROWS * TYPE_ROW_H)) {
            return;
        }
        typeOffset = PanelDrawing.clampScroll(typeOffset - (int) Math.signum(delta), TYPE_ROWS, land.animals().size());
    }
    //endregion
}
