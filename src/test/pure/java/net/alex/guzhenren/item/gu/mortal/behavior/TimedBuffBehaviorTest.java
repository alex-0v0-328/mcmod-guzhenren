package net.alex.guzhenren.item.gu.mortal.behavior;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.item.gu.MortalGuItem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TimedBuffBehaviorTest {

    @Test
    void durationAccumulatesOnlyDuringCooldownAndHigherGradeOverrides() {
        assertEquals(2 * Ticks.SECOND, MortalGuItem.REFINE_DONE_COOLDOWN_TICKS);
        assertEquals(1_200, TimedBuffBehavior.getNextDuration(600, 0, 600, 0, true));
        assertEquals(600, TimedBuffBehavior.getNextDuration(300, 0, 600, 0, false));
        assertEquals(1_200, TimedBuffBehavior.getNextDuration(600, 3, 1_200, 4, true));
        assertEquals(1_200, TimedBuffBehavior.getNextDuration(1_200, 4, 600, 3, true));
    }
}
