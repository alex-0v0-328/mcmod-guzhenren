package net.alex.guzhenren.gameplay.refinement;

import java.util.List;
import net.alex.guzhenren.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.path.time.PathTimeFlowService;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.PrimevalElderGuItem;
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
 * The refinement [炼蛊] container: the ring grid, the ritual's clock, and the settlement.
 *
 * <p>The menu IS the clock -- its {@code broadcastChanges} runs every tick, so closing the window
 * aborts by construction and lifting the state out would need an abort path that does not exist. The
 * 5×5 grid (corners cut), the primeval stone slot, and the 2×2 output are transient; no attachment involved.
 *
 * <p>⚠ The ritual lives in the menu deliberately. The grid locks while it runs, nothing is consumed
 * until the last tick, and the client reads every live figure through {@link
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

    //region the input -- a 5x5 with its four corners cut away: outer ring [外圈] 12 around inner ring [内圈] 9
    public static final int GRID_COLS = 5;
    public static final int GRID_ROWS = 5;
    public static final int RING_SIZE = 12;
    public static final int CORE_COLS = 3;
    public static final int CORE_ROWS = 3;
    public static final int CORE_SIZE = CORE_COLS * CORE_ROWS;
    public static final int INPUT_SIZE = RING_SIZE + CORE_SIZE;
    private static final int[] RING_COLS = { 1, 2, 3, 0, 4, 0, 4, 0, 4, 1, 2, 3 };
    private static final int[] RING_ROWS = { 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4 };
    public static final int INPUT_X = 18;
    public static final int INPUT_Y = 28;

    public static int ringX(int index) { return INPUT_X + RING_COLS[index] * GRID_SLOT; }

    public static int ringY(int index) { return INPUT_Y + RING_ROWS[index] * GRID_SLOT; }

    public static int coreX(int col) { return INPUT_X + (col + 1) * GRID_SLOT; }

    public static int coreY(int row) { return INPUT_Y + (row + 1) * GRID_SLOT; }

    public static int slotAt(int row, int col) {
        for (int i = 0; i < RING_SIZE; i++) {
            if (RING_ROWS[i] == row && RING_COLS[i] == col) return i;
        }
        if (row < 1 || row > CORE_ROWS || col < 1 || col > CORE_COLS) return -1;
        return RING_SIZE + (row - 1) * CORE_COLS + (col - 1);
    }
    //endregion

    public static final int OUTPUT_COLS = 2;
    public static final int OUTPUT_ROWS = 2;
    public static final int OUTPUT_SIZE = OUTPUT_COLS * OUTPUT_ROWS;
    public static final int STONE_SLOT = INPUT_SIZE;
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
    private static final int INVENTORY_COLS = 9;
    private static final int FAILURE_HEALTH_DIVISOR = 4;
    private static final int FULL_SUCCESS = 100;
    private static final int OPENING_PERCENT = 40;
    private static final int CURRENT_PER_MAX_SOUL = 10;
    private static final int DATA_READY = 0;
    private static final int DATA_AFFORD = 1;
    private static final int DATA_RUNNING = 2;
    private static final int DATA_STAGE = 3;
    private static final int DATA_STAGES = 4;
    private static final int DATA_PHASE_LEFT = 5;
    private static final int DATA_IN_WINDOW = 6;
    private static final int DATA_STONES_IN = 7;
    private static final int DATA_STONES_NEEDED = 8;
    private static final int DATA_SELECTED = 9;
    private static final int DATA_SIZE = 10;
    private static final String FAILED_ESSENCE = "guzhenren.menu.refinement.essence";
    private static final String FAILED_NO_ROOM = "guzhenren.menu.refinement.no_room";
    private static final String FAILED_NOT_AWAKENED = "guzhenren.menu.refinement.not_awakened";
    private static final String LOST_STONES = "guzhenren.menu.refinement.lost_stones";
    private static final String LOST_ESSENCE = "guzhenren.menu.refinement.lost_essence";
    private static final String LOST_ROLL = "guzhenren.menu.refinement.lost_roll";
    private static final String ELDER_SPENT = "guzhenren.menu.refinement.elder_spent";
    private static final String STOPPED = "guzhenren.menu.refinement.stopped";
    private final Player player;
    private final SimpleContainer input = new SimpleContainer(INPUT_SIZE);
    private final SimpleContainer supply = new SimpleContainer(1);
    private final SimpleContainer output = new SimpleContainer(OUTPUT_SIZE);
    private final ContainerData craftData = new SimpleContainerData(DATA_SIZE);
    private @Nullable GuRecipe pending;
    private int selectedIndex = -1;
    private @Nullable GuRecipe running;
    private int @Nullable [] claimed;
    private int stage;
    private int phaseLeft;
    private boolean inWindow;
    private int stonesThisWindow;
    private int secondCounter;

    public RefinementMenu(int id, Inventory inventory) {
        super(ModMenus.REFINEMENT_MENU.get(), id);
        this.player = inventory.player;

        for (int i = 0; i < RING_SIZE; i++) {
            addSlot(new RingSlot(input, i, ringX(i), ringY(i)));
        }
        for (int row = 0; row < CORE_ROWS; row++) {
            for (int col = 0; col < CORE_COLS; col++) {
                addSlot(new CoreSlot(input, RING_SIZE + row * CORE_COLS + col, coreX(col), coreY(row)));
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
        if (server == null || running != null) return false;

        List<RecipeHolder<GuRecipe>> known = GuRecipe.known(server.getRecipeManager());
        selectedIndex = index >= 0 && index < known.size() ? index : -1;
        craftData.set(DATA_SELECTED, selectedIndex + 1);
        if (selectedIndex >= 0) fill(known.get(selectedIndex).value());
        refresh();
        return true;
    }

    private void refresh() {
        if (!(player instanceof ServerPlayer) || running != null) return;

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
            if (slot < 0 || slot >= INPUT_SIZE) continue;

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

    //region the ritual -- the menu is its own clock, because ServerPlayer.tick() broadcasts every tick
    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer server) {
            for (int step = PathTimeFlowService.steps(server); step > 0 && running != null; step--) {
                advance(server);
            }
            craftData.set(DATA_AFFORD, pending != null && affords(server, pending) ? 1 : 0);
        }
        super.broadcastChanges();
    }

    private void advance(ServerPlayer server) {
        GuRecipe recipe = running;
        if (recipe == null) return;

        if (++secondCounter >= Ticks.SECOND) {
            secondCounter = 0;
            if (recipe.essencePerSecond() > 0
                    && !ApertureEssenceService.consume(server, recipe.essencePerSecond())) {
                fail(server, LOST_ESSENCE);
                return;
            }
            burnSoul(server, recipe.soulPerSecond());
        }
        if (inWindow) gatherStones(server, recipe);
        refillFromSupply(server);

        if (--phaseLeft > 0) {
            publishRun(recipe);
            return;
        }
        if (!inWindow) {
            stage++;
            inWindow = true;
            stonesThisWindow = 0;
            phaseLeft = GuRecipe.WINDOW_TICKS;
            publishRun(recipe);
            return;
        }
        if (stonesThisWindow < recipe.stonesFor(stage)) {
            fail(server, LOST_STONES);
            return;
        }
        if (stage + 1 >= recipe.windowCount()) {
            settle(server, recipe);
            return;
        }
        inWindow = false;
        phaseLeft = GuRecipe.GAP_TICKS;
        publishRun(recipe);
    }

    private void gatherStones(ServerPlayer server, GuRecipe recipe) {
        int wanted = recipe.stonesFor(stage) - stonesThisWindow;
        if (wanted <= 0) return;

        stonesThisWindow += takeStones(wanted);
        ItemStack held = supply.getItem(0);
        if (!(held.getItem() instanceof PrimevalElderGuItem)) return;
        if (stonesThisWindow >= recipe.stonesFor(stage)) return;

        say(server, ELDER_SPENT, ChatFormatting.RED);
        if (!server.getInventory().add(held.copy())) server.drop(held.copy(), false);
        supply.setItem(0, ItemStack.EMPTY);
    }

    private int takeStones(int wanted) {
        if (wanted <= 0) return 0;

        ItemStack held = supply.getItem(0);
        if (held.getItem() instanceof PrimevalStoneItem) {
            int taken = Math.min(wanted, held.getCount());
            held.shrink(taken);
            supply.setChanged();
            return taken;
        }
        return held.getItem() instanceof PrimevalElderGuItem vault
                ? vault.drawStones(held, wanted) : 0;
    }
    //endregion

    //region soul [魂魄] -- current first, then maxSoul at a tenth the rate, which is what kills him
    private static void burnSoul(ServerPlayer server, long amount) {
        if (amount <= 0L || SoulService.consume(server, amount)) return;

        long owed = amount - SoulService.get(server).currentSoul();
        SoulService.setCurrent(server, 0L);

        long fromMax = (owed + CURRENT_PER_MAX_SOUL - 1) / CURRENT_PER_MAX_SOUL;
        SoulService.setMax(server, Math.max(0L, SoulService.get(server).maxSoul() - fromMax));
    }
    //endregion

    //region the stone top-up [元石补给] -- only ever what the window did not want
    private void refillFromSupply(ServerPlayer server) {
        if (!PrimevalStoneItem.needsTopUp(server)) return;

        long perStone = PrimevalStoneItem.essencePerStone();
        long missing = PrimevalStoneItem.topUpDeficit(server);
        if (perStone <= 0L || missing <= 0L) return;

        int wanted = (int) Math.min(Integer.MAX_VALUE, (missing + perStone - 1) / perStone);
        int drawn = takeStones(wanted);
        if (drawn > 0) ApertureEssenceService.add(server, drawn * perStone);
    }
    //endregion

    //region settling -- nothing is consumed until here, so failure can take half and wound the rest
    private void settle(ServerPlayer server, GuRecipe recipe) {
        int chance = Math.min(FULL_SUCCESS, recipe.baseSuccess()
                + PathService.attainment(server, GuPath.REFINEMENT).getRefinementBonus());
        if (server.getRandom().nextInt(FULL_SUCCESS) >= chance) {
            fail(server, LOST_ROLL);
            return;
        }
        int[] taken = claimed;
        boolean vital = taken != null && eatsVital(taken);
        if (taken != null) {
            for (int i = 0; i < taken.length; i++) {
                if (taken[i] > 0) input.removeItem(i, taken[i]);
            }
        }
        deliver(server, recipe, vital);
        stop();
        refresh();
    }

    private void fail(ServerPlayer server, String key) {
        int[] taken = claimed;
        if (taken != null) {
            for (int i = 0; i < taken.length; i++) {
                if (taken[i] > 0) spoil(server, i, taken[i]);
            }
        }
        say(server, key, ChatFormatting.RED);
        stop();
        refresh();
    }

    private void spoil(ServerPlayer server, int slot, int taken) {
        ItemStack stack = input.getItem(slot);
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof TendedGuItem gu) {
            if (gu.damageKills(server, stack, gu.maxHealth() / FAILURE_HEALTH_DIVISOR)) {
                input.setItem(slot, ItemStack.EMPTY);
            }
            return;
        }
        if (stack.getItem() instanceof MortalGuItem) return;

        input.removeItem(slot, (taken + 1) / 2);
    }

    private void stop() {
        running = null;
        claimed = null;
        stage = 0;
        phaseLeft = 0;
        inWindow = false;
        stonesThisWindow = 0;
        secondCounter = 0;
        craftData.set(DATA_RUNNING, 0);
        craftData.set(DATA_STAGE, 0);
        craftData.set(DATA_STAGES, 0);
        craftData.set(DATA_PHASE_LEFT, 0);
        craftData.set(DATA_IN_WINDOW, 0);
        craftData.set(DATA_STONES_IN, 0);
        craftData.set(DATA_STONES_NEEDED, 0);
    }

    private void publishRun(GuRecipe recipe) {
        craftData.set(DATA_RUNNING, 1);
        craftData.set(DATA_STAGE, stage);
        craftData.set(DATA_STAGES, recipe.windowCount());
        craftData.set(DATA_PHASE_LEFT, phaseLeft);
        craftData.set(DATA_IN_WINDOW, inWindow ? 1 : 0);
        craftData.set(DATA_STONES_IN, stonesThisWindow);
        craftData.set(DATA_STONES_NEEDED, recipe.stonesFor(stage));
    }
    //endregion

    //region 炼制 -- the button only STARTS it now; the outcome lands 26 seconds later
    @Override
    public boolean clickMenuButton(@NotNull Player who, int id) {
        if (who != player) return false;
        if (id == BUTTON_CRAFT) return begin();
        if (id == BUTTON_STOP) return abort();
        if (id == BUTTON_CLEAR_RECIPE) return select(-1);
        return id >= BUTTON_RECIPE_BASE && select(id - BUTTON_RECIPE_BASE);
    }

    private boolean abort() {
        if (!(player instanceof ServerPlayer server) || running == null) return false;

        say(server, STOPPED, ChatFormatting.RED);
        stop();
        refresh();
        return true;
    }

    private boolean begin() {
        if (!(player instanceof ServerPlayer server) || running != null) return false;
        if (!ApertureService.isAwakened(server)) return refuse(server, FAILED_NOT_AWAKENED);

        GuRecipe recipe = match();
        if (recipe == null || recipe.results().isEmpty() || recipe.windowCount() <= 0) return false;
        if (!affords(server, recipe)) return refuse(server, FAILED_ESSENCE, recipe.essenceToFinish());
        if (freeOutputSlots() < recipe.results().size()) return refuse(server, FAILED_NO_ROOM);

        int[] taken = recipe.claim(GuRecipeInput.of(input));
        if (taken == null) return false;

        running = recipe;
        claimed = taken;
        stage = 0;
        inWindow = true;
        stonesThisWindow = 0;
        secondCounter = 0;
        phaseLeft = GuRecipe.WINDOW_TICKS;
        publishRun(recipe);
        return true;
    }

    private void deliver(ServerPlayer server, GuRecipe recipe, boolean vital) {
        boolean sole = recipe.guResultCount() == 1;
        int slot = 0;

        for (ItemStack stack : recipe.results()) {
            ItemStack made = stack.copy();
            if (made.getItem() instanceof TendedGuItem gu) {
                gu.bornRefined(server, made);
                if (vital && sole) inherit(server, made, gu);
            }
            while (slot < OUTPUT_SIZE && !output.getItem(slot).isEmpty()) slot++;
            if (slot >= OUTPUT_SIZE) return;
            output.setItem(slot, made);
        }
    }

    private static void inherit(ServerPlayer server, ItemStack made, TendedGuItem gu) {
        GuItem.bind(made, server, ApertureData.PRIMARY);
        ApertureService.setPrimaryPath(server, ApertureData.PRIMARY, gu.path());
    }

    private int freeOutputSlots() {
        int free = 0;
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            if (output.getItem(i).isEmpty()) free++;
        }
        return free;
    }

    private boolean eatsVital(int[] taken) {
        for (int i = 0; i < taken.length; i++) {
            if (taken[i] > 0 && GuItem.isVital(input.getItem(i))) return true;
        }
        return false;
    }

    private static void say(ServerPlayer who, String key, ChatFormatting colour, Object... args) {
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
        stop();
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
                || moveItemStackTo(stack, 0, INPUT_SIZE, false);
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
            return running == null && !(stack.getItem() instanceof MortalGuItem);
        }

        @Override
        public boolean mayPickup(@NotNull Player who) { return running == null; }
    }

    private class CoreSlot extends Slot {

        CoreSlot(Container container, int index, int x, int y) { super(container, index, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (running != null || !(stack.getItem() instanceof MortalGuItem)) return false;
            return !GuItem.isVital(stack) || GuItem.isVitalOf(stack, player);
        }

        @Override
        public boolean mayPickup(@NotNull Player who) { return running == null; }
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
