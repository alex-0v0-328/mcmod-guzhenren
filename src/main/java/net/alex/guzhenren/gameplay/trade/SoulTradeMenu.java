package net.alex.guzhenren.gameplay.trade;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.MenuSlots;
import net.alex.guzhenren.core.Refusals;
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
 * 交易魂的报价列表容器菜单：在玩家背包上方为每个 {@link SoulTradeOffer} 渲染一行可点击报价。
 * <ul>
 *   <li>无交易物品槽位：报价列表随打开菜单时的 payload（{@link #writeOffers}）下发客户端；点击行按钮即触发原版
 *       菜单按钮（{@link #clickMenuButton}，button id 为报价下标），由服务端直接从玩家主背包扣费与发放物
 *       品，背包槽位用于界面开启期间通过 {@code broadcastChanges} 同步增减。</li>
 *   <li>⚠ 玩家材料不足时客户端行按钮仍保持可点击：拒绝提示与 {@link SoulTraderEntity} 的摇头反应必须在服务端
 *       触发，服务端是交易成败的唯一裁判。</li>
 *   <li>⚠ 结算空间时计入关闭菜单后将落回背包的光标物品与 2×2 合成格物品（{@link SoulTradeOffer#settle}），放不
 *       下则提示背包已满并拒绝成交；当交易魂死亡、双方跨维度或距离超过 {@link SoulTraderEntity#TRADE_RANGE}
 *       时菜单自动关闭。</li>
 * </ul>
 */

public class SoulTradeMenu extends AbstractContainerMenu {

    public static final int SLOT = 18;
    public static final int ROWS_Y = 24;
    public static final int ROW_H = 30;
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
            case NO_ROOM -> ActionBar.refuse(server, Refusals.BAG_FULL);
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
