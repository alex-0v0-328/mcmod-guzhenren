package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BeastChargeDamageTest {

    private static final int RANK_FIVE_CAPACITY = 56;
    private static final int RANK_FIVE_ALL_OUT_CAPACITY = 168;
    private static final float DELTA = 1.0E-4F;
    private static final PathStrengthData BEAR_AND_BOAR = new PathStrengthData(
            Set.of(BeastStrength.BEAR, BeastStrength.WHITE_BOAR), Map.of());

    @Test
    @DisplayName("a charge is worth 0.25 attack a point, twice the fist")
    void aPointIsTwiceTheFist() {
        assertEquals(2.0D * PathStrengthData.ATTACK_PER_POINT, BeastChargeDamage.ATTACK_PER_POINT);
    }

    @Test
    @DisplayName("Strength Qi: a rank five bear hits 10.0, released 14.0 cut to the capacity, under All-Out 30.0")
    void strengthQiBearIsCutToTheCapacity() {
        List<BeastStrengthFamily> bear = List.of(BeastStrengthFamily.BEAR);
        assertArrayEquals(new float[] { 10.0F }, BeastChargeDamage.getDamages(BEAR_AND_BOAR, bear, List.of(),
                StrengthService.FULL_RELEASE_FACTOR, RANK_FIVE_CAPACITY), DELTA);
        assertArrayEquals(new float[] { 14.0F }, BeastChargeDamage.getDamages(BEAR_AND_BOAR, bear, bear,
                StrengthService.FULL_RELEASE_FACTOR, RANK_FIVE_CAPACITY), DELTA);
        assertArrayEquals(new float[] { 30.0F }, BeastChargeDamage.getDamages(BEAR_AND_BOAR, bear, bear,
                StrengthService.FULL_RELEASE_FACTOR, RANK_FIVE_ALL_OUT_CAPACITY), DELTA);
    }

    @Test
    @DisplayName("charges past the capacity share the cut by their points")
    void chargesShareTheCutByTheirPoints() {
        List<BeastStrengthFamily> both = List.of(BeastStrengthFamily.BEAR, BeastStrengthFamily.BOAR);
        assertArrayEquals(new float[] { 10.0F, 6.0F }, BeastChargeDamage.getDamages(BEAR_AND_BOAR, both, List.of(),
                StrengthService.FULL_RELEASE_FACTOR, 64), DELTA);
        assertArrayEquals(new float[] { 5.0F, 3.0F }, BeastChargeDamage.getDamages(BEAR_AND_BOAR, both, List.of(),
                StrengthService.FULL_RELEASE_FACTOR, 32), DELTA);
        assertArrayEquals(new float[] { 0.0F, 0.0F }, BeastChargeDamage.getDamages(BEAR_AND_BOAR, both, List.of(),
                StrengthService.FULL_RELEASE_FACTOR, 0), DELTA);
    }

    @Test
    @DisplayName("the charged punch: a released bear hits 120.0 uncut, a random family once")
    void chargedPunchIsUncut() {
        List<BeastStrengthFamily> bear = List.of(BeastStrengthFamily.BEAR);
        assertArrayEquals(new float[] { 120.0F }, BeastChargeDamage.getDamages(BEAR_AND_BOAR, bear, bear,
                StoreStrength.CHARGED_RELEASE_FACTOR, BeastChargeDamage.UNCAPPED), DELTA);
        assertArrayEquals(new float[] { 6.0F }, BeastChargeDamage.getDamages(BEAR_AND_BOAR,
                List.of(BeastStrengthFamily.BOAR), bear, StoreStrength.CHARGED_RELEASE_FACTOR,
                BeastChargeDamage.UNCAPPED), DELTA);
    }

    @Test
    @DisplayName("the points never go negative or overflow")
    void pointsStayInRange() {
        assertEquals(0, BeastChargeDamage.getPoints(-40, 3));
        assertEquals(0, BeastChargeDamage.getPoints(40, -1));
        assertEquals(Integer.MAX_VALUE, BeastChargeDamage.getPoints(Integer.MAX_VALUE, 12));
    }
}
