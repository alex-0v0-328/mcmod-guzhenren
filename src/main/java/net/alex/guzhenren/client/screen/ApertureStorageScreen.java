package net.alex.guzhenren.client.screen;

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
 * The screen for one aperture's [空窍] store, a page at a time.
 *
 * <p>Extends {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen} for
 * {@link ApertureStorageMenu}. Draws 54 slots per page, the Vital Gu [本命蛊] slot in a side tab that juts past
 * the panel's right edge, and a pager (prev, page label, next) under the panel's bottom-left corner. A back button
 * ({@code <-}) at the top-left closes the container first, then opens the B panel. The side tab sprite is
 * drawn over the panel's top-right corner, which its left edge overlaps by {@link #SIDE_TAB_OVERLAP} pixels so the
 * two frames join, and one header rule runs across both. The background and the pager are one {@link GuiBatch} each,
 * sprites, then fills, then text.
 *
 * <p>⚠ The pager stays below the panel, off the top edge where Inventory Profiles Next draws its sort buttons;
 * only the back button sits up there, at the left.
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
