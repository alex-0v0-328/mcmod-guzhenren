package net.alex.guzhenren.client.screen;

import java.util.List;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.alex.guzhenren.refinement.recipe.GuIngredient;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.alex.guzhenren.refinement.recipe.GuRecipeInput;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;

/**
 * The cells a picked Gu Recipe [蛊方] still wants, drawn into the refinement grid as faded items under the vanilla
 * ghost-recipe overlay with the missing count; plus the two recipe readings the refinement screen and the
 * {@link RecipePicker} share: {@link #getResult}, the first result, and {@link #getOption}, one of an ingredient's
 * items, cycling once a second.
 *
 * <p>⚠ {@link #draw} runs from the screen's {@code renderLabels}: coordinates are panel-relative, and the carried
 * item still paints over the ghosts. A cell that already holds anything gets no ghost; an ingredient's ghost takes
 * the first empty cell of its group.
 *
 * <p>The ghosts draw in one {@link GuiBatch}, in three passes -- every faded item, every overlay, every count --
 * which keeps each cell's own order; the grid's cells sit 22 apart and never overlap, so only that order shows.
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinementScreen
 * @since 1.0.0
 */

final class RecipeGhosts {

    private RecipeGhosts() {}

    private static final int OVERLAY = 0x1AFFFFFF;
    private static final float ALPHA = 0.35F;

    static void draw(GuiGraphics graphics, Font font, GuRecipe recipe, GuRecipeInput grid) {
        int[] missing = recipe.getShortfall(grid);
        ItemStack[] shown = new ItemStack[missing.length];
        int[] at = new int[missing.length];
        boolean any = false;
        for (int n = 0; n < missing.length; n++) {
            if (missing[n] <= 0) continue;
            int slot = getFreeCell(recipe.slots().get(n), grid);
            if (slot < 0) continue;

            ItemStack option = getOption(recipe.ingredients().get(n));
            if (option.isEmpty()) continue;
            shown[n] = option;
            at[n] = slot;
            any = true;
        }
        if (any) GuiBatch.draw(graphics, () -> drawGhosts(graphics, font, shown, at, missing));
    }

    private static int getFreeCell(List<Integer> cells, GuRecipeInput grid) {
        for (int slot : cells) {
            if (slot >= 0 && slot < grid.size() && grid.getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    static ItemStack getOption(GuIngredient need) {
        ItemStack[] options = need.ingredient().getItems();
        if (options.length == 0) return ItemStack.EMPTY;
        return options[(int) (Util.getMillis() / 1000L % options.length)];
    }

    static ItemStack getResult(GuRecipe recipe) {
        return recipe.results().isEmpty() ? ItemStack.EMPTY : recipe.results().get(0);
    }

    private static void drawGhosts(GuiGraphics graphics, Font font, ItemStack[] shown, int[] at, int[] missing) {
        graphics.setColor(1.0F, 1.0F, 1.0F, ALPHA);
        for (int n = 0; n < shown.length; n++) {
            if (shown[n] == null) continue;
            graphics.renderFakeItem(shown[n], getSlotX(at[n]), getSlotY(at[n]));
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        for (int n = 0; n < shown.length; n++) {
            if (shown[n] == null) continue;
            int x = getSlotX(at[n]);
            int y = getSlotY(at[n]);
            graphics.fill(RenderType.guiGhostRecipeOverlay(), x, y, x + Widgets.CELL, y + Widgets.CELL, OVERLAY);
        }
        for (int n = 0; n < shown.length; n++) {
            if (shown[n] == null) continue;
            graphics.renderItemDecorations(font, shown[n], getSlotX(at[n]), getSlotY(at[n]),
                    missing[n] > 1 ? String.valueOf(missing[n]) : null);
        }
    }

    private static int getSlotX(int slot) {
        return slot < GuRecipe.RING_SIZE ? RefinementMenu.getRingX(slot)
                : RefinementMenu.getCoreX((slot - GuRecipe.RING_SIZE) % GuRecipe.CORE_COLS);
    }

    private static int getSlotY(int slot) {
        return slot < GuRecipe.RING_SIZE ? RefinementMenu.getRingY(slot)
                : RefinementMenu.getCoreY((slot - GuRecipe.RING_SIZE) / GuRecipe.CORE_COLS);
    }
}
