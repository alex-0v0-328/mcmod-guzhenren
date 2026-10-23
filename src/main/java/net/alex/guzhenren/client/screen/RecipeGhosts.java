package net.alex.guzhenren.client.screen;

import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.refinement.GuIngredient;
import net.alex.guzhenren.refinement.GuRecipe;
import net.alex.guzhenren.refinement.GuRecipeInput;
import net.alex.guzhenren.refinement.RefinementMenu;
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
 * item still paints over the ghosts. A cell that already holds anything gets no ghost.
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
        int[] missing = recipe.shortfall(grid);
        ItemStack[] shown = new ItemStack[missing.length];
        boolean any = false;
        for (int n = 0; n < missing.length; n++) {
            int slot = recipe.slots().get(n);
            if (missing[n] <= 0 || slot < 0 || slot >= grid.size()) continue;
            if (!grid.getItem(slot).isEmpty()) continue;

            ItemStack option = getOption(recipe.ingredients().get(n));
            if (option.isEmpty()) continue;
            shown[n] = option;
            any = true;
        }
        if (any) GuiBatch.draw(graphics, () -> drawGhosts(graphics, font, recipe, shown, missing));
    }

    static ItemStack getOption(GuIngredient need) {
        ItemStack[] options = need.ingredient().getItems();
        if (options.length == 0) return ItemStack.EMPTY;
        return options[(int) (Util.getMillis() / 1000L % options.length)];
    }

    static ItemStack getResult(GuRecipe recipe) {
        return recipe.results().isEmpty() ? ItemStack.EMPTY : recipe.results().get(0);
    }

    private static void drawGhosts(GuiGraphics graphics, Font font, GuRecipe recipe, ItemStack[] shown,
                                   int[] missing) {
        graphics.setColor(1.0F, 1.0F, 1.0F, ALPHA);
        for (int n = 0; n < shown.length; n++) {
            if (shown[n] == null) continue;
            int slot = recipe.slots().get(n);
            graphics.renderFakeItem(shown[n], getSlotX(slot), getSlotY(slot));
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        for (int n = 0; n < shown.length; n++) {
            if (shown[n] == null) continue;
            int x = getSlotX(recipe.slots().get(n));
            int y = getSlotY(recipe.slots().get(n));
            graphics.fill(RenderType.guiGhostRecipeOverlay(), x, y, x + Widgets.CELL, y + Widgets.CELL, OVERLAY);
        }
        for (int n = 0; n < shown.length; n++) {
            if (shown[n] == null) continue;
            int slot = recipe.slots().get(n);
            graphics.renderItemDecorations(font, shown[n], getSlotX(slot), getSlotY(slot),
                    missing[n] > 1 ? String.valueOf(missing[n]) : null);
        }
    }

    private static int getSlotX(int slot) {
        return slot < GuRecipe.RING_SIZE ? RefinementMenu.ringX(slot)
                : RefinementMenu.coreX((slot - GuRecipe.RING_SIZE) % GuRecipe.CORE_COLS);
    }

    private static int getSlotY(int slot) {
        return slot < GuRecipe.RING_SIZE ? RefinementMenu.ringY(slot)
                : RefinementMenu.coreY((slot - GuRecipe.RING_SIZE) / GuRecipe.CORE_COLS);
    }
}
