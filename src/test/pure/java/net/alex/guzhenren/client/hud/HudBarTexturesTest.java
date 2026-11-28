package net.alex.guzhenren.client.hud;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import net.alex.guzhenren.client.slider.HoldSlider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 钉住 HUD 条与状态圆珠贴图和不缩放绘制的布局（{@link HudBar}、{@link OrbDrawing}）：标题、堆叠依条排版，精灵图改尺寸即越界或不满，须变红待裁定。 */

class HudBarTexturesTest {

    private static final String ROOT = "/assets/guzhenren/textures/gui/sprites/hud/";

    @Test
    @DisplayName("the three hotbar bars' fills are the hotbar bar size, their frames one pixel larger all round")
    void hotbarBarsMatchTheLayout() throws IOException {
        for (String name : List.of("hotbar", "awakening", "nourish")) {
            assertBar(name, HotbarHud.BAR_WIDTH, HotbarHud.BAR_HEIGHT);
        }
    }

    @Test
    @DisplayName("the status orb frames are the orb sizes and the wave strip holds eight frames of three layers")
    void statusOrbsMatchTheLayout() throws IOException {
        assertSize("orb_main", OrbDrawing.MAIN, OrbDrawing.MAIN);
        assertSize("orb_sub", OrbDrawing.SUB, OrbDrawing.SUB);
        assertSize("orb_wave", OrbDrawing.WAVE_W, OrbDrawing.WAVE_H);
    }

    @Test
    @DisplayName("the hold slider's fill is the track size, its frame one pixel larger, its knob the knob size")
    void holdSliderMatchesTheLayout() throws IOException {
        assertBar("slider", HoldSlider.TRACK_WIDTH, HoldSlider.TRACK_HEIGHT);
        assertSize("slider_knob", HoldSlider.KNOB_WIDTH, HoldSlider.KNOB_HEIGHT);
    }

    private static void assertBar(String name, int width, int height) throws IOException {
        assertSize(name + "_bar_fill", width, height);
        assertSize(name + "_bar_background", width + 2, height + 2);
    }

    private static void assertSize(String sprite, int width, int height) throws IOException {
        try (InputStream input = HudBarTexturesTest.class.getResourceAsStream(ROOT + sprite + ".png")) {
            assertNotNull(input, "Missing packaged sprite hud/" + sprite);
            BufferedImage image = ImageIO.read(input);
            assertEquals(width, image.getWidth(), sprite + " width");
            assertEquals(height, image.getHeight(), sprite + " height");
        }
    }
}
