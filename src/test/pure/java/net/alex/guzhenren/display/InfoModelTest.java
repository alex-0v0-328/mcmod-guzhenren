package net.alex.guzhenren.display;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfoModelTest {

    @Test
    void attackRowShowsForNonStrengthAttackBonus() {
        assertTrue(InfoModel.shouldShowAttackRow(true, 5.0D));
        assertFalse(InfoModel.shouldShowAttackRow(true, 0.0D));
        assertTrue(InfoModel.shouldShowAttackRow(false, 0.0D));
    }

    @Test
    void capacityRowCarriesTheTotalTheOneCapacityAndTheOverload() {
        InfoModel.CapacityRow row = new InfoModel.CapacityRow(64, 56, 8);
        assertEquals(64, row.total());
        assertEquals(56, row.capacity());
        assertEquals(8, row.overload());
    }

    @Test
    void critRowCarriesTheChance() {
        assertEquals(0.9F, new InfoModel.CritRow(0.9F).chance());
    }
}
