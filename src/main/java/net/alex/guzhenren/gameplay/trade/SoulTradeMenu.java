package net.alex.guzhenren.gameplay.trade;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The soul trader's offer list: one clickable row per {@link SoulTradeOffer} above the player's own inventory.
 *
 * <p>There are no trade slots. The client learns the offers from the menu-open payload
 * ({@link #writeOffers}); a click arrives as a vanilla menu-button press whose id is the offer index, and
 * {@link #clickMenuButton} settles it against the player's bag on the server. The inventory slots are here so
 * that {@code broadcastChanges} syncs what a trade took and gave while the screen stays open.
 *
 * <p>⚠ The row button stays live when the bag cannot pay: the refusal has to reach the server so the soul can
 * shake its head, and the server is the only judge anyway. Before settling, everything that will later fall
 * back into the bag -- the cursor stack of this menu and the player's 2x2 crafting grid -- joins the room check
 * ({@link SoulTradeOffer}); a refusal for room tells the player to make space first.
 *
 * <p>The menu closes once the trader dies or the player is more than {@code TRADE_RANGE} away.
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulTraderEntity
 * @since 1.0.0
 */

public class SoulTradeMenu extends AbstractContainerMenu {

    public static final int SLOT = 18;
    public static final int ROWS_Y = 20;
    public static final int ROW_H = 22;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_COLS = 9;
    public static final int INVENTORY_ROWS = 3;
    private static final int INVENTORY_LABEL_GAP = 14;
    private static final int HOTBAR_GAP = 4;
    private static final int INVENTORY_SLOTS = INVENTORY_COLS * INVENTORY_ROWS;
    private static final String SHORT = "guzhenren.menu.soul_trade.short";
    private static final String NO_ROOM = "guzhenren.menu.soul_trade.no_room";
    private final Player player;
    private final @Nullable SoulTraderEntity trader;
    private final List<SoulTradeOffer> offers;

    public SoulTradeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, null, SoulTradeOffer.LIST_STREAM_CODEC.decode(data));
    }

    public SoulTradeMenu(int id, Inventory inventory, SoulTraderEntity trader) {
        this(id, inventory, trader, trader.trader().offers());
    }

    private SoulTradeMenu(int id, Inventory inventory, @Nullable SoulTraderEntity trader, List<SoulTradeOffer> offers) {
        super(ModMenus.SOUL_TRADE_MENU.get(), id);
        this.player = inventory.player;
        this.trader = trader;
        this.offers = List.copyOf(offers);

        int top = inventoryY(this.offers.size());
        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < INVENTORY_COLS; col++) {
                addSlot(new Slot(inventory, col + row * INVENTORY_COLS + INVENTORY_COLS,
                        INVENTORY_X + col * SLOT, top + row * SLOT));
            }
        }
        for (int col = 0; col < INVENTORY_COLS; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * SLOT, hotbarY(this.offers.size())));
        }
    }

    public static void writeOffers(RegistryFriendlyByteBuf buffer, List<SoulTradeOffer> offers) {
        SoulTradeOffer.LIST_STREAM_CODEC.encode(buffer, offers);
    }

    public static int inventoryY(int offerCount) { return ROWS_Y + offerCount * ROW_H + INVENTORY_LABEL_GAP; }

    public static int hotbarY(int offerCount) { return inventoryY(offerCount) + INVENTORY_ROWS * SLOT + HOTBAR_GAP; }

    public List<SoulTradeOffer> offers() { return this.offers; }

    @Override
    public boolean clickMenuButton(@NotNull Player who, int id) {
        if (who != this.player || !(who instanceof ServerPlayer server) || this.trader == null) return false;
        if (id < 0 || id >= this.offers.size()) return false;

        SoulTradeOffer.Outcome outcome = this.offers.get(id).settle(server.getInventory(), this.returning(server));
        switch (outcome) {
            case SHORT -> say(server, SHORT);
            case NO_ROOM -> say(server, NO_ROOM);
            case DONE -> {}
        }
        this.trader.reactToTrade(outcome == SoulTradeOffer.Outcome.DONE);
        return true;
    }

    private List<ItemStack> returning(ServerPlayer server) {
        List<ItemStack> returning = new ArrayList<>();
        returning.add(this.getCarried());
        returning.addAll(server.inventoryMenu.getCraftSlots().getItems());
        return returning;
    }

    private static void say(ServerPlayer who, String key) {
        who.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player who, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < INVENTORY_SLOTS
                ? this.moveItemStackTo(stack, INVENTORY_SLOTS, this.slots.size(), false)
                : this.moveItemStackTo(stack, 0, INVENTORY_SLOTS, false);
        if (!moved) return ItemStack.EMPTY;

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(@NotNull Player who) {
        if (this.trader == null) return true;

        return who == this.player && this.trader.isAlive() && this.trader.level() == who.level()
                && who.distanceToSqr(this.trader) <= SoulTraderEntity.TRADE_RANGE * SoulTraderEntity.TRADE_RANGE;
    }
}
