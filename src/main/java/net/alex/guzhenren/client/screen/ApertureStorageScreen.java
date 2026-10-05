package net.alex.guzhenren.client.screen;

import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageMenu;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.minecraft.client.Minecraft;
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
 * closes the container first, then opens the B panel. All drawing is {@code g.fill}, no textures.
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
    private static final int SLOT_SIZE = 16;
    private static final int VITAL_PADDING = 8;
    private static final int VITAL_LEFT = ApertureStorageMenu.VITAL_X - VITAL_PADDING;
    private static final int VITAL_RIGHT = ApertureStorageMenu.VITAL_X + SLOT_SIZE + VITAL_PADDING;
    private static final int VITAL_BOTTOM = 44;
    private static final String VITAL_KEY = "guzhenren.menu.vital";
    private static final int BACK_W = 16;
    private static final String BACK_GLYPH = "<-";
    private static final int TITLE_X_WITH_BACK = 26;
    private static final String LOAD_KEY = "guzhenren.menu.aperture_load";

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
        graphics.fill(x, y, x + imageWidth, y + imageHeight, ModPalette.PANEL_FILL);
        graphics.renderOutline(x, y, imageWidth, imageHeight, ModPalette.BORDER);
        graphics.fill(x + 7, y + 15, x + imageWidth - 7, y + 16, ModPalette.APERTURE);

        int left = x + ApertureStorageMenu.STORAGE_X;
        fillSlotGrid(graphics, left, y + ApertureStorageMenu.STORAGE_Y, ApertureStorageMenu.ROWS);
        fillSlotGrid(graphics, left, y + ApertureStorageMenu.INVENTORY_Y, ApertureStorageMenu.INVENTORY_ROWS);
        fillSlotGrid(graphics, left, y + ApertureStorageMenu.HOTBAR_Y, 1);
        renderVital(graphics, x, y);
    }

    private void fillSlotGrid(GuiGraphics graphics, int left, int top, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < ApertureStorageMenu.COLS; col++) {
                fillSlot(graphics, left + col * ApertureStorageMenu.SLOT, top + row * ApertureStorageMenu.SLOT);
            }
        }
    }

    private void fillSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, ModPalette.SLOT_FILL);
    }

    private void renderVital(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + VITAL_LEFT, y, x + VITAL_RIGHT, y + VITAL_BOTTOM, ModPalette.PANEL_FILL);
        graphics.renderOutline(x + VITAL_LEFT, y, VITAL_RIGHT - VITAL_LEFT, VITAL_BOTTOM, ModPalette.BORDER);
        graphics.fill(x + VITAL_LEFT + 4, y + 15, x + VITAL_RIGHT - 4, y + 16, ModPalette.APERTURE);

        Component label = Component.translatable(VITAL_KEY);
        int width = VITAL_RIGHT - VITAL_LEFT;
        graphics.drawString(font, label, x + VITAL_LEFT + (width - font.width(label)) / 2, y + 5,
                ModPalette.APERTURE, false);
        fillSlot(graphics, x + ApertureStorageMenu.VITAL_X, y + ApertureStorageMenu.VITAL_Y);
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
        renderBack(graphics, mouseX, mouseY);
        renderPageButton(graphics, mouseX, mouseY, prevX(), pagerBottomY(), menu.getPageIndex() > 0, "<");
        renderPageButton(graphics, mouseX, mouseY, nextX(), pagerBottomY(),
                menu.getPageIndex() + 1 < menu.getPageCount(), ">");

        Component page = Component.literal((menu.getPageIndex() + 1) + " / " + menu.getPageCount());
        graphics.drawString(font, page, labelX() + (PAGE_LABEL_W - font.width(page)) / 2,
                pagerBottomY() + (PAGE_BUTTON_H - font.lineHeight) / 2 + 1, ModPalette.TEXT, false);

        Component load = Component.translatable(LOAD_KEY, menu.getLoad(), ApertureStorageService.MAX_LOAD);
        graphics.drawString(font, load, leftPos + imageWidth - font.width(load), pagerBottomY() + 1,
                ModPalette.APERTURE, false);
    }

    private void renderPageButton(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, boolean live,
                                  String glyph) {
        boolean hover = live && inButton(mouseX, mouseY, x, y, PAGE_BUTTON_W);
        graphics.fill(x, y, x + PAGE_BUTTON_W, y + PAGE_BUTTON_H,
                live ? (hover ? ModPalette.BUTTON_HOVER : ModPalette.BUTTON_IDLE) : ModPalette.BUTTON_DEAD);
        graphics.drawString(font, glyph, x + (PAGE_BUTTON_W - font.width(glyph)) / 2,
                y + (PAGE_BUTTON_H - font.lineHeight) / 2 + 1, live ? ModPalette.TEXT : ModPalette.BUTTON_IDLE,
                false);
    }

    private void renderBack(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = backX();
        int y = pagerY();
        boolean hover = inBackButton(mouseX, mouseY);
        graphics.fill(x, y, x + BACK_W, y + PAGE_BUTTON_H,
                hover ? ModPalette.BUTTON_HOVER : ModPalette.BUTTON_IDLE);
        graphics.drawString(font, BACK_GLYPH, x + (BACK_W - font.width(BACK_GLYPH)) / 2,
                y + (PAGE_BUTTON_H - font.lineHeight) / 2 + 1, ModPalette.TEXT, false);
    }

    private int pagerY() { return topPos + 3; }

    private int pagerBottomY() { return topPos + imageHeight + 4; }

    private int backX() { return leftPos + 7; }

    private int prevX() { return leftPos + 7; }

    private int labelX() { return prevX() + PAGE_BUTTON_W; }

    private int nextX() { return labelX() + PAGE_LABEL_W; }

    private boolean inBackButton(double mouseX, double mouseY) {
        return inButton(mouseX, mouseY, backX(), pagerY(), BACK_W);
    }

    private boolean inPageButton(double mouseX, double mouseY, int x) {
        return inButton(mouseX, mouseY, x, pagerBottomY(), PAGE_BUTTON_W);
    }

    private boolean inButton(double mouseX, double mouseY, int x, int y, int width) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + PAGE_BUTTON_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inBackButton(mouseX, mouseY)) return clickBack();
            if (inPageButton(mouseX, mouseY, prevX())) return clickPage(ApertureStorageMenu.BUTTON_PREV);
            if (inPageButton(mouseX, mouseY, nextX())) return clickPage(ApertureStorageMenu.BUTTON_NEXT);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickBack() {
        onClose();
        Minecraft.getInstance().setScreen(new PlayerInfoScreen());
        return true;
    }

    private boolean clickPage(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null) return false;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }
}
