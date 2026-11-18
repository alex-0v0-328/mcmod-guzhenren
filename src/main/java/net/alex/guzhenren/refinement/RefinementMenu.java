package net.alex.guzhenren.refinement;

import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.MenuSlots;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.alex.guzhenren.refinement.recipe.GuRecipeInput;
import net.alex.guzhenren.refinement.recipe.RefinementRecipe;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 炼蛊界面的服务端容器菜单，统一管理 5×5 去角环形网格、配方选中逻辑以及所属 {@link RefinementRitual} 的运行推进。
 * <ul>
 *   <li>界面即仪式时钟：菜单每刻在 {@link #broadcastChanges} 中按时间流速步数推进仪式，并在关闭菜单时立即终止仪式，由容
 *       器生命周期天然保证中止语义；5×5 网格、元石[Primeval Stone]补给格与 2×2 产出格均为临时容器，不挂接玩家持久附
 *       件。</li>
 *   <li>⚠ 仪式刻意内置于菜单而非下沉至玩法服务：仪式运行时会直接锁定网格槽位，且客户端无法自行匹配 {@link GuRecipe}，所
 *       有运行期状态均通过 10 个 {@link ContainerData} 整型槽同步。</li>
 *   <li>⚠ 网格外环仅允许放置蛊材、内核仅允许放置玩家自身非他人本命的蛊虫，且仪式开始后全部输入格禁止拿取或放入。</li>
 *   <li>只认玩家已学会的蛊方（{@link GuRecipeKnowledge}）：未学会的既选不中也不参与自动匹配；选中索引仍是服务器全部蛊方
 *       的排序索引。</li>
 *   <li>选中固定配方后仅匹配该配方并通过 {@link RecipeAutofill} 从背包补料；点击炼制按钮仅启动仪式，实际扣料与产出结算
 *       须待全部投石窗口与间隔走完才落定。</li>
 *   <li>有意超过 300 行：槽位坐标布局、放置规则、配方匹配状态与仪式时钟共享同一套输入网格与锁定状态，强行拆分会割裂不变
 *       量。</li>
 * </ul>
 */

public class RefinementMenu extends AbstractContainerMenu {

    public static final int SLOT = 18;
    public static final int GRID_SLOT = 22;

    //region the input on screen [输入格]
    public static final int INPUT_X = 18;
    public static final int INPUT_Y = 28;

    public static int getRingX(int index) { return INPUT_X + GuRecipe.getRingCol(index) * GRID_SLOT; }

    public static int getRingY(int index) { return INPUT_Y + GuRecipe.getRingRow(index) * GRID_SLOT; }

    public static int getCoreX(int col) { return INPUT_X + (col + 1) * GRID_SLOT; }

    public static int getCoreY(int row) { return INPUT_Y + (row + 1) * GRID_SLOT; }
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
    private static final String FAILED_NO_ROOM = "guzhenren.menu.refinement.no_room";
    private static final String STOPPED = "guzhenren.menu.refinement.stopped";
    private final Player player;
    private final SimpleContainer input = new SimpleContainer(GuRecipe.INPUT_SIZE);
    private final SimpleContainer supply = new SimpleContainer(1);
    private final SimpleContainer output = new SimpleContainer(OUTPUT_SIZE);
    private final ContainerData craftData = new SimpleContainerData(DATA_SIZE);
    private final RefinementRitual ritual = new RefinementRitual(input, supply, output, craftData, this::refresh);
    private @Nullable GuRecipe pending;
    private int selectedIndex = -1;
    private long advancedAt = Long.MIN_VALUE;

    public RefinementMenu(int id, Inventory inventory) {
        super(ModMenus.REFINEMENT_MENU.get(), id);
        this.player = inventory.player;

        for (int i = 0; i < GuRecipe.RING_SIZE; i++) {
            addSlot(new GridSlot(i, getRingX(i), getRingY(i), false));
        }
        for (int row = 0; row < GuRecipe.CORE_ROWS; row++) {
            for (int col = 0; col < GuRecipe.CORE_COLS; col++) {
                int index = GuRecipe.RING_SIZE + row * GuRecipe.CORE_COLS + col;
                addSlot(new GridSlot(index, getCoreX(col), getCoreY(row), true));
            }
        }
        addSlot(new SupplySlot(STONE_X, STONE_Y));
        for (int row = 0; row < OUTPUT_ROWS; row++) {
            for (int col = 0; col < OUTPUT_COLS; col++) {
                addSlot(new OutputSlot(row * OUTPUT_COLS + col,
                        OUTPUT_X + col * GRID_SLOT, OUTPUT_Y + row * GRID_SLOT));
            }
        }
        MenuSlots.addPlayerInventory(this::addSlot, inventory, INVENTORY_X, INVENTORY_Y, HOTBAR_Y, SLOT);
        addDataSlots(craftData);
        input.addListener(container -> refresh());
    }

    //region what the screen reads [界面读数]
    public boolean isReady() { return craftData.get(DATA_READY) != 0; }

    public boolean canAfford() { return craftData.get(DATA_AFFORD) != 0; }

    public boolean isRunning() { return craftData.get(DATA_RUNNING) != 0; }

    public boolean isInWindow() { return craftData.get(DATA_IN_WINDOW) != 0; }

    public int getStage() { return craftData.get(DATA_STAGE); }

    public int getStages() { return craftData.get(DATA_STAGES); }

    public int getPhaseLeft() { return craftData.get(DATA_PHASE_LEFT); }

    public int getStonesIn() { return craftData.get(DATA_STONES_IN); }

    public int getStonesNeeded() { return craftData.get(DATA_STONES_NEEDED); }

    public int getSelected() { return craftData.get(DATA_SELECTED) - 1; }

    public GuRecipeInput getGrid() { return GuRecipeInput.of(input); }
    //endregion

    //region the selected recipe [选中蛊方]
    private @Nullable GuRecipe match() {
        MinecraftServer server = player.getServer();
        if (server == null) return null;

        GuRecipeInput in = GuRecipeInput.of(input);
        List<RecipeHolder<RefinementRecipe>> all = RefinementRecipe.getSorted(server.getRecipeManager());
        if (selectedIndex >= 0) {
            if (selectedIndex >= all.size() || !isLearned(all.get(selectedIndex))) return null;

            GuRecipe only = all.get(selectedIndex).value().gu();
            return only.claim(in) != null ? only : null;
        }
        for (RecipeHolder<RefinementRecipe> held : all) {
            if (isLearned(held) && held.value().gu().claim(in) != null) return held.value().gu();
        }
        return null;
    }

    private boolean select(int index) {
        MinecraftServer server = player.getServer();
        if (server == null || ritual.isRunning()) return false;

        List<RecipeHolder<RefinementRecipe>> all = RefinementRecipe.getSorted(server.getRecipeManager());
        if (index >= 0 && index < all.size() && !isLearned(all.get(index))) return false;

        selectedIndex = index >= 0 && index < all.size() ? index : -1;
        craftData.set(DATA_SELECTED, selectedIndex + 1);
        if (selectedIndex >= 0) RecipeAutofill.fill(all.get(selectedIndex).value().gu(), input, player);
        refresh();
        return true;
    }

    private boolean isLearned(RecipeHolder<RefinementRecipe> held) {
        return GuRecipeKnowledge.knows(player, held.id());
    }

    private void refresh() {
        if (!(player instanceof ServerPlayer) || ritual.isRunning()) return;

        pending = match();
        craftData.set(DATA_READY, pending != null ? 1 : 0);
    }

    private static boolean canAfford(Player who, GuRecipe recipe) {
        return ApertureEssenceService.getSpendable(who) >= getThreshold(recipe);
    }

    private static long getThreshold(GuRecipe recipe) {
        long essence = recipe.getEssenceToFinish();
        return essence / 100L * OPENING_PERCENT + essence % 100L * OPENING_PERCENT / 100L;
    }
    //endregion

    //region the ritual's clock [仪式时钟]
    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer server) {
            long now = server.serverLevel().getGameTime();
            if (now != advancedAt) {
                advancedAt = now;
                for (int step = TimeFlow.getSteps(server); step > 0 && ritual.isRunning(); step--) {
                    ritual.advance(server);
                }
            }
            craftData.set(DATA_AFFORD, pending != null && canAfford(server, pending) ? 1 : 0);
        }
        super.broadcastChanges();
    }

    //endregion

    //region crafting [炼制]
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

        ActionBar.refuse(server, STOPPED);
        ritual.stop();
        refresh();
        return true;
    }

    private boolean begin() {
        if (!(player instanceof ServerPlayer server) || ritual.isRunning()) return false;
        if (!ApertureService.isAwakened(server)) return refuse(server, Refusal.UNAWAKENED);

        GuRecipe recipe = match();
        if (recipe == null || recipe.results().isEmpty() || recipe.getWindowCount() <= 0) return false;
        if (!canAfford(server, recipe)) return refuse(server, Refusal.ESSENCE);
        if (countFreeOutputSlots() < recipe.results().size()) return refuse(server, FAILED_NO_ROOM);

        int[] taken = recipe.claim(GuRecipeInput.of(input));
        if (taken == null) return false;

        ritual.start(recipe, taken);
        return true;
    }

    private int countFreeOutputSlots() {
        int free = 0;
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            if (output.getItem(i).isEmpty()) free++;
        }
        return free;
    }

    private static boolean refuse(ServerPlayer who, String key, Object... args) {
        ActionBar.refuse(who, key, args);
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
        return MenuSlots.quickMove(slots.get(index), stack -> index < INVENTORY_START
                ? moveItemStackTo(stack, INVENTORY_START, slots.size(), true)
                : moveItemStackTo(stack, STONE_SLOT, STONE_SLOT + 1, false)
                || moveItemStackTo(stack, 0, GuRecipe.INPUT_SIZE, false));
    }

    @Override
    public boolean stillValid(@NotNull Player who) { return who == player && who.isAlive(); }

    //region where a thing may sit [放置规则]
    /** 网格单格：外环放材料，内核放玩家自己的蛊。 */
    private class GridSlot extends Slot {

        private final boolean core;

        GridSlot(int index, int x, int y, boolean core) {
            super(input, index, x, y);
            this.core = core;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (ritual.isRunning() || (stack.getItem() instanceof MortalGuItem) != core) return false;
            return !core || !VitalGuState.isForeignVital(stack, player);
        }

        @Override
        public boolean mayPickup(@NotNull Player who) { return !ritual.isRunning(); }
    }

    private class SupplySlot extends Slot {

        SupplySlot(int x, int y) { super(supply, 0, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            boolean fuel = stack.getItem() instanceof PrimevalStoneItem
                    || stack.getItem() instanceof PrimevalElderGuItem;
            return fuel && !VitalGuState.isForeignVital(stack, player);
        }
    }

    private class OutputSlot extends Slot {

        OutputSlot(int index, int x, int y) { super(output, index, x, y); }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) { return false; }
    }
    //endregion
}
