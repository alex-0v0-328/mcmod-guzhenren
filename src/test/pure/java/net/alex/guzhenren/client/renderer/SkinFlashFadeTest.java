package net.alex.guzhenren.client.renderer;

import net.alex.guzhenren.gameplay.body.Skin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinFlashFadeTest {

    @Test
    @DisplayName("a flash starts at its full alpha and is gone after the fade")
    void alphaFallsToZeroOverTheFade() {
        assertEquals(SkinFlashes.START_ALPHA, SkinFlashes.Flash.getAlpha(0.0F));
        assertEquals(SkinFlashes.START_ALPHA / 2.0F, SkinFlashes.Flash.getAlpha(SkinFlashes.FADE_TICKS / 2.0F), 1e-6F);
        assertEquals(0.0F, SkinFlashes.Flash.getAlpha(SkinFlashes.FADE_TICKS));
        assertEquals(0.0F, SkinFlashes.Flash.getAlpha(SkinFlashes.FADE_TICKS + 5.0F));
        assertEquals(SkinFlashes.START_ALPHA, SkinFlashes.Flash.getAlpha(-1.0F));
    }

    @Test
    @DisplayName("the jade skin flashes green and the white jade skin near white")
    void eachSkinHasItsTint() {
        SkinFlashes.Flash jade = new SkinFlashes.Flash(Skin.JADE_SKIN, 0L);
        SkinFlashes.Flash white = new SkinFlashes.Flash(Skin.WHITE_JADE_SKIN, 0L);
        int jadeGreen = (jade.getTint() >> 8) & 0xFF;
        int jadeRed = (jade.getTint() >> 16) & 0xFF;
        assertTrue(jadeGreen > jadeRed, "jade reads green");
        int whiteRed = (white.getTint() >> 16) & 0xFF;
        int whiteBlue = white.getTint() & 0xFF;
        assertTrue(whiteRed >= 0xE0 && whiteBlue >= 0xE0, "white jade reads white");
        assertEquals(0.4F, jade.getAlpha(0L, SkinFlashes.FADE_TICKS / 3.0F), 1e-6F);
    }

    @Test
    @DisplayName("bronze flashes warm and iron flashes gray, each family one tint across its ranks")
    void permanentFamiliesHaveTheirOwnTints() {
        int bronze = SkinFlashes.getTintOf(Skin.BRONZE_SKIN_1);
        int iron = SkinFlashes.getTintOf(Skin.IRON_SKIN_1);
        assertEquals(bronze, SkinFlashes.getTintOf(Skin.BRONZE_SKIN_4));
        assertEquals(iron, SkinFlashes.getTintOf(Skin.IRON_SKIN_4));
        assertTrue(((bronze >> 16) & 0xFF) > (bronze & 0xFF), "bronze reads warm");
        int ironRed = (iron >> 16) & 0xFF;
        int ironBlue = iron & 0xFF;
        assertTrue(Math.abs(ironRed - ironBlue) < 0x20, "iron reads gray");
        assertTrue(bronze != SkinFlashes.getTintOf(Skin.JADE_SKIN), "bronze is not jade");
        assertTrue(iron != SkinFlashes.getTintOf(Skin.WHITE_JADE_SKIN), "iron is not white jade");
    }
}
