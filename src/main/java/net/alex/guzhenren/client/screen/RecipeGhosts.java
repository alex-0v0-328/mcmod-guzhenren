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
 * 炼蛊网格中已选蛊方的缺料虚影提示绘制器。
 * <ul>
 *   <li>虚影由半透明物品图标、原版虚影配方遮罩与缺少数量数字叠绘而成；已有物品的槽位不画虚影，材料组缺口画在组内首个
 *       空槽位上。</li>
 *   <li>{@link #getResult} 取蛊方首个产物，{@link #getOption} 按秒轮换展示材料候选物品；本逻辑由炼蛊界面与
 *       {@link RecipePicker} 共用。</li>
 *   <li>⚠ {@link #draw} 在 {@code renderLabels} 中以相对面板坐标调用，确保鼠标抓起的手持物品依然绘制在虚影之上。</li>
 *   <li>所有槽位共用一个 {@link GuiBatch} 并分三遍提交（半透明物品、遮罩、数量数字）：由于网格间距 22 像素且各格互
 *       不重叠，格内层序完全保持正确。</li>
 * </ul>
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
