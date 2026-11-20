package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 大力蛊[Big Strength Gu]算术：增幅只给点数，不带承受力、非兽力；超出承受力的部分全额发挥并计入过载；活玩家见
 * {@code BigStrengthGameTests}。
 */

class BigStrengthTest {

    private static final ResourceLocation SOURCE = ResourceLocation.fromNamespaceAndPath("guzhenren", "test");

    @Test
    @DisplayName("the amplifier lends its points and nothing else: no damage factor, no beast family")
    void amplifierShape() {
        StrengthAmplifier amplifier = BigStrength.getAmplifier(SOURCE, BigStrength.POINTS);
        assertEquals(32, BigStrength.POINTS);
        assertEquals(new StrengthAmplifier(SOURCE, null, 32, 0), amplifier);
        assertFalse(amplifier.isBeast(), "the big strength counts as a timed beast strength");
        assertNull(BigStrength.getAmplifier(SOURCE, 0), "no points, yet an amplifier");
        assertNull(BigStrength.getAmplifier(SOURCE, -8), "negative points, yet an amplifier");
    }

    @Test
    @DisplayName("the points past the capacity land in full and all of them overload")
    void pointsPastTheCapacityOverload() {
        List<StrengthAmplifier> big = List.of(BigStrength.getAmplifier(SOURCE, BigStrength.POINTS));
        StrengthSnapshot bare = StrengthSnapshot.of(56, 0, big);
        assertEquals(56, bare.capacity(), "the big strength lifted the capacity");
        assertEquals(32, bare.settledPoints());
        assertEquals(0, bare.overloadPoints(bare.settledPoints()), "32 points under 56 overloaded");

        StrengthSnapshot beast = StrengthSnapshot.of(56, 40, big);
        assertEquals(72, beast.totalPoints());
        assertEquals(56, beast.capacity());
        assertEquals(72, beast.settledPoints(), "the points past the capacity were cut");
        assertEquals(16, beast.overloadPoints(beast.settledPoints()));
        assertEquals(112, beast.settle(40, 1.0D), "the big strength was doubled beside the crit bear");

        StrengthSnapshot human = StrengthSnapshot.of(56, 200, big);
        assertEquals(232, human.settledPoints());
        assertEquals(176, human.overloadPoints(human.settledPoints()));
    }
}
