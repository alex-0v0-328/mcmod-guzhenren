package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.item.gu.mortal.path.strength.SelfRelianceBehavior;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 自动蛊算术：苦力蛊[Hardship Strength Gu]伤势档位边界；每档给人身或兽力虚影身的力量点[Strength Points]与容量加成；
 * 自力更生蛊[Self Reliance Gu]脉冲曲线；活玩家见 {@code HardshipStrengthGameTests}、{@code SelfRelianceGameTests}。
 */

class HardshipStrengthTest {

    private static final float MAX = 20.0F;
    private static final ResourceLocation SOURCE = ResourceLocation.fromNamespaceAndPath("guzhenren", "test");

    @Test
    @DisplayName("the tiers turn at 75 / 60 / 45 / 30 / 15 percent, each bound in the lower tier")
    void tiersAtTheirBounds() {
        assertEquals(0, HardshipStrength.getTier(MAX, MAX));
        assertEquals(0, HardshipStrength.getTier(MAX * 0.75F, MAX));
        assertEquals(1, HardshipStrength.getTier(MAX * 0.749F, MAX));
        assertEquals(1, HardshipStrength.getTier(MAX * 0.601F, MAX));
        assertEquals(2, HardshipStrength.getTier(MAX * 0.60F, MAX));
        assertEquals(3, HardshipStrength.getTier(MAX * 0.45F, MAX));
        assertEquals(4, HardshipStrength.getTier(MAX * 0.30F, MAX));
        assertEquals(5, HardshipStrength.getTier(MAX * 0.15F, MAX));
        assertEquals(5, HardshipStrength.getTier(MAX * 0.149F, MAX));
        assertEquals(0, HardshipStrength.getTier(0.0F, MAX), "a dead body is in no tier");
    }

    @Test
    @DisplayName("each tier adds 5 / 10 / 25 / 50 / 100 percent of the normal capacity, rounded down")
    void capacityBonusByTier() {
        int[] expected = { 0, 2, 5, 14, 28, 56 };
        for (int tier = 0; tier <= HardshipStrength.TIERS; tier++) {
            assertEquals(expected[tier], HardshipStrength.getCapacityBonus(56, tier), "tier " + tier);
        }
        assertEquals(0, HardshipStrength.getCapacityBonus(16, 1), "0.8 rounds down to nothing");
        assertEquals(1, HardshipStrength.getCapacityBonus(20, 1), "a 20 capacity's lightest tier is one point");
        assertEquals(0, HardshipStrength.getCapacityBonus(-8, 5), "a negative capacity gained");
        assertEquals(Integer.MAX_VALUE, HardshipStrength.getCapacityBonus(Integer.MAX_VALUE, 5), "no overflow");
    }

    @Test
    @DisplayName("the amplifier lends no points and no family, only capacity, and only to a body holding a branch")
    void amplifierShape() {
        assertEquals(new StrengthAmplifier(SOURCE, null, 0, 5), HardshipStrength.getAmplifier(SOURCE, true, 56, 2));
        assertNull(HardshipStrength.getAmplifier(SOURCE, false, 56, 5), "no branch, yet lent");
        assertNull(HardshipStrength.getAmplifier(SOURCE, true, 56, 0), "healthy, yet lent");
        assertNull(HardshipStrength.getAmplifier(SOURCE, true, 16, 1), "lent nothing, yet an amplifier");
    }

    @Test
    @DisplayName("the Self Reliance pulse is full at or under 40 percent and falls linearly to a quarter at 75")
    void selfRelianceCurve() {
        assertEquals(1.0F, SelfRelianceBehavior.getCurve(MAX * 0.10F, MAX));
        assertEquals(1.0F, SelfRelianceBehavior.getCurve(MAX * 0.40F, MAX));
        assertEquals(0.625F, SelfRelianceBehavior.getCurve(MAX * 0.575F, MAX), 1.0E-4F);
        assertEquals(SelfRelianceBehavior.FLOOR, SelfRelianceBehavior.getCurve(MAX * 0.75F, MAX), 1.0E-4F);
    }
}
