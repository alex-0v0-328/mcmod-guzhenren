package net.alex.guzhenren.item.gu.tending;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link GuRefineCooldown#longest}：取物品等待、炼化后保护期、效果冷却的最大值；效果冷却可由
 * {@code canUseDuringEffectCooldown} 免除。
 */

class GuRefineCooldownTest {

    @Test
    void takesTheLargestOfTheThree() {
        assertEquals(30, GuRefineCooldown.longest(5, 20, false, 30));
        assertEquals(20, GuRefineCooldown.longest(5, 20, false, 10));
        assertEquals(40, GuRefineCooldown.longest(40, 20, false, 10));
    }

    @Test
    void aWaivedEffectCooldownNeverCounts() {
        assertEquals(20, GuRefineCooldown.longest(5, 20, true, 300));
        assertEquals(0, GuRefineCooldown.longest(0, 0, true, 300));
    }

    @Test
    void theGateAsksForNoItemWait() {
        assertEquals(0, GuRefineCooldown.longest(0, 0, false, 0));
        assertEquals(12, GuRefineCooldown.longest(0, 0, false, 12));
    }
}
