package net.alex.guzhenren.client.screen.immortalaperture.map;

import com.mojang.math.Axis;
import java.util.Locale;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapCamera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 地图页右上角的视角簇：等高线开关（开 = 选中键，关 = 幽灵键）、回山心（幽灵键）两枚 {@value #KEY} 方键与一枚罗盘，下面
 * 一排键帽写快捷键（V、H、Q、E）；只管排版、命中与绘制，按下后做什么由 {@link MapPage} 决定。
 * <ul>
 *   <li>Alex 定：视角按钮只留这两枚，转视角靠 Q / E（R 备用）、平移靠 WASD 与拖动、缩放只靠滚轮。</li>
 *   <li>罗盘盘面静止，指针按 90° × 旋转数顺时针转（金尖 = 北），点罗盘顺时针转一档。</li>
 * </ul>
 */

final class MapControls {

    private static final int KEY = 22;
    private static final int COMPASS = 28;
    private static final int TOP = EarthSpiritScreen.BAR_H + 5;
    private static final int KEYCAP_Y = TOP + COMPASS + 2;
    private static final String TIP = EarthSpiritScreen.TIP + "map_";
    private static final ResourceLocation CONTOUR_ICON = PanelDrawing.sprite("view_contour");
    private static final ResourceLocation HOME_ICON = PanelDrawing.sprite("view_home");
    private static final ResourceLocation DIAL = PanelDrawing.sprite("compass");
    private static final ResourceLocation NEEDLE = PanelDrawing.sprite("compass_needle");

    enum Control { CONTOUR, HOME, COMPASS }

    private int compassX;
    private int homeX;
    private int contourX;

    void layout(int width) {
        compassX = width - 6 - COMPASS;
        homeX = compassX - 4 - KEY;
        contourX = homeX - 3 - KEY;
    }

    int bottom() { return KEYCAP_Y + PanelDrawing.KEYCAP_H; }

    private int left(Control control) {
        return switch (control) {
            case CONTOUR -> contourX;
            case HOME -> homeX;
            case COMPASS -> compassX;
        };
    }

    private static int top(Control control) { return control == Control.COMPASS ? TOP : TOP + 3; }

    private static int size(Control control) { return control == Control.COMPASS ? COMPASS : KEY; }

    @Nullable Control at(double mouseX, double mouseY) {
        for (Control control : Control.values()) {
            if (Widgets.isOver(mouseX, mouseY, left(control), top(control), size(control), size(control))) {
                return control;
            }
        }
        return null;
    }

    boolean contains(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, contourX, TOP, compassX + COMPASS - contourX, bottom() - TOP);
    }

    void sprites(SpriteBatch sprites, MapCamera camera, double mouseX, double mouseY) {
        PanelDrawing.Look contour = camera.plan() ? PanelDrawing.Look.SELECTED : PanelDrawing.Look.GHOST;
        PanelDrawing.button(sprites, contourX, top(Control.CONTOUR), KEY, KEY, contour, true, mouseX, mouseY);
        PanelDrawing.button(sprites, homeX, top(Control.HOME), KEY, KEY, PanelDrawing.Look.GHOST, true, mouseX, mouseY);
        int sunk = camera.plan() ? 1 : 0;
        sprites.blit(CONTOUR_ICON, contourX + 3, top(Control.CONTOUR) + 3 + sunk, 16, 16);
        sprites.blit(HOME_ICON, homeX + 3, top(Control.HOME) + 3, 16, 16);
        sprites.blit(DIAL, compassX, TOP, COMPASS, COMPASS);
        keycap(sprites, contourX, KEY);
        keycap(sprites, homeX, KEY);
        PanelDrawing.keycap(sprites, compassX + 1, KEYCAP_Y, 12);
        PanelDrawing.keycap(sprites, compassX + 15, KEYCAP_Y, 12);
    }

    private static void keycap(SpriteBatch sprites, int left, int width) {
        PanelDrawing.keycap(sprites, left + (width - 12) / 2, KEYCAP_Y, 12);
    }

    void needle(GuiGraphics graphics, MapCamera camera) {
        graphics.pose().pushPose();
        graphics.pose().translate(compassX + COMPASS / 2.0F, TOP + COMPASS / 2.0F, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(90.0F * camera.rotation()));
        graphics.blitSprite(NEEDLE, -COMPASS / 2, -COMPASS / 2, COMPASS, COMPASS);
        graphics.pose().popPose();
    }

    void texts(GuiGraphics graphics, Font font) {
        keycapText(graphics, font, "V", contourX + (KEY - 12) / 2);
        keycapText(graphics, font, "H", homeX + (KEY - 12) / 2);
        keycapText(graphics, font, "Q", compassX + 1);
        keycapText(graphics, font, "E", compassX + 15);
    }

    private static void keycapText(GuiGraphics graphics, Font font, String key, int left) {
        PanelDrawing.keycapText(graphics, font, Component.literal(key), left, KEYCAP_Y, 12);
    }

    boolean tip(MapPage page, GuiGraphics graphics, int mouseX, int mouseY) {
        Control control = at(mouseX, mouseY);
        if (control == null) return false;
        return page.tipAt(graphics, left(control), top(control), size(control), size(control), mouseX, mouseY,
                Component.translatable(TIP + control.name().toLowerCase(Locale.ROOT)));
    }
}
