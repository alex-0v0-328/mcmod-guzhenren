package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
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
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 炼蛊操作界面。
 * <ul>
 *   <li>中央为去四角 5×5 炼蛊网格（内圈格子亮一档；网格就绪时内圈外描一圈 1 像素金线，金只表状态），配有三态炼制按
 *       钮、阶段进度条、元石[Primeval Stone]供给槽与蛊方选择器。</li>
 *   <li>⚠ 所有格子坐标均直接读取菜单自身的布局辅助方法，防止绘制格位与真实槽位错位（炼蛊网格与下方玩家背包使用两种不同
 *       格距）。</li>
 *   <li>缺料虚影（{@link RecipeGhosts}）与资源池读数（{@link PoolBars}）在 {@code renderLabels} 中绘制，确保手持物
 *       品最后绘制在最上层；按钮与格子通过 {@link Widgets} 绘制，整块背景使用一个 {@link GuiBatch}、三个按钮共用另一
 *       个 {@link GuiBatch}，且精灵图全部排在文本之前。</li>
 *   <li>当网格就绪但空窍支付不起第一秒真元时，炼制按钮文字变红且按钮禁用；炼制仪式运行期间炼制按钮切换为「停止」且蛊
 *       方选择按钮禁用（网格已锁定）。</li>
 *   <li>阶段条先在投石窗口内、再在间隔期内逐步走空；状态行未选蛊方时显示内外圈图例，选定后提示当前蛊方尚缺哪些条
 *       件。</li>
 *   <li>蛊方选择器的数据源取自 {@link KnownRecipes} 缓存（客户端持有全量同步配方表，无需发包）；弹窗沿 Z 轴平移
 *       500 压在槽位物品之上；弹窗开启时 {@link #mouseClicked(double, double, int)} 吞掉所有点击且仅左键作用于选择
 *       器，按 Esc 先关闭选择器，否则与返回键一样退回 B 面板。</li>
 *   <li>本文件略超 300 行：两个按钮、阶段条、状态行与选择器的输入分派均围绕菜单提供的同一份仪式状态协同工作。</li>
 * </ul>
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
    private static final int CORE_OUTLINE_PAD = 3;
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
            Widgets.drawRule(graphics, x + MARGIN, y + HEADER_H, x + imageWidth - MARGIN, ModPalette.SURFACE_2);
            if (menu.isReady()) drawCoreOutline(graphics, x, y);
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
    }

    private static void drawCoreOutline(GuiGraphics graphics, int x, int y) {
        int left = x + RefinementMenu.getCoreX(0) - CORE_OUTLINE_PAD;
        int top = y + RefinementMenu.getCoreY(0) - CORE_OUTLINE_PAD;
        int width = (GuRecipe.CORE_COLS - 1) * GRID_SLOT + Widgets.CELL + CORE_OUTLINE_PAD * 2;
        int height = (GuRecipe.CORE_ROWS - 1) * GRID_SLOT + Widgets.CELL + CORE_OUTLINE_PAD * 2;
        graphics.renderOutline(left, top, width, height, ModPalette.ACCENT);
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
        graphics.drawString(font, title, titleLabelX, titleLabelY, ModPalette.ACCENT, false);
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
                    : Component.translatable(GAP_KEY);
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
            KnownRecipes.Learned learned = KnownRecipes.getLearned();
            picker.render(graphics, font, learned.recipes(), learned.toLocal(menu.getSelected()), leftPos, topPos,
                    imageWidth, imageHeight, mouseX, mouseY);
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
                KnownRecipes.Learned learned = KnownRecipes.getLearned();
                int chosen = picker.click(mouseX, mouseY, learned.recipes(), leftPos, topPos, imageWidth, imageHeight);
                if (chosen >= RefinementMenu.BUTTON_RECIPE_BASE) {
                    int global = learned.toGlobal(chosen - RefinementMenu.BUTTON_RECIPE_BASE);
                    chosen = global < 0 ? RecipePicker.NO_BUTTON : RefinementMenu.BUTTON_RECIPE_BASE + global;
                }
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
