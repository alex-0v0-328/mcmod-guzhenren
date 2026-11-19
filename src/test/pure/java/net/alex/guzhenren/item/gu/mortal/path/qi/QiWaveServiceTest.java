package net.alex.guzhenren.item.gu.mortal.path.qi;

import java.util.List;
import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.entity.projectile.QiBurst;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QiWaveServiceTest {

    private static final double REACH = 3.0D;

    @Test
    @DisplayName("Strength Qi Gu spec: 30 seconds")
    void specIsPinned() {
        assertEquals(600, StrengthQiGuEffect.DURATION_TICKS);
    }

    @Test
    @DisplayName("a wave takes any target past the reach, with no far limit")
    void waveTakesEveryTargetPastTheReach() {
        assertFalse(QiWaveService.isPastReach(2.0D, REACH), "inside the reach stays a melee hit");
        assertFalse(QiWaveService.isPastReach(REACH, REACH), "the reach's edge stays a melee hit");
        assertTrue(QiWaveService.isPastReach(REACH + 0.01D, REACH));
        assertTrue(QiWaveService.isPastReach(64.0D, REACH), "a far lock-on target is a wave");
    }

    @Test
    @DisplayName("a ranged punch is worth half the fist; its burst reaches 3 blocks and spills over 8 targets at most")
    void theRangedPunchIsPinned() {
        assertEquals(0.5F, QiWaveService.RANGED_RATIO);
        assertEquals(3.0D, QiBurst.RADIUS, 1.0E-9D);
        assertEquals(8, QiBurst.MAX_TARGETS);
        assertEquals(3.0D, QiBurst.Spec.QI_WAVE.radius(), 1.0E-9D);
        assertFalse(QiBurst.Spec.QI_WAVE.areaImpact(), "the plain burst opens the area impact");
        assertNull(QiBurst.Spec.QI_WAVE.shot(), "the plain burst carries an overload shot");
    }

    @Test
    @DisplayName("a beast holder charges with every family the round drew, else one held; anyone else throws an orb")
    void theChargeFamilies() {
        RandomSource random = RandomSource.create(0L);
        List<BeastStrengthFamily> held = List.of(BeastStrengthFamily.BOAR, BeastStrengthFamily.BEAR);
        List<BeastStrengthFamily> drawn = List.of(BeastStrengthFamily.BEAR, BeastStrengthFamily.BOAR);
        assertEquals(List.of(), QiWaveService.getChargeFamilies(false, drawn, held, random));
        assertEquals(List.of(), QiWaveService.getChargeFamilies(true, List.of(), List.of(), random),
                "no permanent beast: an orb");
        assertEquals(drawn, QiWaveService.getChargeFamilies(true, drawn, held, random));
        for (long seed = 0L; seed < 16L; seed++) {
            List<BeastStrengthFamily> families =
                    QiWaveService.getChargeFamilies(true, List.of(), held, RandomSource.create(seed));
            assertEquals(1, families.size(), "a round with no draw charged with " + families);
            assertTrue(held.contains(families.getFirst()), "charged with a family not held: " + families);
        }
    }

    @Test
    @DisplayName("every drawn family charges, the ox and the turtle with the rest, alone each charges itself")
    void everyDrawnFamilyCharges() {
        RandomSource random = RandomSource.create(0L);
        List<BeastStrengthFamily> held = List.of(BeastStrengthFamily.OX, BeastStrengthFamily.HORSE);
        assertEquals(held, QiWaveService.getChargeFamilies(true, held, held, random));
        assertEquals(List.of(BeastStrengthFamily.OX), QiWaveService.getChargeFamilies(true,
                List.of(BeastStrengthFamily.OX), List.of(BeastStrengthFamily.OX), random), "an ox alone did not charge");
        assertEquals(List.of(BeastStrengthFamily.TURTLE), QiWaveService.getChargeFamilies(true,
                List.of(BeastStrengthFamily.TURTLE), List.of(BeastStrengthFamily.TURTLE, BeastStrengthFamily.HORSE),
                random), "a released turtle did not charge itself");
    }
}
