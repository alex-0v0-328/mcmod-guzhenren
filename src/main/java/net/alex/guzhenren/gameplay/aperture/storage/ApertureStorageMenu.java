package net.alex.guzhenren.gameplay.aperture.storage;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.minecraft.server.level.ServerPlayer;
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
 * override is never called because {@code AbstractContainerMenu} is not a {@code ContainerListener}.
 *
 * <p>{@code countPages} counts the pages that hold every stored Gu plus one free slot, so a full page
 * still leaves an empty one for the next Gu. Binding a Vital Gu here only marks the stack;
 * {@code ApertureStorageService.setVital} then sets the aperture's primary path.
 *
 * <p>⚠ It has to reload after the day-rollover walk, or an open menu saves its stale view back and
 * resurrects a Gu that starved a moment earlier.
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
    public static final int VITAL_SLOT = PAGE_SIZE + 36;
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
        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                addSlot(new Slot(inventory, col + row * COLS + 9,
                        STORAGE_X + col * SLOT, INVENTORY_Y + row * SLOT));
            }
        }
        for (int col = 0; col < COLS; col++) {
            addSlot(new Slot(inventory, col, STORAGE_X + col * SLOT, HOTBAR_Y));
        }
        addSlot(new VitalSlot(VITAL_X, VITAL_Y));

        page.addListener(container -> save());
        vital.addListener(container -> save());

        addDataSlots(pageData);
        load(pageIndex);
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

    public void reload() {
        load(getPageIndex());
        broadcastChanges();
    }

    private void load(int index) {
        loading = true;
        int at = Math.clamp(index, 0, countPages() - 1);
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

        List<ItemStack> window = new ArrayList<>(PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) window.add(page.getItem(i).copy());
        int from = getPageIndex() * PAGE_SIZE;
        if (!ApertureStorageService.matchesPage(server, aperture, from, window)) {
            if (!ApertureStorageService.setPage(server, aperture, from, window)) {
                load(getPageIndex());
                return;
            }
        }

        ItemStack bound = vital.getItem(0);
        if (!bound.isEmpty() && bound.getItem() instanceof GuItem
                && (!GuItem.isVital(bound) || GuItem.boundAperture(bound) != aperture)) {
            GuItem.bind(bound, server, aperture);
        }
        if (!same(bound, ApertureStorageService.getVital(server, aperture))) {
            if (!ApertureStorageService.setVital(server, aperture, bound.copy())) {
                load(getPageIndex());
                return;
            }
        }

        pageData.set(DATA_PAGES, countPages());
        pageData.set(DATA_LOAD, ApertureStorageService.load(server, aperture));
    }

    private static boolean same(ItemStack first, ItemStack second) {
        return first.getCount() == second.getCount()
                && ItemStack.isSameItemSameComponents(first, second);
    }
    //endregion

    @Override
    public void removed(@NotNull Player who) {
        save();
        super.removed(who);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player who, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean toInventory = index < PAGE_SIZE || index == VITAL_SLOT;

        boolean moved = toInventory
                ? moveItemStackTo(stack, PAGE_SIZE, VITAL_SLOT, true)
                : moveItemStackTo(stack, 0, PAGE_SIZE, false);
        if (!moved) return ItemStack.EMPTY;

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(@NotNull Player who) { return who == player && who.isAlive(); }

    private class GuSlot extends Slot {

        GuSlot(Container container, int index, int x, int y) { super(container, index, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof MortalGuItem
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
            if (!(stack.getItem() instanceof TendedGuItem gu) || !gu.refined(stack)) return false;
            if (!gu.canBeVital()) return false;
            return !GuItem.isVital(stack) || GuItem.isVitalOf(stack, player);
        }

        @Override
        public int getMaxStackSize() { return 1; }
    }
}
