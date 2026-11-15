package net.alex.guzhenren.gameplay.path.strength;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrengthRoundTest {

    private static final List<BeastStrengthFamily> THREE =
            List.of(BeastStrengthFamily.BOAR, BeastStrengthFamily.BEAR, BeastStrengthFamily.TIGER);

    @Test
    @DisplayName("a round lasts twenty ticks from its opening")
    void aRoundLastsTwentyTicks() {
        assertFalse(StrengthRound.isOver(100L, 100L + StrengthRound.ROUND_TICKS - 1));
        assertTrue(StrengthRound.isOver(100L, 100L + StrengthRound.ROUND_TICKS));
    }

    @Test
    @DisplayName("the streak starts at one, chains within twenty ticks of the last round's end and resets past it")
    void theStreakChainsWithinTheWindow() {
        assertEquals(1, StrengthRound.getNextStreak(0, 0L, 20L));
        assertEquals(2, StrengthRound.getNextStreak(1, 0L, 20L));
        assertEquals(3, StrengthRound.getNextStreak(2, 0L, 20L + StrengthRound.STREAK_WINDOW_TICKS));
        assertEquals(1, StrengthRound.getNextStreak(2, 0L, 21L + StrengthRound.STREAK_WINDOW_TICKS));
    }

    @Test
    @DisplayName("ranged punches keep their own spacing, more than one to a round")
    void aRangedPunchKeepsItsOwnSpacing() {
        assertTrue(StrengthRound.canRange(null, 0L), "the first ranged punch is free");
        assertFalse(StrengthRound.canRange(30L, 30L), "a second in the same tick");
        long next = 30L + StrengthRound.RANGE_INTERVAL_TICKS;
        assertFalse(StrengthRound.canRange(30L, next - 1), "closer than the spacing");
        assertTrue(StrengthRound.canRange(30L, next), "the spacing later");
        assertTrue(StrengthRound.RANGE_INTERVAL_TICKS < StrengthRound.ROUND_TICKS, "a round takes only one");
    }

    @Test
    @DisplayName("the opening roll lands 5% of the time, 10% with the brown bear, 80/90/100% under the rank "
            + "three/four/five All-Out Effort whatever the beasts")
    void theChance() {
        float brownBear = BeastStrength.BROWN_BEAR.getChanceBonus();
        assertEquals(0.05F, StrengthRound.getChance(StrengthService.NO_ALL_OUT, 0.0F));
        assertEquals(0.05F, StrengthRound.getChance(1, 0.0F), "a tier under three raised the chance");
        assertEquals(0.80F, StrengthRound.getChance(2, 0.0F));
        assertEquals(0.90F, StrengthRound.getChance(3, 0.0F));
        assertEquals(1.0F, StrengthRound.getChance(4, 0.0F));
        assertEquals(1.0F, StrengthRound.getChance(9, 0.0F), "a tier past five broke the table");
        assertEquals(0.10F, StrengthRound.getChance(StrengthService.NO_ALL_OUT, brownBear),
                "the brown bear did not double the passive roll");
        assertEquals(0.80F, StrengthRound.getChance(2, brownBear), "the brown bear raised the All-Out chance");
    }

    @Test
    @DisplayName("hardship draws min(streak, families); otherwise a landed roll draws one, else none")
    void theDrawCount() {
        assertEquals(1, StrengthRound.getDrawCount(true, false, 1, 3));
        assertEquals(2, StrengthRound.getDrawCount(true, false, 2, 3));
        assertEquals(3, StrengthRound.getDrawCount(true, false, 5, 3));
        assertEquals(2, StrengthRound.getDrawCount(true, true, 2, 3));
        assertEquals(1, StrengthRound.getDrawCount(false, true, 4, 3));
        assertEquals(0, StrengthRound.getDrawCount(false, false, 4, 3));
        assertEquals(0, StrengthRound.getDrawCount(false, true, 1, 0));
        assertEquals(0, StrengthRound.getDrawCount(true, false, 3, 0));
    }

    @Test
    @DisplayName("a draw never repeats a family and never draws more than there are")
    void aDrawNeverRepeats() {
        for (long seed = 0L; seed < 64L; seed++) {
            RandomSource random = RandomSource.create(seed);
            for (int count = 0; count <= 4; count++) {
                List<BeastStrengthFamily> picked = StrengthRound.pickFamilies(THREE, count, random);
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
            firsts.add(StrengthRound.pickFamilies(THREE, 1, RandomSource.create(seed)).getFirst());
        }
        assertEquals(Set.copyOf(THREE), firsts);
    }

    @Test
    @DisplayName("only permanent families are available, two boars as one, in declaration order")
    void availableFamilies() {
        PathStrengthData data = new PathStrengthData(
                Set.of(BeastStrength.TIGER, BeastStrength.WHITE_BOAR, BeastStrength.BLACK_BOAR), Map.of());
        assertEquals(List.of(BeastStrengthFamily.BOAR, BeastStrengthFamily.TIGER),
                StrengthRound.getAvailableFamilies(data));
        assertEquals(List.of(), StrengthRound.getAvailableFamilies(PathStrengthData.DEFAULT));
    }

    @Test
    @DisplayName("a rolled release triples the family's permanent points under the capacity; a plain draw adds none")
    void aRolledRelease() {
        PathStrengthData bear = new PathStrengthData(Set.of(BeastStrength.BEAR), Map.of());
        List<BeastStrengthFamily> drawn = List.of(BeastStrengthFamily.BEAR);
        int extra = StrengthRound.getReleasedExtra(56, 40, drawn, bear, false, BeastStrengthFamily.BEAR);
        assertEquals(StrengthService.getReleasedExtra(56, 40, 40), extra);
        assertEquals(56 - 40, extra, "the release passed the capacity");
        assertEquals(0, StrengthRound.getReleasedExtra(56, 40, drawn, bear, false, null), "a shown family added");
    }

    @Test
    @DisplayName("the fully released families: every drawn one under All-Out, else the rolled one while still drawn")
    void theFullyReleasedFamilies() {
        List<BeastStrengthFamily> drawn = List.of(BeastStrengthFamily.TIGER, BeastStrengthFamily.HORSE);
        assertEquals(drawn, StrengthRound.getFullyReleased(drawn, true, null));
        assertEquals(List.of(BeastStrengthFamily.HORSE),
                StrengthRound.getFullyReleased(drawn, false, BeastStrengthFamily.HORSE));
        assertEquals(List.of(), StrengthRound.getFullyReleased(drawn, false, null), "a plain draw released");
        assertEquals(List.of(), StrengthRound.getFullyReleased(List.of(BeastStrengthFamily.TIGER), false,
                BeastStrengthFamily.HORSE), "a family that left the draw stayed released");
    }

    @Test
    @DisplayName("All-Out releases every drawn family as one sum, a landed roll only the first")
    void allOutReleasesEveryDrawnFamily() {
        PathStrengthData data = new PathStrengthData(Set.of(BeastStrength.TIGER, BeastStrength.BEAR), Map.of());
        List<BeastStrengthFamily> both = List.of(BeastStrengthFamily.TIGER, BeastStrengthFamily.BEAR);
        assertEquals(2 * 104, StrengthRound.getReleasedExtra(400, 104, both, data, true, null));
        assertEquals(2 * 64, StrengthRound.getReleasedExtra(400, 104, both, data, false, BeastStrengthFamily.TIGER));
        assertEquals(224 - 104, StrengthRound.getReleasedExtra(224, 104, both, data, true, null),
                "the release passed the capacity");
        assertEquals(0, StrengthRound.getReleasedExtra(400, 104, both, data, false, null));
    }

    @Test
    @DisplayName("a same-family timed beast is never tripled")
    void aTimedBeastIsNeverTripled() {
        PathStrengthData boar = new PathStrengthData(Set.of(BeastStrength.WHITE_BOAR), Map.of());
        assertEquals(2 * 24, StrengthRound.getReleasedExtra(400, 24 + 24, List.of(BeastStrengthFamily.BOAR), boar,
                false, BeastStrengthFamily.BOAR));
    }

    @Test
    @DisplayName("a family no longer held releases nothing")
    void aFamilyNoLongerHeldReleasesNothing() {
        assertEquals(0, StrengthRound.getReleasedExtra(400, 40, List.of(BeastStrengthFamily.BEAR),
                PathStrengthData.DEFAULT, true, BeastStrengthFamily.BEAR));
    }
}
