package net.alex.guzhenren.client.screen.immortalaperture.map;

import com.mojang.math.Axis;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.screen.Widgets;
import net.alex.guzhenren.client.screen.immortalaperture.EarthSpiritScreen;
import net.alex.guzhenren.client.screen.immortalaperture.PanelDrawing;
import net.alex.guzhenren.client.screen.immortalaperture.map.render.MapCamera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 地图页右上角的罗盘（Alex 定：俯视与回中心两键删掉，只用 V / H，按键都写在功能栏上方的提示条）：盘面静止，指针按 90° ×
 * 旋转数顺时针转（金尖 = 北），点罗盘顺时针转一档；只管排版、命中与绘制。
 */

final class MapControls {

    private static final int COMPASS = 28;
    private static final int TOP = EarthSpiritScreen.BAR_H + 5;
    private static final String TIP = EarthSpiritScreen.TIP + "map_compass";
    private static final ResourceLocation DIAL = PanelDrawing.sprite("compass");
    private static final ResourceLocation NEEDLE = PanelDrawing.sprite("compass_needle");

    private int compassX;

    void layout(int width) { compassX = width - 6 - COMPASS; }

    int bottom() { return TOP + COMPASS; }

    boolean contains(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, compassX, TOP, COMPASS, COMPASS);
    }

    void sprites(SpriteBatch sprites) { sprites.blit(DIAL, compassX, TOP, COMPASS, COMPASS); }

    void needle(GuiGraphics graphics, MapCamera camera) {
        graphics.pose().pushPose();
        graphics.pose().translate(compassX + COMPASS / 2.0F, TOP + COMPASS / 2.0F, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(90.0F * camera.rotation()));
        graphics.blitSprite(NEEDLE, -COMPASS / 2, -COMPASS / 2, COMPASS, COMPASS);
        graphics.pose().popPose();
    }

    boolean tip(MapPage page, GuiGraphics graphics, int mouseX, int mouseY) {
        return page.tipAt(graphics, compassX, TOP, COMPASS, COMPASS, mouseX, mouseY, Component.translatable(TIP));
    }
}
