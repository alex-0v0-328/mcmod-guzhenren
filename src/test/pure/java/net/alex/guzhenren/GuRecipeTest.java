package net.alex.guzhenren;

import java.util.AbstractList;
import java.util.List;
import java.util.stream.Stream;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuRecipeTest {

    @Test
    @DisplayName("recipe runtime costs and windows cannot be negative")
    void negativeRuntimeValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> recipe(-1L, 0L, List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> recipe(0L, -1L, List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> recipe(0L, 0L, List.of(-1), 0));
    }

    @Test
    @DisplayName("recipe success must be a percentage")
    void successIsBounded() {
        assertThrows(IllegalArgumentException.class, () -> recipe(0L, 0L, List.of(), -1));
        assertThrows(IllegalArgumentException.class, () -> recipe(0L, 0L, List.of(), 101));
    }

    @Test
    @DisplayName("zero costs, windows and success remain valid")
    void zeroRemainsValid() {
        assertDoesNotThrow(() -> recipe(0L, 0L, List.of(0), 0));
    }

    @Test
    @DisplayName("recipe rejects runtime cost multiplication overflow")
    void runtimeCostOverflowIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> recipe(Long.MAX_VALUE, 0L, List.of(1), 0));
        assertThrows(IllegalArgumentException.class, () -> recipe(0L, Long.MAX_VALUE, List.of(1), 0));
    }

    @Test
    @DisplayName("recipe rejects a window count whose duration overflows")
    void windowDurationOverflowIsRejected() {
        List<Integer> tooManyWindows = new AbstractList<>() {
            @Override
            public Integer get(int index) { return 0; }

            @Override
            public int size() { return Integer.MAX_VALUE; }

            @Override
            public @NotNull Stream<Integer> stream() { return Stream.empty(); }
        };

        assertThrows(IllegalArgumentException.class, () -> recipe(0L, 0L, tooManyWindows, 0));
    }

    private static GuRecipe recipe(long essence, long soul, List<Integer> windows, int success) {
        return new GuRecipe(List.of(), List.of(), List.of(), essence, soul, windows, success);
    }
}
