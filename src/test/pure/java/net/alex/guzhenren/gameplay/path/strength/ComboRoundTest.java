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

class ComboRoundTest {

    private static final List<BeastStrengthFamily> THREE =
            List.of(BeastStrengthFamily.BOAR, BeastStrengthFamily.BEAR, BeastStrengthFamily.TIGER);

    @Test
    @DisplayName("under hardship each finished round adds one to the streak; a cut round or no hardship resets it")
    void theStreakCountsFinishedRounds() {
        assertEquals(1, ComboRound.getNextStreak(0, true, true));
        assertEquals(3, ComboRound.getNextStreak(2, true, true));
        assertEquals(0, ComboRound.getNextStreak(2, true, false), "a cut round kept the streak");
        assertEquals(0, ComboRound.getNextStreak(2, false, true), "the streak outlived hardship");
    }

    @Test
    @DisplayName("the finisher roll lands 5% of the time, 10% with the brown bear, 80/90/100% under the rank "
            + "three/four/five All-Out Effort whatever the beasts")
    void theChance() {
        float brownBear = BeastStrength.BROWN_BEAR.getChanceBonus();
        assertEquals(0.05F, ComboRound.getChance(StrengthService.NO_ALL_OUT, 0.0F));
        assertEquals(0.05F, ComboRound.getChance(1, 0.0F), "a tier under three raised the chance");
        assertEquals(0.80F, ComboRound.getChance(2, 0.0F));
        assertEquals(0.90F, ComboRound.getChance(3, 0.0F));
        assertEquals(1.0F, ComboRound.getChance(4, 0.0F));
        assertEquals(1.0F, ComboRound.getChance(9, 0.0F), "a tier past five broke the table");
        assertEquals(0.10F, ComboRound.getChance(StrengthService.NO_ALL_OUT, brownBear),
                "the brown bear did not double the passive roll");
        assertEquals(0.80F, ComboRound.getChance(2, brownBear), "the brown bear raised the All-Out chance");
    }

    @Test
    @DisplayName("a missed roll draws nothing, hardship or not; a landed one draws one family, or min(streak, "
            + "families) under hardship")
    void theDrawCount() {
        assertEquals(0, ComboRound.getDrawCount(false, false, 4, 3), "a missed roll drew");
        assertEquals(0, ComboRound.getDrawCount(true, false, 4, 3), "hardship drew on a missed roll");
        assertEquals(1, ComboRound.getDrawCount(false, true, 4, 3), "a landed roll without hardship did not draw one");
        assertEquals(1, ComboRound.getDrawCount(true, true, 1, 3));
        assertEquals(2, ComboRound.getDrawCount(true, true, 2, 3));
        assertEquals(3, ComboRound.getDrawCount(true, true, 3, 3));
        assertEquals(3, ComboRound.getDrawCount(true, true, 5, 3), "the draw passed the families held");
        assertEquals(0, ComboRound.getDrawCount(false, true, 1, 0));
        assertEquals(0, ComboRound.getDrawCount(true, true, 3, 0));
    }

    @Test
    @DisplayName("a draw never repeats a family and never draws more than there are")
    void aDrawNeverRepeats() {
        for (long seed = 0L; seed < 64L; seed++) {
            RandomSource random = RandomSource.create(seed);
            for (int count = 0; count <= 4; count++) {
                List<BeastStrengthFamily> picked = ComboRound.pickFamilies(THREE, count, random);
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
            firsts.add(ComboRound.pickFamilies(THREE, 1, RandomSource.create(seed)).getFirst());
        }
        assertEquals(Set.copyOf(THREE), firsts);
    }

    @Test
    @DisplayName("only permanent families are available, two boars as one, in declaration order")
    void availableFamilies() {
        PathStrengthData data = new PathStrengthData(
                Set.of(BeastStrength.TIGER, BeastStrength.WHITE_BOAR, BeastStrength.BLACK_BOAR), Map.of());
        assertEquals(List.of(BeastStrengthFamily.BOAR, BeastStrengthFamily.TIGER),
                ComboRound.getAvailableFamilies(data));
        assertEquals(List.of(), ComboRound.getAvailableFamilies(PathStrengthData.DEFAULT));
    }

    @Test
    @DisplayName("every drawn family counts its permanent points once more, so a released family settles twice")
    void everyDrawnFamilyIsReleased() {
        PathStrengthData data = new PathStrengthData(Set.of(BeastStrength.TIGER, BeastStrength.BEAR), Map.of());
        List<BeastStrengthFamily> both = List.of(BeastStrengthFamily.TIGER, BeastStrengthFamily.BEAR);
        assertEquals(40, ComboRound.getReleasedPoints(List.of(BeastStrengthFamily.BEAR), data));
        assertEquals(64, ComboRound.getReleasedPoints(List.of(BeastStrengthFamily.TIGER), data));
        assertEquals(104, ComboRound.getReleasedPoints(both, data), "a drawn family was left out of the release");
        assertEquals(0, ComboRound.getReleasedPoints(List.of(), data), "a missed roll released");

        assertEquals(104 + 40, StrengthService.getSettledPoints(104, 40, 1.0D), "the bear did not double");
        assertEquals(2 * 104, StrengthService.getSettledPoints(104, 104, 1.0D), "both families did not double");
        assertEquals(2, StrengthService.FULL_RELEASE_FACTOR);
    }

    @Test
    @DisplayName("the two boars release as one family, and a same-family timed beast is never doubled")
    void onlyPermanentPointsAreReleased() {
        PathStrengthData boars = new PathStrengthData(
                Set.of(BeastStrength.WHITE_BOAR, BeastStrength.BLACK_BOAR), Map.of());
        assertEquals(48, ComboRound.getReleasedPoints(List.of(BeastStrengthFamily.BOAR), boars));

        PathStrengthData boar = new PathStrengthData(Set.of(BeastStrength.WHITE_BOAR), Map.of());
        int released = ComboRound.getReleasedPoints(List.of(BeastStrengthFamily.BOAR), boar);
        assertEquals(24, released, "a timed boar beside the permanent one joined the release");
        assertEquals(2 * 24 + 24, StrengthService.getSettledPoints(24 + 24, released, 1.0D));
    }

    @Test
    @DisplayName("human strength and a family no longer held release nothing")
    void whatIsNotAPermanentBeastReleasesNothing() {
        PathStrengthData human = new PathStrengthData(Set.of(), Map.of(HumanStrength.JUN, 2));
        assertEquals(List.of(), ComboRound.getAvailableFamilies(human), "human strength offered a family");
        assertEquals(0, ComboRound.getReleasedPoints(List.of(BeastStrengthFamily.BEAR), human));
        assertEquals(0, ComboRound.getReleasedPoints(List.of(BeastStrengthFamily.BEAR), PathStrengthData.DEFAULT));
    }

    @Test
    @DisplayName("the release is multiplied by the whole-punch factor with the rest of the fist")
    void theReleaseTakesTheWholePunchFactor() {
        assertEquals(96, StrengthService.getSettledPoints(40, 40, 1.2D), "a second-tier hardship bear release");
        assertEquals(48, StrengthService.getSettledPoints(40, 0, 1.2D));
        assertEquals(240, StrengthService.getSettledPoints(40, 40, 3.0D), "a full charge on a released bear");
    }
}
