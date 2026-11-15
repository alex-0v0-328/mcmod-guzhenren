package net.alex.guzhenren;

import java.util.Random;
import net.alex.guzhenren.core.WeightedPick;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedPickTest {

    private enum Grade {

        A(3), B(1), C(0);
        final int weight;

        Grade(int weight) { this.weight = weight; }
    }

    @Test
    void zeroWeightNeverPicked() {
        for (int i = 0; i < 1000; i++) {
            Grade g = WeightedPick.pick(Grade.values(), new Random(i), v -> true, v -> v.weight);
            assertTrue(g == Grade.A || g == Grade.B);
        }
    }

    @Test
    void filterExcludesValues() {
        for (int i = 0; i < 1000; i++) {
            Grade g = WeightedPick.pick(Grade.values(), new Random(i), v -> v != Grade.A, v -> v.weight);
            assertEquals(Grade.B, g);
        }
    }

    @Test
    void sameSeedSameSequence() {
        Grade[] a = new Grade[8];
        Grade[] b = new Grade[8];
        for (int i = 0; i < 8; i++) {
            a[i] = WeightedPick.pick(Grade.values(), new Random(42), v -> true, v -> v.weight);
            b[i] = WeightedPick.pick(Grade.values(), new Random(42), v -> true, v -> v.weight);
        }
        assertArrayEquals(a, b);
    }

    @Test
    void zeroTotalThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> WeightedPick.pick(Grade.values(), new Random(1), v -> v == Grade.C, v -> v.weight));
    }
}
