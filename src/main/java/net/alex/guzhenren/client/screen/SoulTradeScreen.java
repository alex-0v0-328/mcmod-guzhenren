package net.alex.guzhenren.client.screen;

import java.util.List;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.gameplay.trade.SoulTradeMenu;
import net.alex.guzhenren.gameplay.trade.SoulTradeOffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import org.jetbrains.annotations.NotNull;

/**
 * The soul trader's screen: one row per offer -- costs, an arrow, the result and a trade button -- above the
 * player's inventory. Drawn from fills and the shared {@link ModPalette}, like the refinement screen, so it
 * needs no texture.
 *
 * <p>A cost count turns red while the bag holds less than it asks, and the button then wears the refusal
 * outline, but it stays clickable: the server judges and the soul answers with a head shake. Icons, counts and
 * button labels are drawn from {@code renderLabels}, so the carried item still paints over them.
 *
 * <p>The bag is counted once per frame in {@link #render}, into {@code enough} per cost and {@code affordable} per
 * offer, which the background and the labels both read; the background is one {@link GuiBatch}.
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulTradeMenu
 * @since 1.0.0
 */

public class SoulTradeScreen extends AbstractContainerScreen<SoulTradeMenu> {

    private static final int WIDTH = 176;
    private static final int BOTTOM_MARGIN = 6;
    private static final int ROW_X = 8;
    private static final int ROW_W = 160;
    private static final int ROW_FILL_H = 20;
    private static final int ICON_PAD = 2;
    private static final int COST_X = 12;
    private static final int COST_STRIDE = 20;
    private static final int ARROW_X = 74;
    private static final int RESULT_X = 90;
    private static final int BUTTON_X = 124;
    private static final int BUTTON_W = 40;
    private static final int BUTTON_H = 16;
    private static final int COUNT_Z = 200;
    private static final int COUNT_RIGHT = 17;
    private static final int COUNT_TOP = 9;
    private static final int ROW_FILL = 0x1AFFFFFF;
    private static final String ARROW = "→";
    private static final Component TRADE = Component.translatable("guzhenren.menu.soul_trade.trade");
    private final boolean[] affordable;
    private final boolean[][] enough;

    public SoulTradeScreen(SoulTradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = SoulTradeMenu.hotbarY(menu.offers().size()) + Widgets.CELL + BOTTOM_MARGIN;
        this.inventoryLabelX = SoulTradeMenu.INVENTORY_X;
        this.inventoryLabelY = SoulTradeMenu.inventoryY(menu.offers().size()) - 10;
        this.affordable = new boolean[menu.offers().size()];
        this.enough = new boolean[menu.offers().size()][];
        for (int i = 0; i < enough.length; i++) enough[i] = new boolean[menu.offers().get(i).costs().size()];
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        List<SoulTradeOffer> offers = this.menu.offers();
        GuiBatch.draw(graphics, () -> {
            Widgets.drawPanel(graphics, x, y, this.imageWidth, this.imageHeight, ModPalette.PANEL_FILL);

            for (int i = 0; i < offers.size(); i++) {
                int rowY = y + rowY(i);
                graphics.fill(x + ROW_X, rowY, x + ROW_X + ROW_W, rowY + ROW_FILL_H, ROW_FILL);
                drawButton(graphics, x + BUTTON_X, rowY + ICON_PAD, this.affordable[i],
                        this.inButton(i, mouseX, mouseY));
            }
            int inventoryX = x + SoulTradeMenu.INVENTORY_X;
            Widgets.drawCells(graphics, inventoryX, y + SoulTradeMenu.inventoryY(offers.size()),
                    SoulTradeMenu.INVENTORY_COLS, SoulTradeMenu.INVENTORY_ROWS, SoulTradeMenu.SLOT,
                    ModPalette.SLOT_FILL);
            Widgets.drawCells(graphics, inventoryX, y + SoulTradeMenu.hotbarY(offers.size()),
                    SoulTradeMenu.INVENTORY_COLS, 1, SoulTradeMenu.SLOT, ModPalette.SLOT_FILL);
        });
    }

