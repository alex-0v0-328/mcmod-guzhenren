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
 * 炼蛊界面。
 * <ul>
 *   <li>去四角 5×5 网格，内圈较亮，外套金框；另有三态炼制按钮、阶段条、元石[Primeval Stone]供给槽、蛊方选择器。</li>
 *   <li>⚠ 格子位置都取菜单自己的辅助方法，画出的格子与真实槽位不错开；有两种格距，网格不沿用背包格距。</li>
 *   <li>缺料虚影（{@link RecipeGhosts}）和资源池读数（{@link PoolBars}）从 {@code renderLabels} 画，手持物品才最后画；
 *       按钮和格子用 {@link Widgets}，整块背景一个 {@link GuiBatch}、三个按钮另一个，sprite 全在文字前。</li>
 *   <li>网格就绪但空窍[Aperture]付不起第一秒真元[Essence]时炼制标签变红、按钮失效；仪式运行时炼制按钮显示「停止」，
 *       蛊方按钮失效（网格已锁）。</li>
 *   <li>阶段条先在投石窗口内、再在间隔内走空；状态行未选蛊方时是内外圈图例，选后显示所选蛊方仍缺什么。</li>
 *   <li>选择器：蛊方表取 {@link KnownRecipes}，只排序一次；客户端持有完整同步蛊方表，不发包；弹窗沿 Z 平移 500，
 *       压在槽位物品上；打开时 {@link #mouseClicked(double, double, int)} 吞掉所有点击，只有左键作用于选择器；Esc
 *       先关选择器，否则同返回键回 B 面板。</li>
 *   <li>刚超 300 行：两个按钮、阶段条、状态行和选择器的输入分派都从菜单读同一份仪式状态。</li>
 * </ul>
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
    private static final Component CRAFT = Component.translatable("guzhenren.menu.refinement.craft");
    private static final Component STOP = Component.translatable("guzhenren.menu.refinement.stop");
    private static final Component LEGEND = Component.translatable("guzhenren.menu.refinement.rings");
    private static final String WINDOW_KEY = "guzhenren.menu.refinement.window";
    private static final String GAP_KEY = "guzhenren.menu.refinement.gap";
    private static final Component RECIPES = Component.translatable("guzhenren.menu.refinement.recipes");
    private static final String SELECTED_KEY = "guzhenren.menu.refinement.selected";
    private static final Component EXTRA = Component.translatable("guzhenren.menu.refinement.extra");
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

    //region the two rings [双圈]
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

    //region the phase bar [阶段条]
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

    //region the status line [状态行]
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
        if (recipe == null) return LEGEND;
        if (isCrowded(recipe)) return EXTRA;
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

    //region the craft button [炼制按钮]
    private void drawCraft(SpriteBatch sprites, int mouseX, int mouseY) {
        boolean live = isCraftLive();
        boolean hover = live && isInCraft(mouseX, mouseY);
        Widgets.drawButton(sprites, getCraftX(), getCraftY(), CRAFT_W, CRAFT_H, live, hover);
    }

    private void drawCraftLabel(GuiGraphics graphics) {
        Component label = menu.isRunning() ? STOP : CRAFT;
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

    //region the recipe button [蛊方按钮]
    private void drawRecipe(SpriteBatch sprites, int mouseX, int mouseY) {
        boolean live = !menu.isRunning();
        boolean hover = live && isInRecipe(mouseX, mouseY);
        Widgets.drawButton(sprites, getRecipeX(), getRecipeY(), RECIPE_W, RECIPE_H, live, hover);
    }

    private void drawRecipeLabel(GuiGraphics graphics) {
        Widgets.drawButtonLabel(graphics, font, RECIPES, getRecipeX(), getRecipeY(),
                RECIPE_W, RECIPE_H, !menu.isRunning());
    }

    private int getRecipeX() { return leftPos + RECIPE_X; }

    private int getRecipeY() { return topPos + RECIPE_Y; }

    private boolean isInRecipe(double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, getRecipeX(), getRecipeY(), RECIPE_W, RECIPE_H);
    }
    //endregion

    //region attemptable recipes [可尝试蛊方]
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
