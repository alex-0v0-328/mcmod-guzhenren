package net.alex.guzhenren.client.screen;

import java.util.List;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.gameplay.trade.SoulTradeMenu;
import net.alex.guzhenren.gameplay.trade.SoulTradeOffer;
import net.alex.guzhenren.gameplay.trade.TradeCost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 灵魂交易 NPC 的交易界面。
 * <ul>
 *   <li>每条报价一行（成本、箭头、结果、交易按钮），在背包上方；同炼蛊界面用 {@link Widgets} sprite 和
 *       {@link ModPalette}。</li>
 *   <li>背包不够某项成本时该数量变红，按钮画成失效、标签变红但仍可点：由服务端裁定，NPC 摇头回应。</li>
 *   <li>{@link #render} 每帧数背包，写各成本 {@code enough}、各报价 {@code affordable}，背景、标签共读。</li>
 *   <li>图标、数量、按钮标签从 {@code renderLabels} 画，手持物品仍盖其上；背景一个 {@link GuiBatch}：先面板和格子，
 *       再叠行填色，最后画按钮。</li>
 * </ul>
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
        this.imageHeight = SoulTradeMenu.getHotbarY(menu.getOffers().size()) + Widgets.CELL + BOTTOM_MARGIN;
        this.inventoryLabelX = SoulTradeMenu.INVENTORY_X;
        this.inventoryLabelY = SoulTradeMenu.getInventoryY(menu.getOffers().size()) - 10;
        this.affordable = new boolean[menu.getOffers().size()];
        this.enough = new boolean[menu.getOffers().size()][];
        for (int i = 0; i < enough.length; i++) enough[i] = new boolean[menu.getOffers().get(i).costs().size()];
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        List<SoulTradeOffer> offers = this.menu.getOffers();
        GuiBatch.draw(graphics, () -> {
            SpriteBatch.draw(graphics, sprites -> {
                Widgets.drawPanel(sprites, x, y, this.imageWidth, this.imageHeight);
                int inventoryX = x + SoulTradeMenu.INVENTORY_X;
                Widgets.drawCells(sprites, inventoryX, y + SoulTradeMenu.getInventoryY(offers.size()),
                        SoulTradeMenu.INVENTORY_COLS, SoulTradeMenu.INVENTORY_ROWS, SoulTradeMenu.SLOT);
                Widgets.drawCells(sprites, inventoryX, y + SoulTradeMenu.getHotbarY(offers.size()),
                        SoulTradeMenu.INVENTORY_COLS, 1, SoulTradeMenu.SLOT);
            });
            for (int i = 0; i < offers.size(); i++) {
                int rowY = y + getRowY(i);
                graphics.fill(x + ROW_X, rowY, x + ROW_X + ROW_W, rowY + ROW_FILL_H, ROW_FILL);
            }
            SpriteBatch.draw(graphics, sprites -> {
                for (int i = 0; i < offers.size(); i++) {
                    boolean hover = this.isInButton(i, mouseX, mouseY);
                    Widgets.drawButton(sprites, x + BUTTON_X, y + getRowY(i) + ICON_PAD, BUTTON_W, BUTTON_H,
                            this.affordable[i] || hover, hover);
                }
            });
        });
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY,
                ModPalette.TREASURE_YELLOW_HEAVEN, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                ModPalette.TEXT, false);

        List<SoulTradeOffer> offers = this.menu.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            SoulTradeOffer offer = offers.get(i);
            int iconY = getRowY(i) + ICON_PAD;
            List<TradeCost> costs = offer.costs();
            for (int n = 0; n < costs.size(); n++) {
                TradeCost cost = costs.get(n);
                int iconX = COST_X + n * COST_STRIDE;
                graphics.renderFakeItem(cost.createItemStack(), iconX, iconY);
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
        List<SoulTradeOffer> offers = this.menu.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            int iconY = this.topPos + getRowY(i) + ICON_PAD;
            List<TradeCost> costs = offers.get(i).costs();
            for (int n = 0; n < costs.size(); n++) {
                if (this.isOver(this.leftPos + COST_X + n * COST_STRIDE, iconY, mouseX, mouseY)) {
                    graphics.renderTooltip(this.font, costs.get(n).createItemStack(), mouseX, mouseY);
                    return;
                }
            }
            if (this.isOver(this.leftPos + RESULT_X, iconY, mouseX, mouseY)) {
                graphics.renderTooltip(this.font, offers.get(i).result(), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < this.menu.getOffers().size(); i++) {
                if (this.isInButton(i, mouseX, mouseY)) return Widgets.clickMenuButton(this.menu.containerId, i);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInButton(int offer, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, this.leftPos + BUTTON_X, this.topPos + getRowY(offer) + ICON_PAD,
                BUTTON_W, BUTTON_H);
    }

    private boolean isOver(int x, int y, double mouseX, double mouseY) {
        return Widgets.isOver(mouseX, mouseY, x, y, Widgets.CELL, Widgets.CELL);
    }

    private void refreshAffordable() {
        List<ItemStack> bag = this.collectBag();
        List<SoulTradeOffer> offers = this.menu.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            List<TradeCost> costs = offers.get(i).costs();
            boolean all = true;
            for (int n = 0; n < costs.size(); n++) {
                this.enough[i][n] = SoulTradeOffer.countHeld(bag, costs.get(n)) >= costs.get(n).count();
                all &= this.enough[i][n];
            }
            this.affordable[i] = all;
        }
    }

    private List<ItemStack> collectBag() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? List.of() : minecraft.player.getInventory().items;
    }

    private static int getRowY(int offer) { return SoulTradeMenu.ROWS_Y + offer * SoulTradeMenu.ROW_H; }
}
