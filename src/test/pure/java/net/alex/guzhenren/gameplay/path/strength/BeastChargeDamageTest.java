package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BeastChargeDamageTest {

    private static final float DELTA = 1.0E-4F;
    private static final List<BeastStrengthFamily> BEAR = List.of(BeastStrengthFamily.BEAR);
    private static final List<BeastStrengthFamily> BEAR_THEN_BOAR =
            List.of(BeastStrengthFamily.BEAR, BeastStrengthFamily.BOAR);
    private static final PathStrengthData BEAR_AND_BOAR = new PathStrengthData(
            Set.of(BeastStrength.BEAR, BeastStrength.WHITE_BOAR), Map.of());
    private static final PathStrengthData BEAR_AND_TWO_BOARS = new PathStrengthData(
            Set.of(BeastStrength.BEAR, BeastStrength.WHITE_BOAR, BeastStrength.BLACK_BOAR), Map.of());

    @Test
    @DisplayName("a family weighs its permanent points, in the order the charges fly")
    void aFamilyWeighsItsPermanentPoints() {
        assertArrayEquals(new int[] { 40, 24 }, BeastChargeDamage.getWeights(BEAR_AND_BOAR, BEAR_THEN_BOAR, List.of()));
        assertArrayEquals(new int[] { 24, 40 }, BeastChargeDamage.getWeights(BEAR_AND_BOAR,
                List.of(BeastStrengthFamily.BOAR, BeastStrengthFamily.BEAR), List.of()));
        assertArrayEquals(new int[] { 40, 48 },
                BeastChargeDamage.getWeights(BEAR_AND_TWO_BOARS, BEAR_THEN_BOAR, List.of()),
                "the two boars are one family");
        assertArrayEquals(new int[] { 0 }, BeastChargeDamage.getWeights(BEAR_AND_BOAR,
                List.of(BeastStrengthFamily.TIGER), List.of()), "a family not held weighs nothing");
    }

    @Test
    @DisplayName("a crit family weighs twice its points, the others once")
    void aCritFamilyWeighsTwice() {
        assertEquals(2, StrengthCrit.CRIT_FACTOR);
        assertArrayEquals(new int[] { 80, 24 }, BeastChargeDamage.getWeights(BEAR_AND_BOAR, BEAR_THEN_BOAR, BEAR));
        assertArrayEquals(new int[] { 40, 48 }, BeastChargeDamage.getWeights(BEAR_AND_BOAR, BEAR_THEN_BOAR,
                List.of(BeastStrengthFamily.BOAR)));
        assertArrayEquals(new int[] { 80, 48 },
                BeastChargeDamage.getWeights(BEAR_AND_BOAR, BEAR_THEN_BOAR, BEAR_THEN_BOAR));
    }

    @Test
    @DisplayName("the charges share the punch by their weights and add up to it")
    void theChargesShareThePunchByTheirWeights() {
        assertArrayEquals(new float[] { 5.0F, 3.0F }, BeastChargeDamage.split(8.0F, new int[] { 40, 24 }), DELTA);
        assertArrayEquals(new float[] { 5.3125F, 3.1875F }, BeastChargeDamage.split(8.5F, new int[] { 80, 48 }),
                DELTA);
        float[] uneven = BeastChargeDamage.split(10.0F, new int[] { 80, 24, 48 });
        assertArrayEquals(new float[] { 10.0F * 80 / 152, 10.0F * 24 / 152, 10.0F * 48 / 152 }, uneven, DELTA);
        assertEquals(10.0F, uneven[0] + uneven[1] + uneven[2], DELTA);
    }

    @Test
    @DisplayName("a lone charge carries the whole punch, crit or not")
    void aLoneChargeCarriesTheWholePunch() {
        assertArrayEquals(new float[] { 4.5F }, BeastChargeDamage.split(4.5F,
                BeastChargeDamage.getWeights(BEAR_AND_BOAR, BEAR, List.of())), DELTA);
        assertArrayEquals(new float[] { 5.5F }, BeastChargeDamage.split(5.5F,
                BeastChargeDamage.getWeights(BEAR_AND_BOAR, BEAR, BEAR)), DELTA);
    }

    @Test
    @DisplayName("charges that weigh nothing share the punch evenly; a negative weight counts as nothing")
    void weightlessChargesShareEvenly() {
        assertArrayEquals(new float[] { 3.0F, 3.0F }, BeastChargeDamage.split(6.0F, new int[] { 0, 0 }), DELTA);
        assertArrayEquals(new float[] { 0.0F, 6.0F }, BeastChargeDamage.split(6.0F, new int[] { -40, 24 }), DELTA);
        assertArrayEquals(new float[0], BeastChargeDamage.split(6.0F, new int[0]), DELTA);
    }
}
