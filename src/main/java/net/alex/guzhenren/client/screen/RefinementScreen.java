package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.refinement.GuRecipe;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The refinement [炼蛊] screen: the ring grid, the phase bar, and the recipe picker.
 *
 * <p>Extends {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen} for
 * {@link RefinementMenu}. Draws the 5×5 grid (corners cut), the craft
 * button in three states, the phase bar, the stone slot, and the recipe picker modal. Ghosts for
 * missing ingredients ({@link RecipeGhosts}) and the pool readings ({@link PoolBars}) are drawn from
 * {@code renderLabels} so the carried item paints last; the buttons and cells are the package's {@link Widgets}, the
 * whole background in one {@link GuiBatch} and the three buttons in another, every fill and outline before any
 * label (each label fits inside its outline). The recipe table the screen and its picker read is {@link KnownRecipes},
 * sorted once rather than per frame. The picker modal translates Z to 500 to stay above slot items. In
 * {@link #mouseClicked(double, double, int)}, the open picker swallows every click; only the left button acts on it.
 *
 * <p>⚠ Every cell position comes from the menu's own helpers, so a drawn cell and the real slot
 * cannot drift apart. Two different pitches are in play; do not reuse the inventory's for the grid.
 *
 * <p>Just past 300 lines: the two buttons, the phase bar, the status line and the picker's input routing all read
 * the same ritual state off the menu.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class RefinementScreen extends AbstractContainerScreen<RefinementMenu> {

    private static final int SLOT = RefinementMenu.SLOT;
    private static final int GRID_SLOT = RefinementMenu.GRID_SLOT;
    private static final int CRAFT_X = 140;
    private static final int CRAFT_Y = 76;
    private static final int CRAFT_W = 52;
    private static final int CRAFT_H = 20;
    private static final int BAR_X = 140;
    private static final int BAR_Y = 102;
    private static final int BAR_W = 52;
    private static final int BAR_H = 6;
    private static final int RECIPE_X = 140;
    private static final int RECIPE_Y = 114;
    private static final int RECIPE_W = 52;
    private static final int RECIPE_H = 20;
    private static final int LEGEND_Y = 142;
    private static final int CORE_FILL = 0x4DFFFFFF;
    private static final int BAR_WINDOW = 0xFF81C784;
    private static final int BAR_GAP = 0x6681C784;
    private static final String CRAFT_KEY = "guzhenren.menu.refinement.craft";
    private static final String STOP_KEY = "guzhenren.menu.refinement.stop";
    private static final String LEGEND_KEY = "guzhenren.menu.refinement.rings";
    private static final String WINDOW_KEY = "guzhenren.menu.refinement.window";
    private static final String GAP_KEY = "guzhenren.menu.refinement.gap";
    private static final String RECIPE_KEY = "guzhenren.menu.refinement.recipes";
    private static final String SELECTED_KEY = "guzhenren.menu.refinement.selected";
    private static final String EXTRA_KEY = "guzhenren.menu.refinement.extra";
    private static final int TITLE_X_WITH_BACK = 32;
    private static final int MARGIN = 18;
    private static final int HEADER_H = 20;
    private final RecipePicker picker = new RecipePicker();

    public RefinementScreen(RefinementMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 266;
        this.imageHeight = 321;
        this.inventoryLabelY = RefinementMenu.INVENTORY_Y - 11;
        this.inventoryLabelX = RefinementMenu.INVENTORY_X;
        this.titleLabelX = TITLE_X_WITH_BACK;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        GuiBatch.draw(graphics, () -> {
            Widgets.drawPanel(graphics, x, y, imageWidth, imageHeight, ModPalette.PANEL_FILL);
            graphics.fill(x + MARGIN, y + HEADER_H, x + imageWidth - MARGIN, y + HEADER_H + 1, ModPalette.REFINEMENT);

            drawInput(graphics, x, y);
            Widgets.drawCell(graphics, x + RefinementMenu.STONE_X, y + RefinementMenu.STONE_Y, ModPalette.SLOT_FILL);
            Widgets.drawCells(graphics, x + RefinementMenu.OUTPUT_X, y + RefinementMenu.OUTPUT_Y,
                    RefinementMenu.OUTPUT_COLS, RefinementMenu.OUTPUT_ROWS, GRID_SLOT, ModPalette.SLOT_FILL);
            Widgets.drawCells(graphics, x + RefinementMenu.INVENTORY_X, y + RefinementMenu.INVENTORY_Y,
                    RefinementMenu.INVENTORY_COLS, 3, SLOT, ModPalette.SLOT_FILL);
            Widgets.drawCells(graphics, x + RefinementMenu.INVENTORY_X, y + RefinementMenu.HOTBAR_Y,
                    RefinementMenu.INVENTORY_COLS, 1, SLOT, ModPalette.SLOT_FILL);
            drawBar(graphics, x, y);
        });
    }

    //region the two rings -- the 内圈 is marked by a brighter cell and an accent frame around the block
    private void drawInput(GuiGraphics graphics, int x, int y) {
        for (int i = 0; i < GuRecipe.RING_SIZE; i++) {
            Widgets.drawCell(graphics, x + RefinementMenu.ringX(i), y + RefinementMenu.ringY(i), ModPalette.SLOT_FILL);
        }
        Widgets.drawCells(graphics, x + RefinementMenu.coreX(0), y + RefinementMenu.coreY(0),
                GuRecipe.CORE_COLS, GuRecipe.CORE_ROWS, GRID_SLOT, CORE_FILL);
        graphics.renderOutline(x + RefinementMenu.coreX(0) - 3, y + RefinementMenu.coreY(0) - 3,
                (GuRecipe.CORE_COLS - 1) * GRID_SLOT + Widgets.CELL + 6,
                (GuRecipe.CORE_ROWS - 1) * GRID_SLOT + Widgets.CELL + 6, ModPalette.REFINEMENT);
    }
    //endregion

    //region the phase bar -- it empties over the 5s window, then over the 2s gap
    private void drawBar(GuiGraphics graphics, int x, int y) {
        int bx = x + BAR_X;
        int by = y + BAR_Y;
        graphics.fill(bx, by, bx + BAR_W, by + BAR_H, PoolBars.TRACK);
        if (!menu.running()) return;

        int span = menu.inWindow() ? GuRecipe.WINDOW_TICKS : GuRecipe.GAP_TICKS;
        int filled = BAR_W * menu.phaseLeft() / span;
        graphics.fill(bx, by, bx + filled, by + BAR_H, barColour());
    }

    private int barColour() {
        if (!menu.inWindow()) return BAR_GAP;
        return menu.stonesIn() < menu.stonesNeeded() ? ModPalette.SHORT : BAR_WINDOW;
    }
    //endregion

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, ModPalette.REFINEMENT, false);
        graphics.drawString(font, statusLine(), RefinementMenu.INPUT_X, LEGEND_Y,
                statusColour(), false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ModPalette.TEXT, false);
        renderGhosts(graphics);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) PoolBars.draw(graphics, font, player);
    }

    //region the status line -- the ring legend until a 蛊方 is picked, then what that pick still wants
    private Component statusLine() {
        if (menu.running()) {
            int shown = menu.stage() + 1;
            int seconds = (menu.phaseLeft() + Ticks.SECOND - 1) / Ticks.SECOND;
            return menu.inWindow()
                    ? Component.translatable(WINDOW_KEY, shown, menu.stages(), seconds,
                    menu.stonesIn(), menu.stonesNeeded())
                    : Component.translatable(GAP_KEY, shown, menu.stages());
        }
        GuRecipe recipe = selectedRecipe();
        if (recipe == null) return Component.translatable(LEGEND_KEY);
        if (crowded(recipe)) return Component.translatable(EXTRA_KEY);
        return Component.translatable(SELECTED_KEY, RecipeGhosts.getResult(recipe).getHoverName());
    }

    private int statusColour() {
        if (menu.running()) return ModPalette.TEXT;

        GuRecipe recipe = selectedRecipe();
        if (recipe == null) return ModPalette.LEGEND_TEXT;
        return crowded(recipe) ? ModPalette.SHORT : ModPalette.TEXT;
    }

    private boolean crowded(GuRecipe recipe) {
        if (menu.ready()) return false;

        for (int missing : recipe.shortfall(menu.grid())) {
            if (missing > 0) return false;
        }
        return true;
    }
    //endregion

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        GuiBatch.draw(graphics, () -> {
            fillCraft(graphics, mouseX, mouseY);
            fillRecipe(graphics, mouseX, mouseY);
            Widgets.fillBack(graphics, backX(), backY(), mouseX, mouseY);
            drawCraftLabel(graphics);
            drawRecipeLabel(graphics);
            Widgets.drawBackLabel(graphics, font, backX(), backY());
        });
        if (picker.isOpen()) {
            picker.render(graphics, font, KnownRecipes.get(), menu.selected(), leftPos, topPos, imageWidth,
                    imageHeight, mouseX, mouseY);
            return;
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderGhosts(GuiGraphics graphics) {
        GuRecipe recipe = selectedRecipe();
        if (recipe != null && !menu.running()) RecipeGhosts.draw(graphics, font, recipe, menu.grid());
    }

    //region the 炼制 button -- three states off the ritual, and 停止 while it runs
    private void fillCraft(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = craftX();
        int y = craftY();
        boolean stopping = menu.running();
        boolean live = craftLive();
        boolean shortOfEssence = !stopping && clickable() && !menu.affords();
        boolean hover = live && inCraft(mouseX, mouseY);

        Widgets.fillButton(graphics, x, y, CRAFT_W, CRAFT_H, live, hover);
        if (live || shortOfEssence) {
            graphics.renderOutline(x, y, CRAFT_W, CRAFT_H,
                    stopping ? ModPalette.SHORT : live ? ModPalette.REFINEMENT : ModPalette.SHORT_OUTLINE);
        }
    }

    private void drawCraftLabel(GuiGraphics graphics) {
        Component label = Component.translatable(menu.running() ? STOP_KEY : CRAFT_KEY);
        Widgets.drawButtonLabel(graphics, font, label, craftX(), craftY(), CRAFT_W, CRAFT_H, craftLive());
    }

    private boolean craftLive() { return menu.running() || (clickable() && menu.affords()); }

    private boolean clickable() { return menu.ready() && !menu.running(); }

    private int craftX() { return leftPos + CRAFT_X; }

    private int craftY() { return topPos + CRAFT_Y; }

    private boolean inCraft(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, craftX(), craftY(), CRAFT_W, CRAFT_H);
    }
    //endregion

    //region the 蛊方 button -- dead while the ritual runs, because the grid is locked anyway
    private void fillRecipe(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = recipeX();
        int y = recipeY();
        boolean live = !menu.running();
        boolean hover = live && inRecipe(mouseX, mouseY);

        Widgets.fillButton(graphics, x, y, RECIPE_W, RECIPE_H, live, hover);
        if (live) graphics.renderOutline(x, y, RECIPE_W, RECIPE_H, ModPalette.REFINEMENT);
    }

    private void drawRecipeLabel(GuiGraphics graphics) {
        Widgets.drawButtonLabel(graphics, font, Component.translatable(RECIPE_KEY), recipeX(), recipeY(), RECIPE_W,
                RECIPE_H, !menu.running());
    }

    private int recipeX() { return leftPos + RECIPE_X; }

    private int recipeY() { return topPos + RECIPE_Y; }

    private boolean inRecipe(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, recipeX(), recipeY(), RECIPE_W, RECIPE_H);
    }
    //endregion

    //region the 蛊方 a player may attempt -- the client holds the whole synced table, so it needs no packet
    private @Nullable GuRecipe selectedRecipe() {
        int index = menu.selected();
        List<GuRecipe> known = KnownRecipes.get();
        return index >= 0 && index < known.size() ? known.get(index) : null;
    }

    //endregion

    private int backX() { return leftPos + 11; }

    private int backY() { return topPos + 4; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (picker.isOpen()) {
            if (button == 0) {
                int chosen = picker.click(mouseX, mouseY, KnownRecipes.get(), leftPos, topPos, imageWidth, imageHeight);
                if (chosen != RecipePicker.NO_BUTTON) send(chosen);
            }
            return true;
        }
        if (button == 0) {
            if (Widgets.isOverBack(mouseX, mouseY, backX(), backY())) return Widgets.openInfo(this);
            if (inRecipe(mouseX, mouseY) && !menu.running()) return openPicker();
            if (inCraft(mouseX, mouseY) && menu.running()) return send(RefinementMenu.BUTTON_STOP);
            if (inCraft(mouseX, mouseY) && clickable()) return send(RefinementMenu.BUTTON_CRAFT);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dy) {
        if (!picker.isOpen()) return super.mouseScrolled(mouseX, mouseY, dy);
        picker.scroll(dy);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!picker.isOpen()) return super.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == InputConstants.KEY_ESCAPE
                || Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            picker.close();
        }
        return true;
    }

    private boolean openPicker() {
        picker.open();
        return true;
    }

    private boolean send(int button) { return Widgets.clickMenuButton(menu.containerId, button); }
}
