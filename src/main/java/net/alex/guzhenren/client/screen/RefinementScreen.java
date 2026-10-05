package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.refinement.GuRecipe;
import net.alex.guzhenren.gameplay.refinement.GuRecipeInput;
import net.alex.guzhenren.gameplay.refinement.RefinementMenu;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The refinement [炼蛊] screen: the ring grid, the phase bar, and the recipe picker.
 *
 * <p>Extends {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen} for
 * {@link RefinementMenu}. Draws the 5×5 grid (corners cut), the craft
 * button in three states, the phase bar, the stone slot, and the recipe picker modal. Ghosts for
 * missing ingredients are drawn from {@code renderLabels} so the carried item paints last. The picker
 * modal translates Z to 500 to stay above slot items. In {@link #mouseClicked(double, double, int)},
 * the open picker swallows every click; only the left button acts on it.
 *
 * <p>⚠ Every cell position comes from the menu's own helpers, so a drawn cell and the real slot
 * cannot drift apart. Two different pitches are in play; do not reuse the inventory's for the grid.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class RefinementScreen extends AbstractContainerScreen<RefinementMenu> {

    private static final int SLOT = RefinementMenu.SLOT;
    private static final int GRID_SLOT = RefinementMenu.GRID_SLOT;
    static final int CELL = 16;
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
    static final int LEGEND_TEXT = 0xFFA0A0A0;
    private static final int SHORT_RED = 0x99FF5555;
    private static final int TRACK = 0x33000000;
    private static final int BAR_WINDOW = 0xFF81C784;
    private static final int BAR_GAP = 0x6681C784;
    private static final int BAR_SHORT = 0xFFFF5555;
    private static final int GHOST_OVERLAY = 0x1AFFFFFF;
    private static final float GHOST_ALPHA = 0.35F;
    private static final int POOL_X = 18;
    private static final int POOL_W = 230;
    private static final int POOL_H = 5;
    private static final int POOL_Y = 156;
    private static final int POOL_STRIDE = 19;
    private static final String CRAFT_KEY = "guzhenren.menu.refinement.craft";
    private static final String STOP_KEY = "guzhenren.menu.refinement.stop";
    private static final String POOL_KEY = "guzhenren.menu.refinement.pool";
    private static final String LEGEND_KEY = "guzhenren.menu.refinement.rings";
    private static final String WINDOW_KEY = "guzhenren.menu.refinement.window";
    private static final String GAP_KEY = "guzhenren.menu.refinement.gap";
    private static final String RECIPE_KEY = "guzhenren.menu.refinement.recipes";
    private static final String SELECTED_KEY = "guzhenren.menu.refinement.selected";
    private static final String EXTRA_KEY = "guzhenren.menu.refinement.extra";
    private static final int BACK_W = 16;
    private static final int BACK_H = 14;
    private static final String BACK_GLYPH = "<-";
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
        graphics.fill(x, y, x + imageWidth, y + imageHeight, ModPalette.PANEL_FILL);
        graphics.renderOutline(x, y, imageWidth, imageHeight, ModPalette.BORDER);
        graphics.fill(x + MARGIN, y + HEADER_H, x + imageWidth - MARGIN, y + HEADER_H + 1, ModPalette.REFINEMENT);

        drawInput(graphics, x, y);
        drawCell(graphics, x + RefinementMenu.STONE_X, y + RefinementMenu.STONE_Y, ModPalette.SLOT_FILL);
        drawCells(graphics, x + RefinementMenu.OUTPUT_X, y + RefinementMenu.OUTPUT_Y,
                RefinementMenu.OUTPUT_COLS, RefinementMenu.OUTPUT_ROWS, GRID_SLOT, ModPalette.SLOT_FILL);
        drawCells(graphics, x + RefinementMenu.INVENTORY_X, y + RefinementMenu.INVENTORY_Y,
                RefinementMenu.INVENTORY_COLS, 3, SLOT, ModPalette.SLOT_FILL);
        drawCells(graphics, x + RefinementMenu.INVENTORY_X, y + RefinementMenu.HOTBAR_Y,
                RefinementMenu.INVENTORY_COLS, 1, SLOT, ModPalette.SLOT_FILL);
        drawBar(graphics, x, y);
    }

    //region the two rings -- the 内圈 is marked by a brighter cell and an accent frame around the block
    private void drawInput(GuiGraphics graphics, int x, int y) {
        for (int i = 0; i < GuRecipe.RING_SIZE; i++) {
            drawCell(graphics, x + RefinementMenu.ringX(i), y + RefinementMenu.ringY(i), ModPalette.SLOT_FILL);
        }
        drawCells(graphics, x + RefinementMenu.coreX(0), y + RefinementMenu.coreY(0),
                GuRecipe.CORE_COLS, GuRecipe.CORE_ROWS, GRID_SLOT, CORE_FILL);
        graphics.renderOutline(x + RefinementMenu.coreX(0) - 3, y + RefinementMenu.coreY(0) - 3,
                (GuRecipe.CORE_COLS - 1) * GRID_SLOT + CELL + 6,
                (GuRecipe.CORE_ROWS - 1) * GRID_SLOT + CELL + 6, ModPalette.REFINEMENT);
    }

    private void drawCells(GuiGraphics graphics, int x, int y, int cols, int rows, int pitch, int fill) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) drawCell(graphics, x + col * pitch, y + row * pitch, fill);
        }
    }

    private void drawCell(GuiGraphics graphics, int x, int y, int fill) {
        graphics.fill(x, y, x + CELL, y + CELL, fill);
    }
    //endregion

    //region the phase bar -- it empties over the 5s window, then over the 2s gap
    private void drawBar(GuiGraphics graphics, int x, int y) {
        int bx = x + BAR_X;
        int by = y + BAR_Y;
        graphics.fill(bx, by, bx + BAR_W, by + BAR_H, TRACK);
        if (!menu.running()) return;

        int span = menu.inWindow() ? GuRecipe.WINDOW_TICKS : GuRecipe.GAP_TICKS;
        int filled = BAR_W * menu.phaseLeft() / span;
        graphics.fill(bx, by, bx + filled, by + BAR_H, barColour());
    }

    private int barColour() {
        if (!menu.inWindow()) return BAR_GAP;
        return menu.stonesIn() < menu.stonesNeeded() ? BAR_SHORT : BAR_WINDOW;
    }
    //endregion

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, ModPalette.REFINEMENT, false);
        graphics.drawString(font, statusLine(), RefinementMenu.INPUT_X, LEGEND_Y,
                statusColour(), false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ModPalette.TEXT, false);
        renderGhosts(graphics);
        renderPools(graphics);
    }

    //region the pools -- all three ride the synced attachments, so none of them needs a packet
    private void renderPools(GuiGraphics graphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        long maxEssence = ApertureEssenceService.getMaxEssence(player);
        int unit = 0;
        drawPool(graphics, unit++, ApertureEssenceService.getCurrentEssence(player), maxEssence, ModPalette.APERTURE);

        long distilled = ApertureEssenceService.getDistilledEssence(player);
        if (distilled > 0L) drawPool(graphics, unit++, distilled, maxEssence, ModPalette.DISTILLED_FILL);

        SoulData soul = SoulService.get(player);
        drawPool(graphics, unit, soul.currentSoul(), soul.maxSoul(), ModPalette.SOUL);
    }

    private void drawPool(GuiGraphics graphics, int unit, long value, long max, int fill) {
        int y = POOL_Y + unit * POOL_STRIDE;
        Component reading = Component.translatable(POOL_KEY, value, max);
        graphics.drawString(font, reading, POOL_X + POOL_W - font.width(reading), y, LEGEND_TEXT, false);

        int barY = y + font.lineHeight;
        graphics.fill(POOL_X, barY, POOL_X + POOL_W, barY + POOL_H, TRACK);
        if (max <= 0L || value <= 0L) return;

        int filled = (int) Math.min(POOL_W, POOL_W * value / max);
        graphics.fill(POOL_X, barY, POOL_X + filled, barY + POOL_H, fill);
    }
    //endregion

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
        return Component.translatable(SELECTED_KEY, resultName(recipe));
    }

    private int statusColour() {
        if (menu.running()) return ModPalette.TEXT;

        GuRecipe recipe = selectedRecipe();
        if (recipe == null) return LEGEND_TEXT;
        return crowded(recipe) ? BAR_SHORT : ModPalette.TEXT;
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
        knownCache = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderCraft(graphics, mouseX, mouseY);
        renderRecipe(graphics, mouseX, mouseY);
        renderBack(graphics, mouseX, mouseY);
        if (picker.isOpen()) {
            picker.render(graphics, font, known(), menu.selected(), leftPos, topPos, imageWidth, imageHeight, mouseX,
                    mouseY);
            return;
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    //region the cells a picked 蛊方 still wants -- drawn from renderLabels, so the carried item stays on top
    private void renderGhosts(GuiGraphics graphics) {
        GuRecipe recipe = selectedRecipe();
        if (recipe == null || menu.running()) return;

        GuRecipeInput grid = menu.grid();
        int[] missing = recipe.shortfall(grid);

        for (int n = 0; n < missing.length; n++) {
            int slot = recipe.slots().get(n);
            if (missing[n] <= 0 || slot < 0 || slot >= grid.size()) continue;
            if (!grid.getItem(slot).isEmpty()) continue;

            ItemStack shown = option(recipe.ingredients().get(n));
            if (!shown.isEmpty()) drawGhost(graphics, shown, missing[n], slot);
        }
    }

    private void drawGhost(GuiGraphics graphics, ItemStack shown, int count, int slot) {
        int x = slotX(slot);
        int y = slotY(slot);

        graphics.setColor(1.0F, 1.0F, 1.0F, GHOST_ALPHA);
        graphics.renderFakeItem(shown, x, y);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.fill(RenderType.guiGhostRecipeOverlay(), x, y, x + CELL, y + CELL, GHOST_OVERLAY);
        graphics.renderItemDecorations(font, shown, x, y, count > 1 ? String.valueOf(count) : null);
    }

    private static int slotX(int slot) {
        return slot < GuRecipe.RING_SIZE ? RefinementMenu.ringX(slot)
                : RefinementMenu.coreX((slot - GuRecipe.RING_SIZE) % GuRecipe.CORE_COLS);
    }

    private static int slotY(int slot) {
        return slot < GuRecipe.RING_SIZE ? RefinementMenu.ringY(slot)
                : RefinementMenu.coreY((slot - GuRecipe.RING_SIZE) / GuRecipe.CORE_COLS);
    }

    static ItemStack option(SizedIngredient need) {
        ItemStack[] options = need.ingredient().getItems();
        if (options.length == 0) return ItemStack.EMPTY;
        return options[(int) (Util.getMillis() / 1000L % options.length)];
    }
    //endregion

    //region the 炼制 button -- three states off the ritual, and 停止 while it runs
    private void renderCraft(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = craftX();
        int y = craftY();
        boolean stopping = menu.running();
        boolean live = stopping || (clickable() && menu.affords());
        boolean shortOfEssence = !stopping && clickable() && !menu.affords();
        boolean hover = live && inCraft(mouseX, mouseY);

        graphics.fill(x, y, x + CRAFT_W, y + CRAFT_H,
                live ? (hover ? ModPalette.BUTTON_HOVER : ModPalette.BUTTON_IDLE) : ModPalette.BUTTON_DEAD);
        if (live || shortOfEssence) {
            graphics.renderOutline(x, y, CRAFT_W, CRAFT_H,
                    stopping ? BAR_SHORT : live ? ModPalette.REFINEMENT : SHORT_RED);
        }

        Component label = Component.translatable(stopping ? STOP_KEY : CRAFT_KEY);
        graphics.drawString(font, label, x + (CRAFT_W - font.width(label)) / 2,
                y + (CRAFT_H - font.lineHeight) / 2 + 1, live ? ModPalette.TEXT : ModPalette.BUTTON_IDLE, false);
    }

    private boolean clickable() { return menu.ready() && !menu.running(); }

    private int craftX() { return leftPos + CRAFT_X; }

    private int craftY() { return topPos + CRAFT_Y; }

    private boolean inCraft(double mouseX, double mouseY) {
        return mouseX >= craftX() && mouseX < craftX() + CRAFT_W
                && mouseY >= craftY() && mouseY < craftY() + CRAFT_H;
    }
    //endregion

    //region the 蛊方 button -- dead while the ritual runs, because the grid is locked anyway
    private void renderRecipe(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = recipeX();
        int y = recipeY();
        boolean live = !menu.running();
        boolean hover = live && inRecipe(mouseX, mouseY);

        graphics.fill(x, y, x + RECIPE_W, y + RECIPE_H,
                live ? (hover ? ModPalette.BUTTON_HOVER : ModPalette.BUTTON_IDLE) : ModPalette.BUTTON_DEAD);
        if (live) graphics.renderOutline(x, y, RECIPE_W, RECIPE_H, ModPalette.REFINEMENT);

        Component label = Component.translatable(RECIPE_KEY);
        graphics.drawString(font, label, x + (RECIPE_W - font.width(label)) / 2,
                y + (RECIPE_H - font.lineHeight) / 2 + 1, live ? ModPalette.TEXT : ModPalette.BUTTON_IDLE, false);
    }

    private int recipeX() { return leftPos + RECIPE_X; }

    private int recipeY() { return topPos + RECIPE_Y; }

    private boolean inRecipe(double mouseX, double mouseY) {
        return mouseX >= recipeX() && mouseX < recipeX() + RECIPE_W
                && mouseY >= recipeY() && mouseY < recipeY() + RECIPE_H;
    }
    //endregion

    //region the 蛊方 a player may attempt -- the client holds the whole synced table, so it needs no packet
    private @Nullable List<RecipeHolder<GuRecipe>> knownCache;

    private List<RecipeHolder<GuRecipe>> known() {
        if (knownCache == null) {
            ClientLevel level = Minecraft.getInstance().level;
            knownCache = level == null ? List.of() : GuRecipe.known(level.getRecipeManager());
        }
        return knownCache;
    }

    private @Nullable GuRecipe selectedRecipe() {
        int index = menu.selected();
        List<RecipeHolder<GuRecipe>> known = known();
        return index >= 0 && index < known.size() ? known.get(index).value() : null;
    }

    static ItemStack result(GuRecipe recipe) {
        return recipe.results().isEmpty() ? ItemStack.EMPTY : recipe.results().getFirst();
    }

    private static Component resultName(GuRecipe recipe) { return result(recipe).getHoverName(); }
    //endregion

    private void renderBack(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = backX();
        int y = backY();
        boolean hover = inBack(mouseX, mouseY);
        graphics.fill(x, y, x + BACK_W, y + BACK_H, hover ? ModPalette.BUTTON_HOVER : ModPalette.BUTTON_IDLE);
        graphics.drawString(font, BACK_GLYPH, x + (BACK_W - font.width(BACK_GLYPH)) / 2,
                y + (BACK_H - font.lineHeight) / 2 + 1, ModPalette.TEXT, false);
    }

    private int backX() { return leftPos + 11; }

    private int backY() { return topPos + 4; }

    private boolean inBack(double mouseX, double mouseY) {
        return mouseX >= backX() && mouseX < backX() + BACK_W && mouseY >= backY() && mouseY < backY() + BACK_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (picker.isOpen()) {
            if (button == 0) {
                int chosen = picker.click(mouseX, mouseY, known(), leftPos, topPos, imageWidth, imageHeight);
                if (chosen != RecipePicker.NO_BUTTON) send(chosen);
            }
            return true;
        }
        if (button == 0) {
            if (inBack(mouseX, mouseY)) return clickBack();
            if (inRecipe(mouseX, mouseY) && !menu.running()) return openPicker();
            if (inCraft(mouseX, mouseY) && menu.running()) return send(RefinementMenu.BUTTON_STOP);
            if (inCraft(mouseX, mouseY) && clickable()) return send(RefinementMenu.BUTTON_CRAFT);
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

    private boolean clickBack() {
        onClose();
        Minecraft.getInstance().setScreen(new PlayerInfoScreen());
        return true;
    }

    private boolean send(int button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;
        mc.gameMode.handleInventoryButtonClick(menu.containerId, button);
        return true;
    }
}
