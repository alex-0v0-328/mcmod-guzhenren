package net.alex.guzhenren.item.gu.tending;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.item.gu.stack.GuCooldowns;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TendedGuItemTest {

    @Test
    void postRefineCooldownIsOneSecond() {
        assertEquals(Ticks.SECOND, TendedGuItem.POST_REFINE_COOLDOWN_TICKS);
    }

    @Test
    void refineStampBlocksFirstUseUntilTheWindowElapses() {
        long refinedAt = 1_000L;
        int window = TendedGuItem.POST_REFINE_COOLDOWN_TICKS;

        assertEquals(window, GuCooldowns.stampCooldownLeft(refinedAt, refinedAt, window));
        assertEquals(1, GuCooldowns.stampCooldownLeft(refinedAt + window - 1, refinedAt, window));
        assertEquals(0, GuCooldowns.stampCooldownLeft(refinedAt + window, refinedAt, window));
        assertEquals(0, GuCooldowns.stampCooldownLeft(refinedAt, null, window));
    }
}
