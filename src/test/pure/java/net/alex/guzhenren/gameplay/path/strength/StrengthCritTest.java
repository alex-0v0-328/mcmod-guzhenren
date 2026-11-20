package net.alex.guzhenren.gameplay.path.strength;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrengthCritTest {

    private static final List<BeastStrengthFamily> THREE =
            List.of(BeastStrengthFamily.BOAR, BeastStrengthFamily.BEAR, BeastStrengthFamily.TIGER);
    private static final int RANK_THREE_ALL_OUT = StrengthService.ALL_OUT_LOWEST_TIER;
    private static final float DELTA = 1.0E-6F;

    //region Chance [暴击率]
    @Test
    @DisplayName("the crit chance is 5% at rest and 10% with the brown bear's innate strength")
    void theRestingChance() {
        assertEquals(0.05F, StrengthCrit.BASE_CHANCE, DELTA);
        assertEquals(0.05F, StrengthCrit.getChance(StrengthService.NO_ALL_OUT, 0.0F), DELTA);
        assertEquals(0.10F, StrengthCrit.getChance(StrengthService.NO_ALL_OUT,
                BeastStrength.BROWN_BEAR.getChanceBonus()), DELTA, "the brown bear did not add 5%");
        assertEquals(0.05F, StrengthCrit.getChance(RANK_THREE_ALL_OUT - 1, 0.0F), DELTA,
                "a tier under rank three raised the chance");
    }

    @Test
    @DisplayName("All-Out Effort sets 80 / 90 / 100% for its rank three / four / five Gu and ignores the beast bonus")
    void allOutEffortSetsTheChanceByItsTier() {
        float brownBear = BeastStrength.BROWN_BEAR.getChanceBonus();
        assertEquals(0.80F, StrengthCrit.getChance(RANK_THREE_ALL_OUT, 0.0F), DELTA);
        assertEquals(0.90F, StrengthCrit.getChance(RANK_THREE_ALL_OUT + 1, 0.0F), DELTA);
        assertEquals(1.0F, StrengthCrit.getChance(RANK_THREE_ALL_OUT + 2, 0.0F), DELTA);
        assertEquals(1.0F, StrengthCrit.getChance(RANK_THREE_ALL_OUT + 9, 0.0F), DELTA, "a tier past five broke");
        assertEquals(0.80F, StrengthCrit.getChance(RANK_THREE_ALL_OUT, brownBear), DELTA,
                "the brown bear raised the rank three All-Out chance");
        assertEquals(0.90F, StrengthCrit.getChance(RANK_THREE_ALL_OUT + 1, brownBear), DELTA,
                "the brown bear raised the rank four All-Out chance");
        assertEquals(1.0F, StrengthCrit.getChance(RANK_THREE_ALL_OUT + 2, brownBear), DELTA,
                "the brown bear pushed the rank five All-Out chance past 100%");
    }
    //endregion

    //region Count [兽种数]
    @Test
    @DisplayName("a beast crit draws min(1 + Hardship tier, families): one family off Hardship or at tier 0")
    void theDrawCountFollowsTheHardshipTier() {
        assertEquals(1, StrengthCrit.getCount(0, 6), "tier 0 drew other than one");
        assertEquals(2, StrengthCrit.getCount(1, 6));
        assertEquals(3, StrengthCrit.getCount(2, 6));
        assertEquals(4, StrengthCrit.getCount(3, 6));
        assertEquals(5, StrengthCrit.getCount(4, 6));
        assertEquals(6, StrengthCrit.getCount(5, 6), "tier five drew other than six");
        assertEquals(3, StrengthCrit.getCount(5, 3), "the draw passed the families held");
        assertEquals(1, StrengthCrit.getCount(4, 1));
        assertEquals(0, StrengthCrit.getCount(0, 0), "a body with no family drew one");
        assertEquals(0, StrengthCrit.getCount(5, 0), "a body with no family drew under Hardship");
    }

    @Test
    @DisplayName("a draw never repeats a family and never draws more than there are")
    void aDrawNeverRepeats() {
        for (long seed = 0L; seed < 64L; seed++) {
            RandomSource random = RandomSource.create(seed);
            for (int count = 0; count <= 4; count++) {
                List<BeastStrengthFamily> picked = StrengthCrit.pickFamilies(THREE, count, random);
                assertEquals(Math.min(count, THREE.size()), picked.size(), "seed " + seed + " count " + count);
                assertEquals(picked.size(), new HashSet<>(picked).size(), "a family repeated: " + picked);
                assertTrue(THREE.containsAll(picked), "drew a family not held: " + picked);
            }
        }
    }

    @Test
    @DisplayName("every held family can be drawn first")
    void everyFamilyCanComeFirst() {
        Set<BeastStrengthFamily> firsts = new HashSet<>();
        for (long seed = 0L; seed < 64L; seed++) {
            firsts.add(StrengthCrit.pickFamilies(THREE, 1, RandomSource.create(seed)).getFirst());
        }
        assertEquals(Set.copyOf(THREE), firsts);
    }

    @Test
    @DisplayName("only permanent families are available, two boars as one, in declaration order")
    void availableFamilies() {
        PathStrengthData data = new PathStrengthData(
                Set.of(BeastStrength.TIGER, BeastStrength.WHITE_BOAR, BeastStrength.BLACK_BOAR), Map.of());
        assertEquals(List.of(BeastStrengthFamily.BOAR, BeastStrengthFamily.TIGER),
                StrengthCrit.getAvailableFamilies(data));
        assertEquals(List.of(), StrengthCrit.getAvailableFamilies(PathStrengthData.DEFAULT));
        assertEquals(List.of(), StrengthCrit.getAvailableFamilies(
                new PathStrengthData(Set.of(), Map.of(HumanStrength.JUN, 2))), "human strength offered a family");
    }
    //endregion

    //region Points [暴击点数]
    @Test
    @DisplayName("every drawn family counts its permanent points once more, so a crit family settles twice")
    void everyDrawnFamilyCountsTwice() {
        assertEquals(2, StrengthCrit.CRIT_FACTOR);
        PathStrengthData data = new PathStrengthData(Set.of(BeastStrength.TIGER, BeastStrength.BEAR), Map.of());
        List<BeastStrengthFamily> both = List.of(BeastStrengthFamily.TIGER, BeastStrengthFamily.BEAR);
        assertEquals(40, StrengthCrit.getCritPoints(List.of(BeastStrengthFamily.BEAR), data));
        assertEquals(64, StrengthCrit.getCritPoints(List.of(BeastStrengthFamily.TIGER), data));
        assertEquals(104, StrengthCrit.getCritPoints(both, data), "a drawn family was left out of the crit");
        assertEquals(0, StrengthCrit.getCritPoints(List.of(), data), "a missed roll added points");

        assertEquals(104 + 40, StrengthService.getSettledPoints(104, 40, 1.0D), "the bear did not double");
        assertEquals(2 * 104, StrengthService.getSettledPoints(104, 104, 1.0D), "both families did not double");
    }

    @Test
    @DisplayName("the two boars crit as one family, and a same-family timed beast is never doubled")
    void onlyPermanentPointsCrit() {
        PathStrengthData boars = new PathStrengthData(
                Set.of(BeastStrength.WHITE_BOAR, BeastStrength.BLACK_BOAR), Map.of());
        assertEquals(48, StrengthCrit.getCritPoints(List.of(BeastStrengthFamily.BOAR), boars));

        PathStrengthData boar = new PathStrengthData(Set.of(BeastStrength.WHITE_BOAR), Map.of());
        int crit = StrengthCrit.getCritPoints(List.of(BeastStrengthFamily.BOAR), boar);
        assertEquals(24, crit, "a timed boar beside the permanent one joined the crit");
        assertEquals(2 * 24 + 24, StrengthService.getSettledPoints(24 + 24, crit, 1.0D));
    }

    @Test
    @DisplayName("human strength and a family no longer held add no crit points")
    void whatIsNotAPermanentBeastAddsNothing() {
        PathStrengthData human = new PathStrengthData(Set.of(), Map.of(HumanStrength.JUN, 2));
        assertEquals(0, StrengthCrit.getCritPoints(List.of(BeastStrengthFamily.BEAR), human));
        assertEquals(0, StrengthCrit.getCritPoints(List.of(BeastStrengthFamily.BEAR), PathStrengthData.DEFAULT));
    }

    @Test
    @DisplayName("the crit points are multiplied by the damage factor with the rest of the fist")
    void theCritTakesTheDamageFactor() {
        assertEquals(96, StrengthService.getSettledPoints(40, 40, 1.2D), "a second-tier Hardship bear crit");
        assertEquals(48, StrengthService.getSettledPoints(40, 0, 1.2D));
        assertEquals(240, StrengthService.getSettledPoints(40, 40, 3.0D), "a full charge on a crit bear");
    }
    //endregion
}
