package net.alex.guzhenren.client.radial;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RadialMotion} 测试：展开 0.6 到 1 带过冲；扇区按错开间隔依次出现；选中停留后膨胀淡出，取消收缩淡出；悬停权重缓
 * 动无过冲；ARGB 辅助方法逐通道混合、淡出。
 */

class RadialMotionTest {

    private static final float EPS = 1.0E-4F;

    @Test
    @DisplayName("the ring grows from 0.6 to 1, overshooting on the way, and fades in")
    void openingGrowsWithOvershoot() {
        assertEquals(RadialMotion.OPEN_FROM, RadialMotion.getOpenScale(0L), EPS);
        assertEquals(1.0F, RadialMotion.getOpenScale(RadialMotion.OPEN_MS), EPS);
        assertEquals(1.0F, RadialMotion.getOpenScale(10_000L), EPS);
        assertTrue(RadialMotion.getOpenScale(RadialMotion.OPEN_MS * 3L / 4L) > 1.0F, "no overshoot");
        assertEquals(0.0F, RadialMotion.getOpenAlpha(0L), EPS);
        assertEquals(1.0F, RadialMotion.getOpenAlpha(RadialMotion.FADE_IN_MS), EPS);
    }

    @Test
    @DisplayName("each sector starts one stagger after the one before it")
    void sectorsAppearOneStaggerApart() {
        long third = 2L * RadialMotion.STAGGER_MS;
        assertEquals(0.0F, RadialMotion.getAppear(third, 2), EPS);
        assertTrue(RadialMotion.getAppear(third + 1L, 2) > 0.0F);
        assertEquals(1.0F, RadialMotion.getAppear(third + RadialMotion.APPEAR_MS, 2), EPS);
        assertEquals(1.0F, RadialMotion.getAppear(RadialMotion.APPEAR_MS, 0), EPS);
    }

    @Test
    @DisplayName("a pick swells to 1.08, holds, then fades out; a cancel shrinks to 0.8 while it fades")
    void exitsSwellOrShrink() {
        assertEquals(RadialMotion.PICK_TO, RadialMotion.getExitScale(RadialMotion.PICK_GROW_MS, true), EPS);
        assertEquals(1.0F, RadialMotion.getExitAlpha(RadialMotion.PICK_HOLD_MS, true), EPS);
        assertEquals(0.0F, RadialMotion.getExitAlpha(RadialMotion.PICK_HOLD_MS + RadialMotion.PICK_FADE_MS, true),
                EPS);
        assertFalse(RadialMotion.isExitDone(RadialMotion.PICK_HOLD_MS + RadialMotion.PICK_FADE_MS - 1L, true));
        assertTrue(RadialMotion.isExitDone(RadialMotion.PICK_HOLD_MS + RadialMotion.PICK_FADE_MS, true));

        assertEquals(RadialMotion.CANCEL_TO, RadialMotion.getExitScale(RadialMotion.CANCEL_MS, false), EPS);
        assertEquals(0.0F, RadialMotion.getExitAlpha(RadialMotion.CANCEL_MS, false), EPS);
        assertTrue(RadialMotion.isExitDone(RadialMotion.CANCEL_MS, false));
        assertEquals(1.0F, RadialMotion.getFlash(0L), EPS);
        assertEquals(0.0F, RadialMotion.getFlash(RadialMotion.FLASH_MS), EPS);
    }

    @Test
    @DisplayName("the hover weight eases toward its target and never passes it")
    void hoverEasesWithoutOvershoot() {
        assertEquals(0.3F, RadialMotion.approach(0.3F, 1.0F, 0.0F), EPS);
        float weight = 0.0F;
        for (int frame = 0; frame < 30; frame++) {
            float next = RadialMotion.approach(weight, 1.0F, 1.0F / 60.0F);
            assertTrue(next > weight && next <= 1.0F, "frame " + frame);
            weight = next;
        }
        assertTrue(weight > 0.99F, "still at " + weight + " after half a second");
        assertEquals(1.0F, RadialMotion.approach(0.0F, 1.0F, 1.0F), EPS);
    }

    @Test
    @DisplayName("colors blend and fade channel by channel")
    void colorsBlendAndFade() {
        assertEquals(0x80808080, RadialMotion.mix(0x00000000, 0xFFFFFFFF, 0.5F));
        assertEquals(0xBF000000, RadialMotion.mix(0xBF000000, 0x99FFD54F, 0.0F));
        assertEquals(0x99FFD54F, RadialMotion.mix(0xBF000000, 0x99FFD54F, 1.0F));
        assertEquals(0x80123456, RadialMotion.fade(0xFF123456, 128.0F / 255.0F));
        assertEquals(0x00123456, RadialMotion.fade(0xFF123456, 0.0F));
    }
}
