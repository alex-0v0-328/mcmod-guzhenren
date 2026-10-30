package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The refinement [炼蛊] screen: the ring grid, the phase bar, and the recipe picker.
 *
 * <p>Extends {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen} for
 * {@link RefinementMenu}. Draws the 5×5 grid (corners cut, the 内圈 in brighter cells inside a gold frame), the
 * craft button in three states, the phase bar, the stone slot, and the recipe picker modal. Ghosts for
 * missing ingredients ({@link RecipeGhosts}) and the pool readings ({@link PoolBars}) are drawn from
 * {@code renderLabels} so the carried item paints last; the buttons and cells are the package's {@link Widgets}, the
 * whole background in one {@link GuiBatch} and the three buttons in another, every sprite before any label. The
 * craft label turns red while the grid is ready but the aperture cannot pay the first second's essence; the button is
 * dead then. The recipe table the screen and its picker read is {@link KnownRecipes},
 * sorted once rather than per frame. The picker modal translates Z to 500 to stay above slot items. In
 * {@link #mouseClicked(double, double, int)}, the open picker swallows every click; only the left button acts on it.
 * Esc closes an open picker, otherwise it goes back to the B panel like the back button.
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
    private static final ResourceLocation CORE_FRAME = Guzhenren.id("core_frame");
    private static final int CORE_FRAME_PAD = 7;
    private static final int BAR_WINDOW = 0xFF81C784;
    private static final int BAR_GAP = 0xFF415A41;
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
            SpriteBatch.draw(graphics, sprites -> {
                Widgets.drawPanel(sprites, x, y, imageWidth, imageHeight);
                drawInput(sprites, x, y);
                Widgets.drawCell(sprites, x + RefinementMenu.STONE_X, y + RefinementMenu.STONE_Y);
                Widgets.drawCells(sprites, x + RefinementMenu.OUTPUT_X, y + RefinementMenu.OUTPUT_Y,
                        RefinementMenu.OUTPUT_COLS, RefinementMenu.OUTPUT_ROWS, GRID_SLOT);
                Widgets.drawCells(sprites, x + RefinementMenu.INVENTORY_X, y + RefinementMenu.INVENTORY_Y,
                        RefinementMenu.INVENTORY_COLS, 3, SLOT);
                Widgets.drawCells(sprites, x + RefinementMenu.INVENTORY_X, y + RefinementMenu.HOTBAR_Y,
                        RefinementMenu.INVENTORY_COLS, 1, SLOT);
                drawBar(sprites, x, y);
            });
            Widgets.drawRule(graphics, x + MARGIN, y + HEADER_H, x + imageWidth - MARGIN, ModPalette.REFINEMENT);
        });
    }

    //region the two rings -- the 内圈 is marked by brighter cells and a gold frame around the block
    private static void drawInput(SpriteBatch sprites, int x, int y) {
        for (int i = 0; i < GuRecipe.RING_SIZE; i++) {
            Widgets.drawCell(sprites, x + RefinementMenu.getRingX(i), y + RefinementMenu.getRingY(i));
        }
        for (int row = 0; row < GuRecipe.CORE_ROWS; row++) {
            for (int col = 0; col < GuRecipe.CORE_COLS; col++) {
                Widgets.drawCell(sprites, Widgets.SLOT_CORE, x + RefinementMenu.getCoreX(col),
                        y + RefinementMenu.getCoreY(row));
            }
        }
        sprites.blit(CORE_FRAME, x + RefinementMenu.getCoreX(0) - CORE_FRAME_PAD,
                y + RefinementMenu.getCoreY(0) - CORE_FRAME_PAD,
                (GuRecipe.CORE_COLS - 1) * GRID_SLOT + Widgets.CELL + CORE_FRAME_PAD * 2,
                (GuRecipe.CORE_ROWS - 1) * GRID_SLOT + Widgets.CELL + CORE_FRAME_PAD * 2);
    }
    //endregion

    //region the phase bar -- it empties over the 5s window, then over the 2s gap
    private void drawBar(SpriteBatch sprites, int x, int y) {
        int filled = 0;
        if (menu.isRunning()) {
            int span = menu.isInWindow() ? GuRecipe.WINDOW_TICKS : GuRecipe.GAP_TICKS;
            filled = (BAR_W - 2) * menu.getPhaseLeft() / span;
        }
        Widgets.drawBar(sprites, x + BAR_X, y + BAR_Y, BAR_W, BAR_H, filled, getBarColor());
    }

    private int getBarColor() {
        if (!menu.isInWindow()) return BAR_GAP;
        return menu.getStonesIn() < menu.getStonesNeeded() ? ModPalette.SHORT : BAR_WINDOW;
    }
    //endregion

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, ModPalette.REFINEMENT, false);
        graphics.drawString(font, formatStatusLine(), RefinementMenu.INPUT_X, LEGEND_Y,
                getStatusColor(), false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ModPalette.TEXT, false);
        renderGhosts(graphics);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) PoolBars.draw(graphics, font, player);
    }

    //region the status line -- the ring legend until a 蛊方 is picked, then what that pick still wants
    private Component formatStatusLine() {
        if (menu.isRunning()) {
            int shown = menu.getStage() + 1;
            int seconds = (menu.getPhaseLeft() + Ticks.SECOND - 1) / Ticks.SECOND;
            return menu.isInWindow()
                    ? Component.translatable(WINDOW_KEY, shown, menu.getStages(), seconds,
                    menu.getStonesIn(), menu.getStonesNeeded())
                    : Component.translatable(GAP_KEY, shown, menu.getStages());
        }
        GuRecipe recipe = getSelectedRecipe();
        if (recipe == null) return Component.translatable(LEGEND_KEY);
        if (isCrowded(recipe)) return Component.translatable(EXTRA_KEY);
        return Component.translatable(SELECTED_KEY, RecipeGhosts.getResult(recipe).getHoverName());
    }

    private int getStatusColor() {
        if (menu.isRunning()) return ModPalette.TEXT;

        GuRecipe recipe = getSelectedRecipe();
        if (recipe == null) return ModPalette.LEGEND_TEXT;
        return isCrowded(recipe) ? ModPalette.SHORT : ModPalette.TEXT;
    }

    private boolean isCrowded(GuRecipe recipe) {
        if (menu.isReady()) return false;

        for (int missing : recipe.getShortfall(menu.getGrid())) {
            if (missing > 0) return false;
        }
        return true;
    }
    //endregion

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> {
                drawCraft(sprites, mouseX, mouseY);
                drawRecipe(sprites, mouseX, mouseY);
                Widgets.drawBack(sprites, getBackX(), getBackY(), mouseX, mouseY);
            });
            drawCraftLabel(graphics);
            drawRecipeLabel(graphics);
            Widgets.drawBackLabel(graphics, font, getBackX(), getBackY());
        });
        if (picker.isOpen()) {
            picker.render(graphics, font, KnownRecipes.get(), menu.getSelected(), leftPos, topPos, imageWidth,
                    imageHeight, mouseX, mouseY);
            return;
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderGhosts(GuiGraphics graphics) {
        GuRecipe recipe = getSelectedRecipe();
        if (recipe != null && !menu.isRunning()) RecipeGhosts.draw(graphics, font, recipe, menu.getGrid());
    }

    //region the 炼制 button -- three states off the ritual, and 停止 while it runs
    private void drawCraft(SpriteBatch sprites, int mouseX, int mouseY) {
        boolean live = isCraftLive();
        boolean hover = live && isInCraft(mouseX, mouseY);
        Widgets.drawButton(sprites, getCraftX(), getCraftY(), CRAFT_W, CRAFT_H, live, hover);
    }

    private void drawCraftLabel(GuiGraphics graphics) {
        Component label = Component.translatable(menu.isRunning() ? STOP_KEY : CRAFT_KEY);
        int colour = isCraftLive() ? ModPalette.TEXT : isClickable() ? ModPalette.SHORT : ModPalette.TEXT_DEAD;
        Widgets.drawCentered(graphics, font, label, getCraftX(), getCraftY(), CRAFT_W, CRAFT_H, colour);
    }

    private boolean isCraftLive() { return menu.isRunning() || (isClickable() && menu.canAfford()); }

    private boolean isClickable() { return menu.isReady() && !menu.isRunning(); }

    private int getCraftX() { return leftPos + CRAFT_X; }

    private int getCraftY() { return topPos + CRAFT_Y; }

    private boolean isInCraft(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, getCraftX(), getCraftY(), CRAFT_W, CRAFT_H);
    }
    //endregion

    //region the 蛊方 button -- dead while the ritual runs, because the grid is locked anyway
    private void drawRecipe(SpriteBatch sprites, int mouseX, int mouseY) {
        boolean live = !menu.isRunning();
        boolean hover = live && isInRecipe(mouseX, mouseY);
        Widgets.drawButton(sprites, getRecipeX(), getRecipeY(), RECIPE_W, RECIPE_H, live, hover);
    }

    private void drawRecipeLabel(GuiGraphics graphics) {
        Widgets.drawButtonLabel(graphics, font, Component.translatable(RECIPE_KEY), getRecipeX(), getRecipeY(),
                RECIPE_W, RECIPE_H, !menu.isRunning());
    }

    private int getRecipeX() { return leftPos + RECIPE_X; }

    private int getRecipeY() { return topPos + RECIPE_Y; }

    private boolean isInRecipe(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, getRecipeX(), getRecipeY(), RECIPE_W, RECIPE_H);
    }
    //endregion

    //region the 蛊方 a player may attempt -- the client holds the whole synced table, so it needs no packet
    private @Nullable GuRecipe getSelectedRecipe() {
        int index = menu.getSelected();
        List<GuRecipe> known = KnownRecipes.get();
        return index >= 0 && index < known.size() ? known.get(index) : null;
    }

    //endregion

    private int getBackX() { return leftPos + 11; }

    private int getBackY() { return topPos + 4; }

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
            if (Widgets.isOverBack(mouseX, mouseY, getBackX(), getBackY())) return Widgets.openInfo(this);
            if (isInRecipe(mouseX, mouseY) && !menu.isRunning()) return openPicker();
            if (isInCraft(mouseX, mouseY) && menu.isRunning()) return send(RefinementMenu.BUTTON_STOP);
            if (isInCraft(mouseX, mouseY) && isClickable()) return send(RefinementMenu.BUTTON_CRAFT);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (!picker.isOpen()) return super.mouseScrolled(mouseX, mouseY, dx, dy);
        picker.scroll(dy);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!picker.isOpen()) {
            if (keyCode == InputConstants.KEY_ESCAPE) return Widgets.openInfo(this, true);
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
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
