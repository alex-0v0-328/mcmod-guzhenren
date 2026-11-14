package net.alex.guzhenren.gameplay.trade;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.MenuSlots;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 交易魂的报价列表：背包上方每个 {@link SoulTradeOffer} 一行可点。
 * <ul>
 *   <li>无交易槽：报价随开菜单 payload（{@link #writeOffers}）下发；点击即原版菜单按钮，id 为报价下标，
 *       {@link #clickMenuButton} 在服务端按背包结算；背包槽供 {@code broadcastChanges} 在界面开着时同步增减。</li>
 *   <li>⚠ 付不起时行按钮仍可点：拒绝须到服务端，魂才会摇头；服务端本是唯一裁判。</li>
 *   <li>⚠ 结算前，之后落回背包的光标物品与 2x2 合成格计入空间检查（{@link SoulTradeOffer}），因空间被拒提示背包已满；交
 *       易魂死亡或玩家远于 {@code TRADE_RANGE} 即关闭。</li>
 * </ul>
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
    private final Player player;
    private final @Nullable SoulTraderEntity trader;
    private final List<SoulTradeOffer> offers;

    public SoulTradeMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, null, SoulTradeOffer.decodeList(data));
    }

    public SoulTradeMenu(int id, Inventory inventory, SoulTraderEntity trader) {
        this(id, inventory, trader, trader.getTrader().getOffers());
    }

    private SoulTradeMenu(int id, Inventory inventory, @Nullable SoulTraderEntity trader, List<SoulTradeOffer> offers) {
        super(ModMenus.SOUL_TRADE_MENU.get(), id);
        this.player = inventory.player;
        this.trader = trader;
        this.offers = List.copyOf(offers);

        MenuSlots.addPlayerInventory(this::addSlot, inventory, INVENTORY_X, getInventoryY(this.offers.size()),
                getHotbarY(this.offers.size()), SLOT);
    }

    public static void writeOffers(FriendlyByteBuf buffer, List<SoulTradeOffer> offers) {
        SoulTradeOffer.encodeList(buffer, offers);
    }

    public static int getInventoryY(int offerCount) { return ROWS_Y + offerCount * ROW_H + INVENTORY_LABEL_GAP; }

    public static int getHotbarY(int offerCount) {
        return getInventoryY(offerCount) + INVENTORY_ROWS * SLOT + HOTBAR_GAP;
    }

    public List<SoulTradeOffer> getOffers() { return this.offers; }

    @Override
    public boolean clickMenuButton(@NotNull Player who, int id) {
        if (who != this.player || !(who instanceof ServerPlayer server) || this.trader == null) return false;
        if (id < 0 || id >= this.offers.size()) return false;

        SoulTradeOffer.Outcome outcome =
                this.offers.get(id).settle(server.getInventory(), this.collectReturning(server));
        switch (outcome) {
            case SHORT -> ActionBar.refuse(server, SHORT);
            case NO_ROOM -> ActionBar.refuse(server, Refusal.BAG_FULL);
            case DONE -> {}
        }
        this.trader.reactToTrade(outcome == SoulTradeOffer.Outcome.DONE);
        return true;
    }

    private List<ItemStack> collectReturning(ServerPlayer server) {
        List<ItemStack> returning = new ArrayList<>();
        returning.add(this.getCarried());
        returning.addAll(server.inventoryMenu.getCraftSlots().getItems());
        return returning;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player who, int index) {
        return MenuSlots.quickMove(this.slots.get(index), stack -> index < INVENTORY_SLOTS
                ? this.moveItemStackTo(stack, INVENTORY_SLOTS, this.slots.size(), false)
                : this.moveItemStackTo(stack, 0, INVENTORY_SLOTS, false));
    }

    @Override
    public boolean stillValid(@NotNull Player who) {
        if (this.trader == null) return true;

        return who == this.player && this.trader.isAlive() && this.trader.level() == who.level()
                && who.distanceToSqr(this.trader) <= SoulTraderEntity.TRADE_RANGE * SoulTraderEntity.TRADE_RANGE;
    }
}
