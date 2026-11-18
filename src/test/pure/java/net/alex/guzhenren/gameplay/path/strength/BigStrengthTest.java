package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 大力蛊[Big Strength Gu]算术：承受加成为点数一半、向下取整；增幅非兽力，超承受只截断不过载；活玩家见
 * {@code BigStrengthGameTests}。
 */

class BigStrengthTest {

    private static final ResourceLocation SOURCE = ResourceLocation.fromNamespaceAndPath("guzhenren", "test");

    @Test
    @DisplayName("the capacity bonus is half the points, rounded down")
    void capacityBonusIsHalf() {
        assertEquals(16, BigStrength.getCapacityBonus(BigStrength.POINTS));
        assertEquals(16, BigStrength.getCapacityBonus(33));
        assertEquals(0, BigStrength.getCapacityBonus(1));
        assertEquals(0, BigStrength.getCapacityBonus(-8), "negative points lent capacity");
    }

    @Test
    @DisplayName("the amplifier lends its points and half as capacity, with no beast family")
    void amplifierShape() {
        StrengthAmplifier amplifier = BigStrength.getAmplifier(SOURCE, BigStrength.POINTS);
        assertEquals(new StrengthAmplifier(SOURCE, null, 32, 16), amplifier);
        assertFalse(amplifier.isBeast(), "the big strength counts as a timed beast strength");
        assertNull(BigStrength.getAmplifier(SOURCE, 0), "no points, yet an amplifier");
    }

    @Test
    @DisplayName("a body at its capacity still brings half the points to bear, and none of it overloads")
    void halfIsUsableAtFullCapacity() {
        StrengthSnapshot snapshot = StrengthSnapshot.of(56, 200, 0,
                List.of(BigStrength.getAmplifier(SOURCE, BigStrength.POINTS)), StrengthService.NO_ALL_OUT);
        assertEquals(232, snapshot.totalPoints());
        assertEquals(72, snapshot.capacity());
        assertEquals(72, StrengthService.getUsablePoints(snapshot.capacity(), snapshot.totalPoints()));
        assertEquals(0, snapshot.overloadPoints(), "the big strength paid a body overload");

        StrengthSnapshot beast = StrengthSnapshot.of(56, 56, 56,
                List.of(BigStrength.getAmplifier(SOURCE, BigStrength.POINTS)), StrengthService.NO_ALL_OUT);
        assertEquals(72, StrengthService.getUsablePoints(beast.capacity(), beast.totalPoints()));
        assertEquals(0, beast.overloadPoints(), "the big strength overloaded a beast body");
    }
}
