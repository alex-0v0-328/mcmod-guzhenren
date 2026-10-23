package net.alex.guzhenren.refinement;

import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One Gu recipe [蛊方]: what it eats, what it yields, and the windows the ritual runs through.
 *
 * <p>The ingredients and their cell positions are parallel lists (SHAPED and exact -- no mirroring, no
 * rotation); the window list's length IS the stage count. The data and the matching logic live here; the
 * recipe manager's side -- recipe type, serializer, the sorted listing -- lives in {@link RefinementRecipe}.
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinementMenu
 * @since 1.0.0
 */

public record GuRecipe(List<GuIngredient> ingredients, List<Integer> slots, List<ItemStack> results,
                       long essencePerSecond, long soulPerSecond, List<Integer> windows, int baseSuccess) {

    public static final int WINDOW_TICKS = 5 * Ticks.SECOND;
    public static final int GAP_TICKS = 2 * Ticks.SECOND;
    public static final int GRID_COLS = 5;
    public static final int GRID_ROWS = 5;
    public static final int RING_SIZE = 12;
    public static final int CORE_COLS = 3;
    public static final int CORE_ROWS = 3;
    public static final int CORE_SIZE = CORE_COLS * CORE_ROWS;
    public static final int INPUT_SIZE = RING_SIZE + CORE_SIZE;
    private static final int[] RING_COLS = { 1, 2, 3, 0, 4, 0, 4, 0, 4, 1, 2, 3 };
    private static final int[] RING_ROWS = { 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4 };

    public GuRecipe {
        if (essencePerSecond < 0L) throw new IllegalArgumentException("essencePerSecond must be non-negative");
        if (soulPerSecond < 0L) throw new IllegalArgumentException("soulPerSecond must be non-negative");
        if (windows.stream().anyMatch(window -> window < 0)) {
            throw new IllegalArgumentException("windows must be non-negative");
        }
        if (baseSuccess < 0 || baseSuccess > 100) {
            throw new IllegalArgumentException("baseSuccess must be 0 to 100");
        }
        if (ingredients.size() != slots.size()) {
            throw new IllegalArgumentException("every ingredient owns exactly one cell of the grid");
        }
        try {
            long totalSeconds = exactTotalTicks(windows.size()) / Ticks.SECOND;
            if (Math.addExact(Math.multiplyExact(essencePerSecond, totalSeconds),
                    Math.multiplyExact(soulPerSecond, totalSeconds)) < 0L) {
                throw new ArithmeticException("combined cost overflow");
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("recipe runtime exceeds supported range", e);
        }
    }

    //region the ritual's own clock -- the window list IS the stage count
    public int windowCount() { return windows.size(); }

    public int stonesFor(int window) { return windows.get(window); }

    public int totalSeconds() { return totalTicks() / Ticks.SECOND; }

    public long essenceToFinish() { return Math.multiplyExact(essencePerSecond, totalSeconds()); }

    public int totalTicks() {
        return exactTotalTicks(windowCount());
    }

    private static int exactTotalTicks(int windows) {
        return Math.addExact(Math.multiplyExact(windows, WINDOW_TICKS),
                Math.multiplyExact(Math.max(0, windows - 1), GAP_TICKS));
    }
    //endregion

    //region matching -- SHAPED and EXACT: every cell holds what its own ingredient asks for, or nothing
    public int @Nullable [] claim(GuRecipeInput input) {
        int[] taken = new int[input.size()];

        for (int n = 0; n < ingredients.size(); n++) {
            int slot = slots.get(n);
            if (slot < 0 || slot >= input.size()) return null;

            GuIngredient need = ingredients.get(n);
            if (!need.test(input.getItem(slot))) return null;

            taken[slot] += need.count();
        }
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).getCount() != taken[i]) return null;
        }
        return taken;
    }

    public int[] shortfall(GuRecipeInput input) {
        int[] missing = new int[ingredients.size()];

        for (int n = 0; n < ingredients.size(); n++) {
            GuIngredient need = ingredients.get(n);
            ItemStack held = held(input, slots.get(n));
            int have = need.test(held) ? held.getCount() : 0;
            missing[n] = Math.max(0, need.count() - have);
        }
        return missing;
    }

    private static ItemStack held(GuRecipeInput input, int slot) {
        return slot < 0 || slot >= input.size() ? ItemStack.EMPTY : input.getItem(slot);
    }
    //endregion

    //region the Gu count among the results -- the sole-Gu inheritance rule reads it
    //  TODO(炼法): rebuild the refinement-mode derivation (two axes, never one enum) once modes gain behavior.
    public int guResultCount() {
        int n = 0;
        for (ItemStack stack : results) {
            if (stack.getItem() instanceof MortalGuItem) n += stack.getCount();
        }
        return n;
    }
    //endregion

    public ItemStack first() { return results.isEmpty() ? ItemStack.EMPTY : results.get(0); }

    //region the grid -- a 5x5 with its four corners cut away: outer ring [外圈] 12 around inner ring [内圈] 9
    public static int ringCol(int index) { return RING_COLS[index]; }

    public static int ringRow(int index) { return RING_ROWS[index]; }

    public static int slotAt(int row, int col) {
        for (int i = 0; i < RING_SIZE; i++) {
            if (RING_ROWS[i] == row && RING_COLS[i] == col) return i;
        }
        if (row < 1 || row > CORE_ROWS || col < 1 || col > CORE_COLS) return -1;
        return RING_SIZE + (row - 1) * CORE_COLS + (col - 1);
    }
    //endregion
}
