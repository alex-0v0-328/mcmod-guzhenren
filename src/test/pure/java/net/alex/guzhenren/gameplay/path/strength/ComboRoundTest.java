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
    @DisplayName("hardship draws min(streak, families); otherwise a landed roll draws one, else none")
    void theDrawCount() {
        assertEquals(1, ComboRound.getDrawCount(true, false, 1, 3));
        assertEquals(2, ComboRound.getDrawCount(true, false, 2, 3));
        assertEquals(3, ComboRound.getDrawCount(true, false, 5, 3));
        assertEquals(2, ComboRound.getDrawCount(true, true, 2, 3));
        assertEquals(1, ComboRound.getDrawCount(false, true, 4, 3));
        assertEquals(0, ComboRound.getDrawCount(false, false, 4, 3));
        assertEquals(0, ComboRound.getDrawCount(false, true, 1, 0));
        assertEquals(0, ComboRound.getDrawCount(true, false, 3, 0));
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
    @DisplayName("a rolled release triples the family's permanent points under the capacity; a plain draw adds none")
    void aRolledRelease() {
        PathStrengthData bear = new PathStrengthData(Set.of(BeastStrength.BEAR), Map.of());
        List<BeastStrengthFamily> drawn = List.of(BeastStrengthFamily.BEAR);
        int extra = ComboRound.getReleasedExtra(56, 40, drawn, bear, false, BeastStrengthFamily.BEAR);
        assertEquals(StrengthService.getReleasedExtra(56, 40, 40), extra);
        assertEquals(56 - 40, extra, "the release passed the capacity");
        assertEquals(0, ComboRound.getReleasedExtra(56, 40, drawn, bear, false, null), "a shown family added");
    }

    @Test
    @DisplayName("the fully released families: every drawn one under All-Out, else the rolled one while still drawn")
    void theFullyReleasedFamilies() {
        List<BeastStrengthFamily> drawn = List.of(BeastStrengthFamily.TIGER, BeastStrengthFamily.HORSE);
        assertEquals(drawn, ComboRound.getFullyReleased(drawn, true, null));
        assertEquals(List.of(BeastStrengthFamily.HORSE),
                ComboRound.getFullyReleased(drawn, false, BeastStrengthFamily.HORSE));
        assertEquals(List.of(), ComboRound.getFullyReleased(drawn, false, null), "a plain draw released");
        assertEquals(List.of(), ComboRound.getFullyReleased(List.of(BeastStrengthFamily.TIGER), false,
                BeastStrengthFamily.HORSE), "a family that left the draw stayed released");
    }

    @Test
    @DisplayName("All-Out releases every drawn family as one sum, a landed roll only the first")
    void allOutReleasesEveryDrawnFamily() {
        PathStrengthData data = new PathStrengthData(Set.of(BeastStrength.TIGER, BeastStrength.BEAR), Map.of());
        List<BeastStrengthFamily> both = List.of(BeastStrengthFamily.TIGER, BeastStrengthFamily.BEAR);
        assertEquals(2 * 104, ComboRound.getReleasedExtra(400, 104, both, data, true, null));
        assertEquals(2 * 64, ComboRound.getReleasedExtra(400, 104, both, data, false, BeastStrengthFamily.TIGER));
        assertEquals(224 - 104, ComboRound.getReleasedExtra(224, 104, both, data, true, null),
                "the release passed the capacity");
        assertEquals(0, ComboRound.getReleasedExtra(400, 104, both, data, false, null));
    }

    @Test
    @DisplayName("a same-family timed beast is never tripled")
    void aTimedBeastIsNeverTripled() {
        PathStrengthData boar = new PathStrengthData(Set.of(BeastStrength.WHITE_BOAR), Map.of());
        assertEquals(2 * 24, ComboRound.getReleasedExtra(400, 24 + 24, List.of(BeastStrengthFamily.BOAR), boar,
                false, BeastStrengthFamily.BOAR));
    }

    @Test
    @DisplayName("a family no longer held releases nothing")
    void aFamilyNoLongerHeldReleasesNothing() {
        assertEquals(0, ComboRound.getReleasedExtra(400, 40, List.of(BeastStrengthFamily.BEAR),
                PathStrengthData.DEFAULT, true, BeastStrengthFamily.BEAR));
    }
}
