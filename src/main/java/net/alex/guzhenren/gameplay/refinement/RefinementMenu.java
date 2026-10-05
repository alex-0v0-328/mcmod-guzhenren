package net.alex.guzhenren.gameplay.refinement;

import java.util.List;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.path.time.PathTimeFlowService;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The refinement [炼蛊] container: the ring grid, the recipe [蛊方] behind the button, and the clock of
 * the {@link RefinementRitual} it owns.
 *
 * <p>The menu IS the clock -- its {@code broadcastChanges} runs every tick and advances the ritual, so
 * closing the window aborts by construction. The 5×5 grid (corners cut), the primeval stone slot, and the
 * 2×2 output are transient; no attachment involved.
 *
 * <p>⚠ The ritual is owned by the menu deliberately, never lifted into a service: the grid locks while it
 * runs, and the client reads every live figure through {@link
 * net.minecraft.world.inventory.ContainerData} (ten ints) because it cannot match a recipe itself.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuRecipe
 * @since 1.0.0
 */

public class RefinementMenu extends AbstractContainerMenu {

    public static final int SLOT = 18;
    public static final int GRID_SLOT = 22;

    //region the input on screen -- the grid's shape is GuRecipe's; this only places it
    public static final int INPUT_X = 18;
    public static final int INPUT_Y = 28;

    public static int ringX(int index) { return INPUT_X + GuRecipe.ringCol(index) * GRID_SLOT; }

    public static int ringY(int index) { return INPUT_Y + GuRecipe.ringRow(index) * GRID_SLOT; }

    public static int coreX(int col) { return INPUT_X + (col + 1) * GRID_SLOT; }

    public static int coreY(int row) { return INPUT_Y + (row + 1) * GRID_SLOT; }
    //endregion

    public static final int OUTPUT_COLS = 2;
    public static final int OUTPUT_ROWS = 2;
    public static final int OUTPUT_SIZE = OUTPUT_COLS * OUTPUT_ROWS;
    public static final int STONE_SLOT = GuRecipe.INPUT_SIZE;
    public static final int OUTPUT_START = STONE_SLOT + 1;
    public static final int INVENTORY_START = OUTPUT_START + OUTPUT_SIZE;
    public static final int BUTTON_CRAFT = 0;
    public static final int BUTTON_STOP = 1;
    public static final int BUTTON_CLEAR_RECIPE = 2;
    public static final int BUTTON_RECIPE_BASE = 3;
    public static final int OUTPUT_X = 210;
    public static final int OUTPUT_Y = 60;
    public static final int STONE_X = 158;
    public static final int STONE_Y = 48;
    public static final int INVENTORY_X = 52;
    public static final int INVENTORY_Y = 229;
    public static final int HOTBAR_Y = 287;
    public static final int INVENTORY_COLS = 9;
    private static final int OPENING_PERCENT = 40;
    private static final int DATA_READY = 0;
    private static final int DATA_AFFORD = 1;
    static final int DATA_RUNNING = 2;
    static final int DATA_STAGE = 3;
    static final int DATA_STAGES = 4;
    static final int DATA_PHASE_LEFT = 5;
    static final int DATA_IN_WINDOW = 6;
    static final int DATA_STONES_IN = 7;
    static final int DATA_STONES_NEEDED = 8;
    private static final int DATA_SELECTED = 9;
    private static final int DATA_SIZE = 10;
    private static final String FAILED_ESSENCE = "guzhenren.menu.refinement.essence";
    private static final String FAILED_NO_ROOM = "guzhenren.menu.refinement.no_room";
    private static final String FAILED_NOT_AWAKENED = "guzhenren.menu.refinement.not_awakened";
    private static final String STOPPED = "guzhenren.menu.refinement.stopped";
    private final Player player;
    private final SimpleContainer input = new SimpleContainer(GuRecipe.INPUT_SIZE);
    private final SimpleContainer supply = new SimpleContainer(1);
    private final SimpleContainer output = new SimpleContainer(OUTPUT_SIZE);
    private final ContainerData craftData = new SimpleContainerData(DATA_SIZE);
    private final RefinementRitual ritual = new RefinementRitual(input, supply, output, craftData, this::refresh);
    private @Nullable GuRecipe pending;
    private int selectedIndex = -1;

