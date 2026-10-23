package net.alex.guzhenren.gameplay.aperture.storage;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ItemStacks;
import net.alex.guzhenren.core.MenuSlots;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The container behind one aperture's [空窍] store, paged because the store outgrows one screen: up to
 * {@code ApertureStorageService.MAX_LOAD} load, at least one per Gu.
 *
 * <p>Extends {@link net.minecraft.world.inventory.AbstractContainerMenu}. 54 slots per page; the Vital Gu
 * [本命蛊] slot sits outside the pager, past {@code imageWidth}. The save trigger is a container
 * listener ({@code page.addListener(container -> save())}), not an override of {@code slotsChanged} -- that
 * override is never called because {@code AbstractContainerMenu} is not a {@code ContainerListener}. ⚠ It
 * saves on every changed slot, not once per click: the slots admit a Gu by {@code getLoad()}, which only a save
 * refreshes, so a deferred save would let one shift-click or drag over-fill the page and lose the refused Gu.
 *
 * <p>{@code countPages} counts the pages that hold every stored Gu plus one free slot, so a full page
 * still leaves an empty one for the next Gu. Binding a Vital Gu here only marks the stack;
 * {@code ApertureStorageService.setVital} then sets the aperture's primary path.
 *
 * <p>⚠ The heartbeat calls {@code refresh} after the upkeep walk: any second can heal, feed or fill a
 * stored Gu, and an open menu that saved its stale view back would undo it -- or resurrect a Gu that
 * starved a moment earlier.
 *
 * <p>⚠ {@link VitalSlot#mayPlace} admits a Gu by the load after the swap, mouse and number key alike: a
 * refused {@code setVital} in {@code save} reloads the slot but cannot take back the copy vanilla already
 * moved to the cursor or hotbar, which would duplicate the old Vital Gu and lose the new one.
 *
 * <p>An aperture has one Vital Gu. Binding a new one makes the replaced one an ordinary Gu: its token no
 * longer matches the store, and {@link #releaseFormer} strips the mark from the copy vanilla just moved to
 * the cursor or into the bag.
 *
 * <p>⚠ A command that rewrites the store under an open menu ({@code /gzr reset}, {@code /gzr awaken}) calls
 * {@link #closeFor} first: before the next heartbeat refresh, a click or the close would save the stale window
 * back over the reset or into the shifted indices.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureStorageService
 * @since 1.0.0
 */

public class ApertureStorageMenu extends AbstractContainerMenu {

    public static final int COLS = 9;
    public static final int ROWS = 6;
    public static final int INVENTORY_ROWS = 3;
    public static final int PAGE_SIZE = COLS * ROWS;
    public static final int VITAL_SLOT = PAGE_SIZE + Inventory.INVENTORY_SIZE;
    public static final int BUTTON_PREV = 0;
    public static final int BUTTON_NEXT = 1;
    public static final int SLOT = 18;
    public static final int STORAGE_X = 8;
    public static final int STORAGE_Y = 18;
    public static final int INVENTORY_Y = 140;
    public static final int HOTBAR_Y = 198;
    public static final int VITAL_X = 186;
    public static final int VITAL_Y = 22;
    private static final int DATA_PAGE = 0;
    private static final int DATA_PAGES = 1;
    private static final int DATA_LOAD = 2;
    private final Player player;
    private final int aperture;
    private final SimpleContainer page = new SimpleContainer(PAGE_SIZE);
    private final SimpleContainer vital = new SimpleContainer(1);
    private final ContainerData pageData = new SimpleContainerData(3);
    private boolean loading;

    public ApertureStorageMenu(int id, Inventory inventory, int aperture, int pageIndex) {
        super(ModMenus.APERTURE_STORAGE_MENU.get(), id);
        this.player = inventory.player;
        this.aperture = aperture;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                addSlot(new GuSlot(page, row * COLS + col,
                        STORAGE_X + col * SLOT, STORAGE_Y + row * SLOT));
            }
        }
        MenuSlots.addPlayerInventory(this::addSlot, inventory, STORAGE_X, INVENTORY_Y, HOTBAR_Y, SLOT);
        addSlot(new VitalSlot(VITAL_X, VITAL_Y));

        page.addListener(container -> save());
        vital.addListener(container -> save());

        addDataSlots(pageData);
        load(pageIndex);
    }

    public static void closeFor(@NotNull ServerPlayer player) {
        if (player.containerMenu instanceof ApertureStorageMenu) player.closeContainer();
    }

    public int getPageIndex() { return pageData.get(DATA_PAGE); }

    public int getPageCount() { return Math.max(1, pageData.get(DATA_PAGES)); }

    public int getLoad() { return pageData.get(DATA_LOAD); }

    public int getAperture() { return aperture; }

    private int countPages() {
        int count = ApertureStorageService.count(player, aperture);
        return count / PAGE_SIZE + 1;
    }

    //region paging
    @Override
    public boolean clickMenuButton(@NotNull Player who, int id) {
        int current = getPageIndex();
        int next = switch (id) {
            case BUTTON_PREV -> current - 1;
            case BUTTON_NEXT -> current + 1;
            default -> current;
        };
        if (next < 0 || next >= countPages() || next == current) return false;

        save();
        load(next);
        broadcastChanges();
        return true;
    }

    public void refresh() {
        if (!(player instanceof ServerPlayer server) || matchesStore(server)) return;

        load(getPageIndex());
        broadcastChanges();
    }

    private boolean matchesStore(ServerPlayer server) {
        return ApertureStorageService.matchesPage(server, aperture, getPageIndex() * PAGE_SIZE, window())
                && same(vital.getItem(0), ApertureStorageService.getVital(server, aperture))
                && getLoad() == ApertureStorageService.load(server, aperture)
                && pageData.get(DATA_PAGES) == countPages();
    }

    private List<ItemStack> window() {
        List<ItemStack> window = new ArrayList<>(PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) window.add(page.getItem(i).copy());
        return window;
    }

    private void load(int index) {
        loading = true;
        int at = Mth.clamp(index, 0, countPages() - 1);
        pageData.set(DATA_PAGE, at);
        pageData.set(DATA_PAGES, countPages());
        pageData.set(DATA_LOAD, ApertureStorageService.load(player, aperture));

        int from = at * PAGE_SIZE;
        List<ItemStack> items = ApertureStorageService.getPage(player, aperture, from, PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) {
            page.setItem(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        }
        vital.setItem(0, ApertureStorageService.getVital(player, aperture).copy());
        loading = false;
    }

    private void save() {
        if (loading || !(player instanceof ServerPlayer server)) return;

        List<ItemStack> window = window();
        int from = getPageIndex() * PAGE_SIZE;
        if (!ApertureStorageService.matchesPage(server, aperture, from, window)) {
            if (!ApertureStorageService.setPage(server, aperture, from, window)) {
                load(getPageIndex());
                return;
            }
        }

        ItemStack bound = vital.getItem(0);
        if (!bound.isEmpty()) ApertureStorageService.storedGu().bindVital(bound, server, aperture);
        if (!same(bound, ApertureStorageService.getVital(server, aperture))) {
            int kind = ApertureService.get(server).kindAt(aperture);
            long former = ApertureStorageService.getVitalToken(server, kind);
            if (!ApertureStorageService.setVital(server, aperture, bound.copy())) {
                load(getPageIndex());
                return;
            }
            if (former != 0L && former != ApertureStorageService.getVitalToken(server, kind)) {
                releaseFormer(server, former);
            }
        }

        pageData.set(DATA_PAGES, countPages());
        pageData.set(DATA_LOAD, ApertureStorageService.load(server, aperture));
    }

    private void releaseFormer(ServerPlayer server, long token) {
        StoredGu gu = ApertureStorageService.storedGu();
        if (gu.token(getCarried()) == token) gu.unbind(getCarried());
        Inventory inventory = server.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (gu.token(inventory.getItem(i)) == token) gu.unbind(inventory.getItem(i));
        }
    }

    private static boolean same(ItemStack first, ItemStack second) {
        return first.getCount() == second.getCount()
                && ItemStacks.sameItemSameData(first, second);
    }
    //endregion

    @Override
    public void removed(@NotNull Player who) {
        save();
        super.removed(who);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player who, int index) {
        boolean toInventory = index < PAGE_SIZE || index == VITAL_SLOT;
        return MenuSlots.quickMove(slots.get(index), stack -> toInventory
                ? moveItemStackTo(stack, PAGE_SIZE, VITAL_SLOT, true)
                : moveItemStackTo(stack, 0, PAGE_SIZE, false));
    }

    /**
     * Vanilla's shift-click merge, reading each slot's limit for the stack it is offered.
     *
     * <p>⚠ A Gu slot's limit depends on the stack ({@code GuSlot#getMaxStackSize(ItemStack)}); 1.20.1's own merge
     * reads only the stack-blind limit, so the vault would ignore its load cap there. This copy keeps both game
     * versions on the per-stack limit.
     */
    @Override
    protected boolean moveItemStackTo(@NotNull ItemStack stack, int start, int end, boolean reverse) {
        boolean moved = false;
        if (stack.isStackable()) {
            for (int i = reverse ? end - 1 : start; !stack.isEmpty() && (reverse ? i >= start : i < end);
                 i += reverse ? -1 : 1) {
                Slot slot = slots.get(i);
                ItemStack held = slot.getItem();
                if (held.isEmpty() || !ItemStacks.sameItemSameData(stack, held)) continue;

                int total = held.getCount() + stack.getCount();
                int limit = slot.getMaxStackSize(held);
                if (total <= limit) {
                    stack.setCount(0);
                    held.setCount(total);
                    slot.setChanged();
                    moved = true;
                } else if (held.getCount() < limit) {
                    stack.shrink(limit - held.getCount());
                    held.setCount(limit);
                    slot.setChanged();
                    moved = true;
                }
            }
        }
        if (stack.isEmpty()) return moved;

        for (int i = reverse ? end - 1 : start; reverse ? i >= start : i < end; i += reverse ? -1 : 1) {
            Slot slot = slots.get(i);
            if (slot.getItem().isEmpty() && slot.mayPlace(stack)) {
                slot.setByPlayer(stack.split(Math.min(stack.getCount(), slot.getMaxStackSize(stack))));
                slot.setChanged();
                return true;
            }
        }
        return moved;
    }

    @Override
    public boolean stillValid(@NotNull Player who) { return who == player && who.isAlive(); }

    private class GuSlot extends Slot {

        GuSlot(Container container, int index, int x, int y) { super(container, index, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return ApertureStorageService.storedGu().rank(stack) != null
                    && (hasItem()
                    || ApertureStorageService.getMaxStackSize(player, aperture, getLoad(), getItem(), stack) > 0);
        }

        @Override
        public int getMaxStackSize(@NotNull ItemStack stack) {
            return ApertureStorageService.getMaxStackSize(player, aperture, getLoad(), getItem(), stack);
        }
    }

    private class VitalSlot extends Slot {

        VitalSlot(int x, int y) { super(vital, 0, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (!ApertureStorageService.storedGu().canBeVital(stack, player)) return false;
            return ApertureStorageService.getMaxStackSize(player, aperture, getLoad(), getItem(), stack) > 0;
        }

        @Override
        public int getMaxStackSize() { return 1; }
    }
}
