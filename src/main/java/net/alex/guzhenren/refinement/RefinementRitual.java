package net.alex.guzhenren.refinement;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.item.gu.tending.GuHealth;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.path.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 单次炼蛊仪式的运行态机与结算引擎，由所属 {@link RefinementMenu} 逐刻调用 {@link #advance} 推进。
 * <ul>
 *   <li>仪式按投石窗口与窗口间隔交替运行（间隔与窗口内每秒均扣除 {@link GuRecipe} 指定的真元与魂魄），全部阶段走完后按
 *       基础成功率与炼道造诣加成掷骰结算，并通过 {@link ContainerData} 向客户端同步实时进度。</li>
 *   <li>补给槽在投石窗口内优先供应当前窗口所需元石，余量及窗口外阶段则用于为真元池补能；若燃料
 *       为元老蛊[Primeval Elder Gu]且中途耗尽，立即将其退回玩家背包或原地掉落。</li>
 *   <li>魂魄消耗优先扣除当前魂魄，不足部分按 10:1 折算扣除魂魄上限（魂魄归零即触发致死）；正式结算前不消耗网格内材料，
 *       成功时扣清已认领输入并交付产物，失败则令每格材料损失一半（向上取整）、每只持续照顾型蛊按当前伤势掷骰恶化一档
 *       （{@link GuHealth#rollRefineFailure}）。</li>
 *   <li>唯一凡蛊产物支持本命蛊继承：若配方仅产出一只凡蛊且消耗了己方本命蛊，通过 {@link ApertureService#inheritVital}
 *       按被吃本命蛊所绑空窍种类择一继承（同时吃两只时槽位靠前且空窍仍存在者胜出，另一空窍清空主修流派），并在产物放入
 *       输出格时即完成令牌绑定与存储登记。</li>
 * </ul>
 */

final class RefinementRitual {

    private static final int FULL_SUCCESS = 100;
    private static final int CURRENT_PER_MAX_SOUL = 10;
    private static final String LOST_STONES = "guzhenren.menu.refinement.lost_stones";
    private static final String LOST_ESSENCE = "guzhenren.menu.refinement.lost_essence";
    private static final String LOST_ROLL = "guzhenren.menu.refinement.lost_roll";
    private static final String ELDER_SPENT = "guzhenren.menu.refinement.elder_spent";
    private final SimpleContainer input;
    private final SimpleContainer supply;
    private final SimpleContainer output;
    private final ContainerData craftData;
    private final Runnable onEnd;
    private @Nullable GuRecipe running;
    private int @Nullable [] claimed;
    private int stage;
    private int phaseLeft;
    private boolean inWindow;
    private int stonesThisWindow;
    private int secondCounter;

    RefinementRitual(SimpleContainer input, SimpleContainer supply, SimpleContainer output, ContainerData craftData,
                     Runnable onEnd) {
        this.input = input;
        this.supply = supply;
        this.output = output;
        this.craftData = craftData;
        this.onEnd = onEnd;
    }

    boolean isRunning() { return running != null; }

    void start(GuRecipe recipe, int[] taken) {
        running = recipe;
        claimed = taken;
        stage = 0;
        secondCounter = 0;
        openWindow(recipe);
    }

    void advance(ServerPlayer server) {
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
            openWindow(recipe);
            return;
        }
        if (stonesThisWindow < recipe.getStonesFor(stage)) {
            fail(server, LOST_STONES);
            return;
        }
        if (stage + 1 >= recipe.getWindowCount()) {
            settle(server, recipe);
            return;
        }
        inWindow = false;
        phaseLeft = GuRecipe.GAP_TICKS;
        publishRun(recipe);
    }

    private void openWindow(GuRecipe recipe) {
        inWindow = true;
        stonesThisWindow = 0;
        phaseLeft = GuRecipe.WINDOW_TICKS;
        publishRun(recipe);
    }

    void stop() {
        running = null;
        claimed = null;
        stage = 0;
        phaseLeft = 0;
        inWindow = false;
        stonesThisWindow = 0;
        secondCounter = 0;
        craftData.set(RefinementMenu.DATA_RUNNING, 0);
        craftData.set(RefinementMenu.DATA_STAGE, 0);
        craftData.set(RefinementMenu.DATA_STAGES, 0);
        craftData.set(RefinementMenu.DATA_PHASE_LEFT, 0);
        craftData.set(RefinementMenu.DATA_IN_WINDOW, 0);
        craftData.set(RefinementMenu.DATA_STONES_IN, 0);
        craftData.set(RefinementMenu.DATA_STONES_NEEDED, 0);
    }

    //region stones [元石]
    private void gatherStones(ServerPlayer server, GuRecipe recipe) {
        int wanted = recipe.getStonesFor(stage) - stonesThisWindow;
        if (wanted <= 0) return;

        stonesThisWindow += takeStones(wanted);
        ItemStack held = supply.getItem(0);
        if (!(held.getItem() instanceof PrimevalElderGuItem)) return;
        if (stonesThisWindow >= recipe.getStonesFor(stage)) return;

        ActionBar.refuse(server, ELDER_SPENT);
        if (!server.getInventory().add(held.copy())) server.drop(held.copy(), false);
        supply.setItem(0, ItemStack.EMPTY);
    }

    private int takeStones(int wanted) {
        if (wanted <= 0) return 0;

        ItemStack held = supply.getItem(0);
        boolean stones = held.getItem() instanceof PrimevalStoneItem;
        int taken = PrimevalStoneSupply.takeFrom(held, wanted);
        if (stones) supply.setChanged();
        return taken;
    }

    private void refillFromSupply(ServerPlayer server) {
        if (!PrimevalStoneSupply.isTopUpNeeded(server)) return;

        long perStone = PrimevalStoneSupply.getEssencePerStone();
        long missing = PrimevalStoneSupply.topUpDeficit(server);
        if (perStone <= 0L || missing <= 0L) return;

        int wanted = (int) Math.min(Integer.MAX_VALUE, (missing + perStone - 1) / perStone);
        int drawn = takeStones(wanted);
        if (drawn > 0) ApertureEssenceService.add(server, drawn * perStone);
    }
    //endregion

    private static void burnSoul(ServerPlayer server, long amount) {
        if (amount <= 0L || SoulService.consume(server, amount)) return;

        long owed = amount - SoulService.get(server).currentSoul();
        SoulService.setCurrent(server, 0L);

        long fromMax = (owed + CURRENT_PER_MAX_SOUL - 1) / CURRENT_PER_MAX_SOUL;
        SoulService.setMax(server, Math.max(0L, SoulService.get(server).maxSoul() - fromMax));
    }

    //region settling [结算]
    private void settle(ServerPlayer server, GuRecipe recipe) {
        int chance = Math.min(FULL_SUCCESS, recipe.baseSuccess()
                + PathService.getAttainment(server, GuPath.REFINEMENT).getRefinementBonus());
        if (server.getRandom().nextInt(FULL_SUCCESS) >= chance) {
            fail(server, LOST_ROLL);
            return;
        }
        int[] taken = claimed;
        List<Integer> eatenVitals = taken == null ? List.of() : getEatenVitalKinds(server, taken);
        if (taken != null) {
            for (int i = 0; i < taken.length; i++) {
                if (taken[i] > 0) input.removeItem(i, taken[i]);
            }
        }
        deliver(server, recipe, eatenVitals);
        stop();
        onEnd.run();
    }

    private void fail(ServerPlayer server, String key) {
        int[] taken = claimed;
        if (taken != null) {
            for (int i = 0; i < taken.length; i++) {
                if (taken[i] > 0) spoil(server, i, taken[i]);
            }
        }
        ActionBar.refuse(server, key);
        stop();
        onEnd.run();
    }

    private void spoil(ServerPlayer server, int slot, int taken) {
        ItemStack stack = input.getItem(slot);
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof TendedGuItem) {
            if (GuHealth.rollRefineFailure(server, stack, server.getRandom())) input.setItem(slot, ItemStack.EMPTY);
            return;
        }
        if (stack.getItem() instanceof MortalGuItem) return;

        input.removeItem(slot, (taken + 1) / 2);
    }

    private void deliver(ServerPlayer server, GuRecipe recipe, List<Integer> eatenVitals) {
        boolean sole = recipe.getGuResultCount() == 1;
        int slot = 0;

        for (ItemStack stack : recipe.results()) {
            ItemStack made = stack.copy();
            if (made.getItem() instanceof TendedGuItem gu) {
                gu.refineAtBirth(server, made);
                if (!eatenVitals.isEmpty() && sole) inherit(server, made, gu, eatenVitals);
            }
            while (slot < RefinementMenu.OUTPUT_SIZE && !output.getItem(slot).isEmpty()) slot++;
            if (slot >= RefinementMenu.OUTPUT_SIZE) return;
            output.setItem(slot, made);
        }
    }

    private static void inherit(ServerPlayer server, ItemStack made, TendedGuItem gu, List<Integer> eatenVitals) {
        int heir = ApertureService.inheritVital(server, eatenVitals, gu.getPath());
        if (heir < 0) return;

        VitalGuState.bind(made, server, heir);
        ApertureStorageService.adoptVital(server, heir, made);
    }

    private List<Integer> getEatenVitalKinds(ServerPlayer server, int[] taken) {
        List<Integer> kinds = new ArrayList<>();
        for (int i = 0; i < taken.length; i++) {
            ItemStack stack = input.getItem(i);
            if (taken[i] > 0 && VitalGuState.isCurrentVital(stack, server)) {
                kinds.add(VitalGuState.getBoundAperture(stack));
            }
        }
        return kinds;
    }
    //endregion

    private void publishRun(GuRecipe recipe) {
        craftData.set(RefinementMenu.DATA_RUNNING, 1);
        craftData.set(RefinementMenu.DATA_STAGE, stage);
        craftData.set(RefinementMenu.DATA_STAGES, recipe.getWindowCount());
        craftData.set(RefinementMenu.DATA_PHASE_LEFT, phaseLeft);
        craftData.set(RefinementMenu.DATA_IN_WINDOW, inWindow ? 1 : 0);
        craftData.set(RefinementMenu.DATA_STONES_IN, stonesThisWindow);
        craftData.set(RefinementMenu.DATA_STONES_NEEDED, recipe.getStonesFor(stage));
    }
}
