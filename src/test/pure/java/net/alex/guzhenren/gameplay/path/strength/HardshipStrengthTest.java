package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import net.alex.guzhenren.item.gu.mortal.path.strength.SelfRelianceBehavior;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 自动蛊算术：苦力蛊[Hardship Strength Gu]伤势档位边界；每档给已入分支身体的伤害倍率，不给力量点[Strength Points]与
 * 承受力；自力更生蛊[Self Reliance Gu]脉冲曲线与力量倍率；活玩家见 {@code HardshipStrengthGameTests}、
 * {@code SelfRelianceGameTests}。
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
    @DisplayName("each tier adds 10 / 20 / 30 / 40 / 50 percent to the damage")
    void damagePercentByTier() {
        int[] expected = { 0, 10, 20, 30, 40, 50 };
        assertEquals(5, HardshipStrength.TIERS);
        for (int tier = 0; tier <= HardshipStrength.TIERS; tier++) {
            assertEquals(expected[tier], HardshipStrength.getDamagePercent(tier), "tier " + tier);
        }
    }

    @Test
    @DisplayName("the amplifier lends no points or family, only the damage factor, and only to a branch")
    void amplifierShape() {
        assertEquals(StrengthAmplifier.damage(SOURCE, 20), HardshipStrength.getAmplifier(SOURCE, true, 2));
        assertEquals(new StrengthAmplifier(SOURCE, null, 0, 50), HardshipStrength.getAmplifier(SOURCE, true, 5));
        assertNull(HardshipStrength.getAmplifier(SOURCE, false, 5), "no branch, yet lent");
        assertNull(HardshipStrength.getAmplifier(SOURCE, true, 0), "healthy, yet lent");
    }

    @Test
    @DisplayName("the tier multiplies the settled points and leaves the total and the capacity alone")
    void tierMultipliesTheSettledPoints() {
        int[] settled = { 40, 44, 48, 52, 56, 60 };
        for (int tier = 0; tier <= HardshipStrength.TIERS; tier++) {
            StrengthAmplifier amplifier = HardshipStrength.getAmplifier(SOURCE, true, tier);
            StrengthSnapshot snapshot = StrengthSnapshot.of(16, 40, amplifier == null ? List.of() : List.of(amplifier));
            assertEquals(40, snapshot.totalPoints(), "tier " + tier);
            assertEquals(16, snapshot.capacity(), "tier " + tier);
            assertEquals(settled[tier], snapshot.settledPoints(), "tier " + tier);
            assertEquals(settled[tier] - 16, snapshot.overloadPoints(snapshot.settledPoints()), "tier " + tier);
        }
    }

    @Test
    @DisplayName("the Self Reliance pulse is full at or under 40 percent and falls linearly to a quarter at 75")
    void selfRelianceCurve() {
        assertEquals(1.0F, SelfRelianceBehavior.getCurve(MAX * 0.10F, MAX));
        assertEquals(1.0F, SelfRelianceBehavior.getCurve(MAX * 0.40F, MAX));
        assertEquals(0.625F, SelfRelianceBehavior.getCurve(MAX * 0.575F, MAX), 1.0E-4F);
        assertEquals(SelfRelianceBehavior.FLOOR, SelfRelianceBehavior.getCurve(MAX * 0.75F, MAX), 1.0E-4F);
    }

    @Test
    @DisplayName("the Self Reliance pulse scales by clamp(0.5 + total points / 200, 0.5, 1.5): 0.5 bare, 1 at 100, "
            + "1.5 from 200")
    void selfRelianceStrengthFactor() {
        assertEquals(0.5F, SelfRelianceBehavior.getStrengthFactor(0), 1.0E-6F);
        assertEquals(0.5F, SelfRelianceBehavior.getStrengthFactor(-40), 1.0E-6F, "negative points went under 0.5");
        assertEquals(0.75F, SelfRelianceBehavior.getStrengthFactor(50), 1.0E-6F);
        assertEquals(1.0F, SelfRelianceBehavior.getStrengthFactor(100), 1.0E-6F);
        assertEquals(1.25F, SelfRelianceBehavior.getStrengthFactor(150), 1.0E-6F);
        assertEquals(1.5F, SelfRelianceBehavior.getStrengthFactor(200), 1.0E-6F);
        assertEquals(1.5F, SelfRelianceBehavior.getStrengthFactor(3000), 1.0E-6F, "the factor passed 1.5");
    }
}