    private static void drawButton(GuiGraphics graphics, int x, int y, boolean affordable, boolean hover) {
        int fill = hover ? ModPalette.BUTTON_HOVER : affordable ? ModPalette.BUTTON_IDLE : ModPalette.BUTTON_DEAD;
        graphics.fill(x, y, x + BUTTON_W, y + BUTTON_H, fill);
        graphics.renderOutline(x, y, BUTTON_W, BUTTON_H, affordable ? ModPalette.BORDER : ModPalette.SHORT_OUTLINE);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY,
                ModPalette.TREASURE_YELLOW_HEAVEN, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                ModPalette.TEXT, false);

        List<SoulTradeOffer> offers = this.menu.offers();
        for (int i = 0; i < offers.size(); i++) {
            SoulTradeOffer offer = offers.get(i);
            int iconY = rowY(i) + ICON_PAD;
            List<ItemCost> costs = offer.costs();
            for (int n = 0; n < costs.size(); n++) {
                ItemCost cost = costs.get(n);
                int iconX = COST_X + n * COST_STRIDE;
                graphics.renderFakeItem(cost.itemStack(), iconX, iconY);
                drawCount(graphics, iconX, iconY, cost.count(), this.enough[i][n] ? ModPalette.TEXT : ModPalette.SHORT);
            }
            graphics.drawString(this.font, ARROW, ARROW_X, iconY + 4, ModPalette.TEXT, false);

            ItemStack result = offer.result();
            graphics.renderFakeItem(result, RESULT_X, iconY);
            drawCount(graphics, RESULT_X, iconY, result.getCount(), ModPalette.TEXT);

            int labelColour = this.affordable[i] ? ModPalette.TEXT : ModPalette.SHORT;
            graphics.drawString(this.font, TRADE, BUTTON_X + (BUTTON_W - this.font.width(TRADE)) / 2, iconY + 4,
                    labelColour, false);
        }
    }

    private void drawCount(GuiGraphics graphics, int x, int y, int count, int colour) {
        if (count <= 1) return;

        String text = String.valueOf(count);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, COUNT_Z);
        graphics.drawString(this.font, text, x + COUNT_RIGHT - this.font.width(text), y + COUNT_TOP, colour, true);
        graphics.pose().popPose();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.refreshAffordable();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.menu.getCarried().isEmpty()) this.renderOfferTooltip(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderOfferTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<SoulTradeOffer> offers = this.menu.offers();
        for (int i = 0; i < offers.size(); i++) {
            int iconY = this.topPos + rowY(i) + ICON_PAD;
            List<ItemCost> costs = offers.get(i).costs();
            for (int n = 0; n < costs.size(); n++) {
                if (this.over(this.leftPos + COST_X + n * COST_STRIDE, iconY, mouseX, mouseY)) {
                    graphics.renderTooltip(this.font, costs.get(n).itemStack(), mouseX, mouseY);
                    return;
                }
            }
            if (this.over(this.leftPos + RESULT_X, iconY, mouseX, mouseY)) {
                graphics.renderTooltip(this.font, offers.get(i).result(), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < this.menu.offers().size(); i++) {
                if (this.inButton(i, mouseX, mouseY)) return Widgets.clickMenuButton(this.menu.containerId, i);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inButton(int offer, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, this.leftPos + BUTTON_X, this.topPos + rowY(offer) + ICON_PAD,
                BUTTON_W, BUTTON_H);
    }

    private boolean over(int x, int y, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, x, y, Widgets.CELL, Widgets.CELL);
    }

    private void refreshAffordable() {
        List<ItemStack> bag = this.bag();
        List<SoulTradeOffer> offers = this.menu.offers();
        for (int i = 0; i < offers.size(); i++) {
            List<ItemCost> costs = offers.get(i).costs();
            boolean all = true;
            for (int n = 0; n < costs.size(); n++) {
                this.enough[i][n] = SoulTradeOffer.held(bag, costs.get(n)) >= costs.get(n).count();
                all &= this.enough[i][n];
            }
            this.affordable[i] = all;
        }
    }

    private List<ItemStack> bag() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? List.of() : minecraft.player.getInventory().items;
    }

    private static int rowY(int offer) { return SoulTradeMenu.ROWS_Y + offer * SoulTradeMenu.ROW_H; }
}
