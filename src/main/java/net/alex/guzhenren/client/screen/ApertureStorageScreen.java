package net.alex.guzhenren.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * 单个空窍[Aperture]的分页存储界面，每页 54 格。
 * <ul>
 *   <li>本命蛊格在伸出面板右缘的侧标签里；侧标签 sprite 盖住面板右上角，左缘重叠 {@link #SIDE_TAB_OVERLAP}
 *       像素使两框相接，标题分隔线横贯两者。</li>
 *   <li>背景、翻页器各一个 {@link GuiBatch}，先 sprite，再填色，最后文字。</li>
 *   <li>⚠ 翻页器（上一页、页码、下一页）在面板左下角下方，避开上缘 Inventory Profiles Next 的排序按钮；
 *       上缘只放左侧返回键 {@code <-}，它或 Esc 先关容器再开 B 面板。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureStorageMenu
 * @since 1.0.0
 */

public class ApertureStorageScreen extends AbstractContainerScreen<ApertureStorageMenu> {

    private static final int PAGE_BUTTON_W = 16;
    private static final int PAGE_BUTTON_H = 14;
    private static final int PAGE_LABEL_W = 40;
    private static final ResourceLocation SIDE_TAB = Guzhenren.id("panel_side_tab");
    private static final int SIDE_TAB_OVERLAP = 6;
    private static final int SIDE_TAB_W = 40;
    private static final int SIDE_TAB_H = 44;
    private static final int HEADER_RULE_Y = 15;
    private static final int RULE_INSET = 7;
    private static final Component VITAL = Component.translatable("guzhenren.menu.vital");
    private static final int TITLE_X_WITH_BACK = 26;
    private static final String LOAD_KEY = "guzhenren.menu.aperture_load";
    private static final Component PREV = Component.literal("<");
    private static final Component NEXT = Component.literal(">");

    public ApertureStorageScreen(ApertureStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = TITLE_X_WITH_BACK;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int sideTabX = x + imageWidth - SIDE_TAB_OVERLAP;
        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> {
                Widgets.drawPanel(sprites, x, y, imageWidth, imageHeight);
                sprites.blit(SIDE_TAB, sideTabX, y, SIDE_TAB_W, SIDE_TAB_H);
                int left = x + ApertureStorageMenu.STORAGE_X;
                drawSlotGrid(sprites, left, y + ApertureStorageMenu.STORAGE_Y, ApertureStorageMenu.ROWS);
                drawSlotGrid(sprites, left, y + ApertureStorageMenu.INVENTORY_Y, ApertureStorageMenu.INVENTORY_ROWS);
                drawSlotGrid(sprites, left, y + ApertureStorageMenu.HOTBAR_Y, 1);
                Widgets.drawCell(sprites, x + ApertureStorageMenu.VITAL_X, y + ApertureStorageMenu.VITAL_Y);
            });
            Widgets.drawRule(graphics, x + RULE_INSET, y + HEADER_RULE_Y, sideTabX + SIDE_TAB_W - RULE_INSET,
                    ModPalette.APERTURE);
            renderVitalLabel(graphics, x, y);
        });
    }

    private static void drawSlotGrid(SpriteBatch sprites, int left, int top, int rows) {
        Widgets.drawCells(sprites, left, top, ApertureStorageMenu.COLS, rows, ApertureStorageMenu.SLOT);
    }

    private void renderVitalLabel(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, VITAL, x + ApertureStorageMenu.VITAL_X + (Widgets.CELL - font.width(VITAL)) / 2,
                y + titleLabelY, ModPalette.APERTURE, false);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, ModPalette.APERTURE, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ModPalette.TEXT, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPager(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderPager(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean prevLive = menu.getPageIndex() > 0;
        boolean nextLive = menu.getPageIndex() + 1 < menu.getPageCount();
        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> {
                Widgets.drawBack(sprites, getBackX(), getPagerY(), mouseX, mouseY);
                drawPageButton(sprites, mouseX, mouseY, getPrevX(), prevLive);
                drawPageButton(sprites, mouseX, mouseY, getNextX(), nextLive);
            });

            Widgets.drawBackLabel(graphics, font, getBackX(), getPagerY());
            drawPageGlyph(graphics, getPrevX(), prevLive, PREV);
            drawPageGlyph(graphics, getNextX(), nextLive, NEXT);
            Component page = Component.literal((menu.getPageIndex() + 1) + " / " + menu.getPageCount());
            Widgets.drawCentered(graphics, font, page, getLabelX(), getPagerBottomY(), PAGE_LABEL_W, PAGE_BUTTON_H,
                    ModPalette.TEXT);

            Component load = Component.translatable(LOAD_KEY, menu.getLoad(), ApertureStorageService.MAX_LOAD);
            graphics.drawString(font, load, leftPos + imageWidth - font.width(load), getPagerBottomY() + 1,
                    ModPalette.APERTURE, false);
        });
    }

    private void drawPageButton(SpriteBatch sprites, int mouseX, int mouseY, int x, boolean live) {
        boolean hover = live && isInPageButton(mouseX, mouseY, x);
        Widgets.drawButton(sprites, x, getPagerBottomY(), PAGE_BUTTON_W, PAGE_BUTTON_H, live, hover);
    }

    private void drawPageGlyph(GuiGraphics graphics, int x, boolean live, Component glyph) {
        Widgets.drawButtonLabel(graphics, font, glyph, x, getPagerBottomY(), PAGE_BUTTON_W, PAGE_BUTTON_H, live);
    }

    private int getPagerY() { return topPos + 3; }

    private int getPagerBottomY() { return topPos + imageHeight + 4; }

    private int getBackX() { return leftPos + 7; }

    private int getPrevX() { return leftPos + 7; }

    private int getLabelX() { return getPrevX() + PAGE_BUTTON_W; }

    private int getNextX() { return getLabelX() + PAGE_LABEL_W; }

    private boolean isInPageButton(double mouseX, double mouseY, int x) {
        return Widgets.isOver(mouseX, mouseY, x, getPagerBottomY(), PAGE_BUTTON_W, PAGE_BUTTON_H);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) return Widgets.openInfo(this, true);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (Widgets.isOverBack(mouseX, mouseY, getBackX(), getPagerY())) return Widgets.openInfo(this);
            if (isInPageButton(mouseX, mouseY, getPrevX())) {
                return Widgets.clickMenuButton(menu.containerId, ApertureStorageMenu.BUTTON_PREV);
            }
            if (isInPageButton(mouseX, mouseY, getNextX())) {
                return Widgets.clickMenuButton(menu.containerId, ApertureStorageMenu.BUTTON_NEXT);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
