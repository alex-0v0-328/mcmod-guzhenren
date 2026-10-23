package net.alex.guzhenren.client.screen;

import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.vault.ApertureStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * The screen for one aperture's [空窍] store, a page at a time.
 *
 * <p>Extends {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen} for
 * {@link ApertureStorageMenu}. Draws 54 slots per page, prev/next pager
 * buttons, and the Vital Gu [本命蛊] slot past the right edge of the panel. A back button ({@code <-})
 * closes the container first, then opens the B panel. All drawing is {@code g.fill}, no textures; the
 * background and the pager are one {@link GuiBatch} each, fills before text.
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
    private static final int VITAL_PADDING = 8;
    private static final int VITAL_LEFT = ApertureStorageMenu.VITAL_X - VITAL_PADDING;
    private static final int VITAL_RIGHT = ApertureStorageMenu.VITAL_X + Widgets.CELL + VITAL_PADDING;
    private static final int VITAL_BOTTOM = 44;
    private static final int HEADER_RULE_Y = 15;
    private static final int PANEL_RULE_INSET = 7;
    private static final int VITAL_RULE_INSET = 4;
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
        GuiBatch.draw(graphics, () -> {
            Widgets.drawPanel(graphics, x, y, imageWidth, imageHeight, ModPalette.PANEL_FILL);
            Widgets.drawRule(graphics, x + PANEL_RULE_INSET, y + HEADER_RULE_Y, x + imageWidth - PANEL_RULE_INSET,
                    ModPalette.APERTURE);

            int left = x + ApertureStorageMenu.STORAGE_X;
            fillSlotGrid(graphics, left, y + ApertureStorageMenu.STORAGE_Y, ApertureStorageMenu.ROWS);
            fillSlotGrid(graphics, left, y + ApertureStorageMenu.INVENTORY_Y, ApertureStorageMenu.INVENTORY_ROWS);
            fillSlotGrid(graphics, left, y + ApertureStorageMenu.HOTBAR_Y, 1);
            fillVital(graphics, x, y);
            renderVitalLabel(graphics, x, y);
        });
    }

    private void fillSlotGrid(GuiGraphics graphics, int left, int top, int rows) {
        Widgets.drawCells(graphics, left, top, ApertureStorageMenu.COLS, rows, ApertureStorageMenu.SLOT,
                ModPalette.SLOT_FILL);
    }

    private void fillVital(GuiGraphics graphics, int x, int y) {
        Widgets.drawPanel(graphics, x + VITAL_LEFT, y, VITAL_RIGHT - VITAL_LEFT, VITAL_BOTTOM, ModPalette.PANEL_FILL);
        Widgets.drawRule(graphics, x + VITAL_LEFT + VITAL_RULE_INSET, y + HEADER_RULE_Y,
                x + VITAL_RIGHT - VITAL_RULE_INSET, ModPalette.APERTURE);
        Widgets.drawCell(graphics, x + ApertureStorageMenu.VITAL_X, y + ApertureStorageMenu.VITAL_Y,
                ModPalette.SLOT_FILL);
    }

    private void renderVitalLabel(GuiGraphics graphics, int x, int y) {
        int width = VITAL_RIGHT - VITAL_LEFT;
        graphics.drawString(font, VITAL, x + VITAL_LEFT + (width - font.width(VITAL)) / 2, y + 5,
                ModPalette.APERTURE, false);
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
            Widgets.fillBack(graphics, backX(), pagerY(), mouseX, mouseY);
            fillPageButton(graphics, mouseX, mouseY, prevX(), prevLive);
            fillPageButton(graphics, mouseX, mouseY, nextX(), nextLive);

            Widgets.drawBackLabel(graphics, font, backX(), pagerY());
            drawPageGlyph(graphics, prevX(), prevLive, PREV);
            drawPageGlyph(graphics, nextX(), nextLive, NEXT);
            Component page = Component.literal((menu.getPageIndex() + 1) + " / " + menu.getPageCount());
            Widgets.drawCentered(graphics, font, page, labelX(), pagerBottomY(), PAGE_LABEL_W, PAGE_BUTTON_H,
                    ModPalette.TEXT);

            Component load = Component.translatable(LOAD_KEY, menu.getLoad(), ApertureStorageService.MAX_LOAD);
            graphics.drawString(font, load, leftPos + imageWidth - font.width(load), pagerBottomY() + 1,
                    ModPalette.APERTURE, false);
        });
    }

    private void fillPageButton(GuiGraphics graphics, int mouseX, int mouseY, int x, boolean live) {
        boolean hover = live && inPageButton(mouseX, mouseY, x);
        Widgets.fillButton(graphics, x, pagerBottomY(), PAGE_BUTTON_W, PAGE_BUTTON_H, live, hover);
    }

    private void drawPageGlyph(GuiGraphics graphics, int x, boolean live, Component glyph) {
        Widgets.drawButtonLabel(graphics, font, glyph, x, pagerBottomY(), PAGE_BUTTON_W, PAGE_BUTTON_H, live);
    }

    private int pagerY() { return topPos + 3; }

    private int pagerBottomY() { return topPos + imageHeight + 4; }

    private int backX() { return leftPos + 7; }

    private int prevX() { return leftPos + 7; }

    private int labelX() { return prevX() + PAGE_BUTTON_W; }

    private int nextX() { return labelX() + PAGE_LABEL_W; }

    private boolean inPageButton(double mouseX, double mouseY, int x) {
        return Widgets.isOver(mouseX, mouseY, x, pagerBottomY(), PAGE_BUTTON_W, PAGE_BUTTON_H);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (Widgets.isOverBack(mouseX, mouseY, backX(), pagerY())) return Widgets.openInfo(this);
            if (inPageButton(mouseX, mouseY, prevX())) {
                return Widgets.clickMenuButton(menu.containerId, ApertureStorageMenu.BUTTON_PREV);
            }
            if (inPageButton(mouseX, mouseY, nextX())) {
                return Widgets.clickMenuButton(menu.containerId, ApertureStorageMenu.BUTTON_NEXT);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