    public RefinementMenu(int id, Inventory inventory) {
        super(ModMenus.REFINEMENT_MENU.get(), id);
        this.player = inventory.player;

        for (int i = 0; i < GuRecipe.RING_SIZE; i++) {
            addSlot(new RingSlot(input, i, ringX(i), ringY(i)));
        }
        for (int row = 0; row < GuRecipe.CORE_ROWS; row++) {
            for (int col = 0; col < GuRecipe.CORE_COLS; col++) {
                int index = GuRecipe.RING_SIZE + row * GuRecipe.CORE_COLS + col;
                addSlot(new CoreSlot(input, index, coreX(col), coreY(row)));
            }
        }
        addSlot(new SupplySlot(supply, 0, STONE_X, STONE_Y));
        for (int row = 0; row < OUTPUT_ROWS; row++) {
            for (int col = 0; col < OUTPUT_COLS; col++) {
                addSlot(new OutputSlot(row * OUTPUT_COLS + col,
                        OUTPUT_X + col * GRID_SLOT, OUTPUT_Y + row * GRID_SLOT));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < INVENTORY_COLS; col++) {
                addSlot(new Slot(inventory, col + row * INVENTORY_COLS + 9,
                        INVENTORY_X + col * SLOT, INVENTORY_Y + row * SLOT));
            }
        }
        for (int col = 0; col < INVENTORY_COLS; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * SLOT, HOTBAR_Y));
        }
        addDataSlots(craftData);
        input.addListener(container -> refresh());
    }

    //region what the screen reads
    public boolean ready() { return craftData.get(DATA_READY) != 0; }

    public boolean affords() { return craftData.get(DATA_AFFORD) != 0; }

    public boolean running() { return craftData.get(DATA_RUNNING) != 0; }

    public boolean inWindow() { return craftData.get(DATA_IN_WINDOW) != 0; }

    public int stage() { return craftData.get(DATA_STAGE); }

    public int stages() { return craftData.get(DATA_STAGES); }

    public int phaseLeft() { return craftData.get(DATA_PHASE_LEFT); }

    public int stonesIn() { return craftData.get(DATA_STONES_IN); }

    public int stonesNeeded() { return craftData.get(DATA_STONES_NEEDED); }

    public int selected() { return craftData.get(DATA_SELECTED) - 1; }

    public GuRecipeInput grid() { return GuRecipeInput.of(input); }
    //endregion

    //region the Gu Recipe [蛊方] behind the button -- a selected one is the only one match() will consider
    private @Nullable GuRecipe match() {
        MinecraftServer server = player.getServer();
        if (server == null) return null;

        GuRecipeInput in = GuRecipeInput.of(input);
        List<RecipeHolder<GuRecipe>> known = GuRecipe.known(server.getRecipeManager());
        if (selectedIndex >= 0) {
            if (selectedIndex >= known.size()) return null;

            GuRecipe only = known.get(selectedIndex).value();
            return only.claim(in) != null ? only : null;
        }
        for (RecipeHolder<GuRecipe> held : known) {
            if (held.value().claim(in) != null) return held.value();
        }
        return null;
    }

    private boolean select(int index) {
        MinecraftServer server = player.getServer();
        if (server == null || ritual.isRunning()) return false;

        List<RecipeHolder<GuRecipe>> known = GuRecipe.known(server.getRecipeManager());
        selectedIndex = index >= 0 && index < known.size() ? index : -1;
        craftData.set(DATA_SELECTED, selectedIndex + 1);
        if (selectedIndex >= 0) fill(known.get(selectedIndex).value());
        refresh();
        return true;
    }

    private void refresh() {
        if (!(player instanceof ServerPlayer) || ritual.isRunning()) return;

        pending = match();
        craftData.set(DATA_READY, pending != null ? 1 : 0);
    }

    private static boolean affords(Player who, GuRecipe recipe) {
        return ApertureEssenceService.spendable(who) >= threshold(recipe);
    }

    private static long threshold(GuRecipe recipe) {
        long essence = recipe.essenceToFinish();
        return essence / 100L * OPENING_PERCENT + essence % 100L * OPENING_PERCENT / 100L;
    }
    //endregion

    //region autofill [自动填充] -- picking a 蛊方 pulls what the bag can cover into the cells it names
    private void fill(GuRecipe recipe) {
        for (int n = 0; n < recipe.ingredients().size(); n++) {
            int slot = recipe.slots().get(n);
            if (slot < 0 || slot >= GuRecipe.INPUT_SIZE) continue;

            SizedIngredient need = recipe.ingredients().get(n);
            ItemStack held = input.getItem(slot);
            if (!held.isEmpty() && !need.ingredient().test(held)) continue;

            int wanted = need.count() - held.getCount();
            if (wanted > 0) draw(need, slot, wanted);
        }
    }

    private void draw(SizedIngredient need, int slot, int wanted) {
        Inventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize() && wanted > 0; i++) {
            ItemStack from = inventory.getItem(i);
            if (from.isEmpty() || !need.ingredient().test(from)) continue;

            ItemStack held = input.getItem(slot);
            if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, from)) continue;

            int move = Math.min(Math.min(wanted, from.getCount()),
                    from.getMaxStackSize() - held.getCount());
            if (move <= 0) continue;

            if (held.isEmpty()) {
                input.setItem(slot, from.split(move));
            } else {
                held.grow(move);
                from.shrink(move);
                input.setChanged();
            }
            wanted -= move;
        }
    }
    //endregion

    //region the ritual's clock -- the menu ticks it, because ServerPlayer.tick() broadcasts every tick
    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer server) {
            for (int step = PathTimeFlowService.getSteps(server); step > 0 && ritual.isRunning(); step--) {
                ritual.advance(server);
            }
            craftData.set(DATA_AFFORD, pending != null && affords(server, pending) ? 1 : 0);
        }
        super.broadcastChanges();
    }

    //endregion

    //region 炼制 -- the button only STARTS it; the outcome lands after every window and gap has run
    @Override
    public boolean clickMenuButton(@NotNull Player who, int id) {
        if (who != player) return false;
        if (id == BUTTON_CRAFT) return begin();
        if (id == BUTTON_STOP) return abort();
        if (id == BUTTON_CLEAR_RECIPE) return select(-1);
        return id >= BUTTON_RECIPE_BASE && select(id - BUTTON_RECIPE_BASE);
    }

    private boolean abort() {
        if (!(player instanceof ServerPlayer server) || !ritual.isRunning()) return false;

        say(server, STOPPED, ChatFormatting.RED);
        ritual.stop();
        refresh();
        return true;
    }

    private boolean begin() {
        if (!(player instanceof ServerPlayer server) || ritual.isRunning()) return false;
        if (!ApertureService.isAwakened(server)) return refuse(server, FAILED_NOT_AWAKENED);

        GuRecipe recipe = match();
        if (recipe == null || recipe.results().isEmpty() || recipe.windowCount() <= 0) return false;
        if (!affords(server, recipe)) return refuse(server, FAILED_ESSENCE, recipe.essenceToFinish());
        if (freeOutputSlots() < recipe.results().size()) return refuse(server, FAILED_NO_ROOM);

        int[] taken = recipe.claim(GuRecipeInput.of(input));
        if (taken == null) return false;

        ritual.start(recipe, taken);
        return true;
    }

    private int freeOutputSlots() {
        int free = 0;
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            if (output.getItem(i).isEmpty()) free++;
        }
        return free;
    }

    static void say(ServerPlayer who, String key, ChatFormatting colour, Object... args) {
        who.displayClientMessage(Component.translatable(key, args).withStyle(colour), true);
    }

    private static boolean refuse(ServerPlayer who, String key, Object... args) {
        say(who, key, ChatFormatting.RED, args);
        return false;
    }
    //endregion

    @Override
    public void removed(@NotNull Player who) {
        super.removed(who);
        ritual.stop();
        clearContainer(who, input);
        clearContainer(who, supply);
        clearContainer(who, output);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player who, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        boolean moved = index < INVENTORY_START
                ? moveItemStackTo(stack, INVENTORY_START, slots.size(), true)
                : moveItemStackTo(stack, STONE_SLOT, STONE_SLOT + 1, false)
                || moveItemStackTo(stack, 0, GuRecipe.INPUT_SIZE, false);
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

    //region where a thing may sit -- 蛊材 outside, 蛊虫 inside, and NOTHING moves while it runs
    private class RingSlot extends Slot {

        RingSlot(Container container, int index, int x, int y) { super(container, index, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return !ritual.isRunning() && !(stack.getItem() instanceof MortalGuItem);
        }

        @Override
        public boolean mayPickup(@NotNull Player who) { return !ritual.isRunning(); }
    }

    private class CoreSlot extends Slot {

        CoreSlot(Container container, int index, int x, int y) { super(container, index, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (ritual.isRunning() || !(stack.getItem() instanceof MortalGuItem)) return false;
            return !GuItem.isVital(stack) || GuItem.isVitalOf(stack, player);
        }

        @Override
        public boolean mayPickup(@NotNull Player who) { return !ritual.isRunning(); }
    }

    private class SupplySlot extends Slot {

        SupplySlot(Container container, int index, int x, int y) { super(container, index, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            boolean fuel = stack.getItem() instanceof PrimevalStoneItem
                    || stack.getItem() instanceof PrimevalElderGuItem;
            return fuel && (!GuItem.isVital(stack) || GuItem.isVitalOf(stack, player));
        }
    }

    private class OutputSlot extends Slot {

        OutputSlot(int index, int x, int y) { super(output, index, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) { return false; }
    }
    //endregion
}
