package net.alex.guzhenren.client.screen;

import java.util.List;
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
 * @author Alex
 * @version 1.0.0
 * @see SoulTradeMenu
 * @since 1.0.0
 */

public class SoulTradeScreen extends AbstractContainerScreen<SoulTradeMenu> {

    private static final int WIDTH = 176;
    private static final int BOTTOM_MARGIN = 6;
    private static final int CELL = 16;
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
    private static final int SHORT_RED = 0xFFFF5555;
    private static final int SHORT_OUTLINE = 0x99FF5555;
    private static final String ARROW = "→";
    private static final String TRADE_KEY = "guzhenren.menu.soul_trade.trade";

    public SoulTradeScreen(SoulTradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = SoulTradeMenu.hotbarY(menu.offers().size()) + CELL + BOTTOM_MARGIN;
        this.inventoryLabelX = SoulTradeMenu.INVENTORY_X;
        this.inventoryLabelY = SoulTradeMenu.inventoryY(menu.offers().size()) - 10;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, ModPalette.PANEL_FILL);
        graphics.renderOutline(x, y, this.imageWidth, this.imageHeight, ModPalette.BORDER);

        List<SoulTradeOffer> offers = this.menu.offers();
        for (int i = 0; i < offers.size(); i++) {
            int rowY = y + rowY(i);
            graphics.fill(x + ROW_X, rowY, x + ROW_X + ROW_W, rowY + ROW_FILL_H, ROW_FILL);
            drawButton(graphics, x + BUTTON_X, rowY + ICON_PAD, this.affordable(offers.get(i)),
                    this.inButton(i, mouseX, mouseY));
        }
        int inventoryY = y + SoulTradeMenu.inventoryY(offers.size());
        int hotbarY = y + SoulTradeMenu.hotbarY(offers.size());
        for (int col = 0; col < SoulTradeMenu.INVENTORY_COLS; col++) {
            int cellX = x + SoulTradeMenu.INVENTORY_X + col * SoulTradeMenu.SLOT;
            for (int row = 0; row < SoulTradeMenu.INVENTORY_ROWS; row++) {
                int cellY = inventoryY + row * SoulTradeMenu.SLOT;
                graphics.fill(cellX, cellY, cellX + CELL, cellY + CELL, ModPalette.SLOT_FILL);
            }
            graphics.fill(cellX, hotbarY, cellX + CELL, hotbarY + CELL, ModPalette.SLOT_FILL);
        }
    }

    private static void drawButton(GuiGraphics graphics, int x, int y, boolean affordable, boolean hover) {
        int fill = hover ? ModPalette.BUTTON_HOVER : affordable ? ModPalette.BUTTON_IDLE : ModPalette.BUTTON_DEAD;
        graphics.fill(x, y, x + BUTTON_W, y + BUTTON_H, fill);
        graphics.renderOutline(x, y, BUTTON_W, BUTTON_H, affordable ? ModPalette.BORDER : SHORT_OUTLINE);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY,
                ModPalette.TREASURE_YELLOW_HEAVEN, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                ModPalette.TEXT, false);

        List<ItemStack> bag = this.bag();
        List<SoulTradeOffer> offers = this.menu.offers();
        for (int i = 0; i < offers.size(); i++) {
            SoulTradeOffer offer = offers.get(i);
            int iconY = rowY(i) + ICON_PAD;
            List<ItemCost> costs = offer.costs();
            for (int n = 0; n < costs.size(); n++) {
                ItemCost cost = costs.get(n);
                int iconX = COST_X + n * COST_STRIDE;
                graphics.renderFakeItem(cost.itemStack(), iconX, iconY);
                boolean enough = SoulTradeOffer.held(bag, cost) >= cost.count();
                drawCount(graphics, iconX, iconY, cost.count(), enough ? ModPalette.TEXT : SHORT_RED);
            }
            graphics.drawString(this.font, ARROW, ARROW_X, iconY + 4, ModPalette.TEXT, false);

            ItemStack result = offer.result();
            graphics.renderFakeItem(result, RESULT_X, iconY);
            drawCount(graphics, RESULT_X, iconY, result.getCount(), ModPalette.TEXT);

            Component label = Component.translatable(TRADE_KEY);
            int labelColour = offer.affordable(bag) ? ModPalette.TEXT : SHORT_RED;
            graphics.drawString(this.font, label, BUTTON_X + (BUTTON_W - this.font.width(label)) / 2, iconY + 4,
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
                if (this.inButton(i, mouseX, mouseY)) return this.send(i);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean send(int offer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null) return false;

        minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, offer);
        return true;
    }

    private boolean inButton(int offer, double mouseX, double mouseY) {
        int x = this.leftPos + BUTTON_X;
        int y = this.topPos + rowY(offer) + ICON_PAD;
        return mouseX >= x && mouseX < x + BUTTON_W && mouseY >= y && mouseY < y + BUTTON_H;
    }

    private boolean over(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
    }

    private boolean affordable(SoulTradeOffer offer) { return offer.affordable(this.bag()); }

    private List<ItemStack> bag() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? List.of() : minecraft.player.getInventory().items;
    }

    private static int rowY(int offer) { return SoulTradeMenu.ROWS_Y + offer * SoulTradeMenu.ROW_H; }
}
