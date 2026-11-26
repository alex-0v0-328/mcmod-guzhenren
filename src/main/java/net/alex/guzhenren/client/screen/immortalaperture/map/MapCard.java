package net.alex.guzhenren.client.screen.immortalaperture.map;

import java.util.List;
import java.util.Locale;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.herd.PastureWork;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 地图上的对象卡片：不拿工具时点田、规划田或牧场，在光标旁弹一张小卡写它的状态，带对应按钮（田：拆除；规划田：取消；
 * 牧场：收拢、迁移、拆除）。
 * <ul>
 *   <li>卡片不压暗地图、不独占输入，点卡片外任意处即关；每帧按占地列表重找一次，快照里没了就自动关。</li>
 *   <li>「收拢」冷却中置灰，悬停写剩余秒数；可用时写每次上限 {@link PastureWork#GATHER_LIMIT} 头与冷却。</li>
 *   <li>卡片贴光标右下，越出地图区时翻到左边或上边。</li>
 * </ul>
 */

final class MapCard {

    private static final int WIDTH = 116;
    private static final int HEIGHT = 52;
    private static final int BUTTON_W = 32;
    private static final int BUTTON_GAP = 4;
    private static final String UI = EarthSpiritScreen.UI + "map.card.";
    private static final String GATHER = "gather";
    private MapPlots.@Nullable Plot plot;
    private int left;
    private int top;

    boolean isOpen() { return plot != null; }

    MapPlots.@Nullable Plot plot() { return plot; }

    void open(MapPlots.Plot plot, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        this.plot = plot;
        left = mouseX + 8 + WIDTH > screenWidth ? mouseX - 8 - WIDTH : mouseX + 8;
        top = mouseY + 8 + HEIGHT > screenHeight ? mouseY - 8 - HEIGHT : mouseY + 8;
        top = Math.max(EarthSpiritScreen.BAR_H + 2, top);
    }

    void close() { plot = null; }

    boolean contains(double mouseX, double mouseY) {
        return plot != null && Widgets.isOver(mouseX, mouseY, left, top, WIDTH, HEIGHT);
    }

    void refresh(List<MapPlots.Plot> plots) {
        if (plot == null) return;
        MapPlots.Plot found = MapPlots.at(plots, plot.chunkX(), plot.chunkZ());
        plot = found != null && found.kind() == plot.kind() ? found : null;
    }

    private List<String> buttons() {
        if (plot == null) return List.of();
        return switch (plot.kind()) {
            case FIELD -> List.of("remove");
            case PLANNED -> List.of("unplan");
            case PASTURE -> List.of(GATHER, "move", "remove");
        };
    }

    private int buttonLeft(int index) { return left + 6 + index * (BUTTON_W + BUTTON_GAP); }

    private int buttonTop() { return top + HEIGHT - 20; }

    //region Drawing [绘制]
    private static boolean live(String button, MapTools.Land land) {
        return !GATHER.equals(button) || land.gatherSeconds() <= 0;
    }

    private static Component label(String button) {
        return Component.translatable(GATHER.equals(button) ? EarthSpiritScreen.UI + GATHER : UI + button);
    }

    void sprites(SpriteBatch sprites, MapTools.Land land, double mouseX, double mouseY) {
        if (plot == null) return;
        sprites.blit(PanelDrawing.POPUP, left, top, WIDTH, HEIGHT);
        List<String> buttons = buttons();
        for (int i = 0; i < buttons.size(); i++) {
            boolean live = live(buttons.get(i), land);
            boolean hover = live
                    && Widgets.isOver(mouseX, mouseY, buttonLeft(i), buttonTop(), BUTTON_W, MapControls.HEIGHT);
            PanelDrawing.button(sprites, buttonLeft(i), buttonTop(), BUTTON_W, MapControls.HEIGHT, live, hover, false,
                    "remove".equals(buttons.get(i)));
        }
    }

    boolean tip(MapPage page, GuiGraphics graphics, MapTools.Land land, int mouseX, int mouseY) {
        List<String> buttons = buttons();
        int gather = buttons.indexOf(GATHER);
        if (gather < 0) return false;
        Component text = land.gatherSeconds() > 0
                ? Component.translatable(EarthSpiritScreen.TIP + "gather_wait", land.gatherSeconds())
                : Component.translatable(EarthSpiritScreen.TIP + GATHER, PastureWork.GATHER_LIMIT,
                        PastureWork.GATHER_COOLDOWN_TICKS / Ticks.SECOND);
        return page.tipAt(graphics, buttonLeft(gather), buttonTop(), BUTTON_W, MapControls.HEIGHT, mouseX, mouseY,
                text);
    }

    void texts(GuiGraphics graphics, Font font, MapTools.Land land) {
        MapPlots.Plot shown = plot;
        if (shown == null) return;
        String kind = shown.kind().name().toLowerCase(Locale.ROOT);
        graphics.drawString(font, Component.translatable(UI + kind), left + 6, top + 6, PanelDrawing.ACCENT, false);
        Component state = switch (shown.kind()) {
            case FIELD -> Component.translatable(UI + "field_state", shown.mature(), shown.growing());
            case PLANNED -> Component.translatable(UI + "planned_state");
            case PASTURE -> Component.translatable(UI + "pasture_state", shown.penned());
        };
        graphics.drawString(font, state, left + 6, top + 18, PanelDrawing.TEXT, false);
        List<String> buttons = buttons();
        for (int i = 0; i < buttons.size(); i++) {
            PanelDrawing.label(graphics, font, label(buttons.get(i)), buttonLeft(i), buttonTop(), BUTTON_W,
                    MapControls.HEIGHT, live(buttons.get(i), land));
        }
    }
    //endregion

    boolean click(MapTools.Land land, MapTools tools, double mouseX, double mouseY) {
        MapPlots.Plot shown = plot;
        if (shown == null || !contains(mouseX, mouseY)) return false;
        List<String> buttons = buttons();
        for (int i = 0; i < buttons.size(); i++) {
            if (!Widgets.isOver(mouseX, mouseY, buttonLeft(i), buttonTop(), BUTTON_W, MapControls.HEIGHT)) continue;
            if (!live(buttons.get(i), land)) return true;
            switch (buttons.get(i)) {
                case GATHER -> land.gather(shown.index());
                case "move" -> tools.startMove(shown.index());
                default -> MapTools.remove(land, shown);
            }
            close();
            return true;
        }
        return true;
    }
}
