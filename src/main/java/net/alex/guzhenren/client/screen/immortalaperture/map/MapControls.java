package net.alex.guzhenren.client.screen.immortalaperture.map;

import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页左下角的视角按钮条：左转、右转、地形 / 等高线切换、放大、变小、山心、割弃；只管排版、命中与绘制，按下后做什么
 * 由 {@link MapPage} 决定。
 * <ul>
 *   <li>按钮高 {@value #HEIGHT}、间距 {@value #GAP}，从窗口左下角往右排；视角按钮写的是「点了会切到哪一种」。</li>
 *   <li>割弃用危险色按钮，与视角按钮之间多空 {@value #SPLIT}。</li>
 * </ul>
 */

final class MapControls {

    static final int HEIGHT = 16;
    private static final int GAP = 2;
    private static final int SPLIT = 8;
    private static final int MARGIN = 6;
    private static final String UI = EarthSpiritScreen.UI + "map.";
    private static final String TIP = EarthSpiritScreen.TIP + "map_";

    enum Control {
        ROTATE_LEFT(30, "rotate_left", "rotate"),
        ROTATE_RIGHT(30, "rotate_right", "rotate"),
        VIEW(38, "", "view"),
        ZOOM_OUT(16, "", "zoom"),
        ZOOM_IN(16, "", "zoom"),
        HOME(38, "home", "home"),
        CEDE(30, "", "cede");

        final int width;
        final String label;
        final String tip;

        Control(int width, String label, String tip) {
            this.width = width;
            this.label = label;
            this.tip = tip;
        }
    }

    private final int[] lefts = new int[Control.values().length];
    private int top;

    void layout(int height) {
        top = height - MARGIN - HEIGHT;
        int left = MARGIN;
        for (Control control : Control.values()) {
            if (control == Control.CEDE) left += SPLIT;
            lefts[control.ordinal()] = left;
            left += control.width + GAP;
        }
    }

    int top() { return top; }

    @Nullable Control at(double mouseX, double mouseY) {
        for (Control control : Control.values()) {
            if (Widgets.isOver(mouseX, mouseY, lefts[control.ordinal()], top, control.width, HEIGHT)) return control;
        }
        return null;
    }

    void sprites(SpriteBatch sprites, double mouseX, double mouseY) {
        for (Control control : Control.values()) {
            int left = lefts[control.ordinal()];
            boolean hover = Widgets.isOver(mouseX, mouseY, left, top, control.width, HEIGHT);
            PanelDrawing.button(sprites, left, top, control.width, HEIGHT, true, hover, false, control == Control.CEDE);
        }
    }

    void texts(GuiGraphics graphics, Font font, MapCamera camera) {
        for (Control control : Control.values()) {
            PanelDrawing.label(graphics, font, label(control, camera), lefts[control.ordinal()], top, control.width,
                    HEIGHT, true);
        }
    }

    boolean tip(MapPage page, GuiGraphics graphics, int mouseX, int mouseY) {
        Control control = at(mouseX, mouseY);
        if (control == null) return false;
        return page.tipAt(graphics, lefts[control.ordinal()], top, control.width, HEIGHT, mouseX, mouseY,
                Component.translatable(TIP + control.tip));
    }

    static void turn(Control control, MapCamera camera) {
        switch (control) {
            case ROTATE_LEFT -> camera.rotate(-1);
            case ROTATE_RIGHT -> camera.rotate(1);
            case VIEW -> camera.setPlan(!camera.plan());
            case ZOOM_OUT -> camera.zoom(-1, camera.width() / 2.0, camera.height() / 2.0);
            case ZOOM_IN -> camera.zoom(1, camera.width() / 2.0, camera.height() / 2.0);
            default -> {}
        }
    }

    private static Component label(Control control, MapCamera camera) {
        return switch (control) {
            case VIEW -> Component.translatable(UI + (camera.plan() ? "iso" : "plan"));
            case ZOOM_OUT -> Component.literal("-");
            case ZOOM_IN -> Component.literal("+");
            case CEDE -> Component.translatable(EarthSpiritScreen.UI + "cede");
            default -> Component.translatable(UI + control.label);
        };
    }
}
