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
 * 已选蛊方在炼蛊网格里仍缺的格子，画成虚影。
 * <ul>
 *   <li>虚影：半透明物品，上盖原版虚影配方遮罩，标缺少数量；已有物品的格子不画，材料虚影落在其组内第一个空格。</li>
 *   <li>{@link #getResult} 取首个产物，{@link #getOption} 每秒轮换材料候选；炼蛊界面与 {@link RecipePicker} 共用。</li>
 *   <li>⚠ {@link #draw} 从 {@code renderLabels} 调用：坐标相对面板，手持物品仍画在虚影上。</li>
 *   <li>一个 {@link GuiBatch} 分三遍画（半透明物品、遮罩、数量），每格层序不变；格距 22 互不重叠，只见格内层序。</li>
 * </ul>
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
        return recipe.results().isEmpty() ? ItemStack.EMPTY : recipe.results().getFirst();
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
