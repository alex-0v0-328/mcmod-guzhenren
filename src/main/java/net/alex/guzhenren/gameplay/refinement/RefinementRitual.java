package net.alex.guzhenren.gameplay.refinement;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.space.PrimevalElderGuItem;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One refinement [炼蛊] run, ticked by the {@link RefinementMenu} that owns it: windows [窗口] for stones and
 * gaps between them, essence [真元] and soul [魂魄] paid every second, and the settlement at the end. The menu
 * stays the clock -- it calls {@link #advance} from its own {@code broadcastChanges} and {@link #stop} when
 * it closes -- so closing the window still aborts the run by construction.
 *
 * <p>Nothing is consumed until the settlement: success takes the claimed inputs and fills the output; failure
 * takes half of each material and wounds each Gu by a quarter of its health. Every live figure is published
 * into the menu's {@link ContainerData}, because the client cannot match a recipe itself.
 *
 * <p>Soul is paid from the current pool first, then from {@code maxSoul} at a tenth of the rate, which is
 * what kills. Within a window the supply slot feeds the stones first; outside the window it only tops the
 * essence pool up, never past what the window did not want. A Primeval Elder Gu [元老蛊] that runs dry
 * mid-window is handed back to the bag.
 *
 * @author Alex
 * @version 1.0.0
 * @see RefinementMenu
 * @see GuRecipe
 * @since 1.0.0
 */

final class RefinementRitual {

    private static final int FAILURE_HEALTH_DIVISOR = 4;
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
        inWindow = true;
        stonesThisWindow = 0;
        secondCounter = 0;
        phaseLeft = GuRecipe.WINDOW_TICKS;
        publishRun(recipe);
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

    //region stones -- the window takes first; outside it the supply only tops the pool up
    private void gatherStones(ServerPlayer server, GuRecipe recipe) {
        int wanted = recipe.stonesFor(stage) - stonesThisWindow;
        if (wanted <= 0) return;

        stonesThisWindow += takeStones(wanted);
        ItemStack held = supply.getItem(0);
        if (!(held.getItem() instanceof PrimevalElderGuItem)) return;
        if (stonesThisWindow >= recipe.stonesFor(stage)) return;

        RefinementMenu.say(server, ELDER_SPENT, ChatFormatting.RED);
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
        if (!PrimevalStoneSupply.needsTopUp(server)) return;

        long perStone = PrimevalStoneSupply.essencePerStone();
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

    //region settling -- nothing is consumed until here, so failure can take half and wound the rest
    private void settle(ServerPlayer server, GuRecipe recipe) {
        int chance = Math.min(FULL_SUCCESS, recipe.baseSuccess()
                + PathService.getAttainment(server, GuPath.REFINEMENT).getRefinementBonus());
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
        onEnd.run();
    }

    private void fail(ServerPlayer server, String key) {
        int[] taken = claimed;
        if (taken != null) {
            for (int i = 0; i < taken.length; i++) {
                if (taken[i] > 0) spoil(server, i, taken[i]);
            }
        }
        RefinementMenu.say(server, key, ChatFormatting.RED);
        stop();
        onEnd.run();
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

    private void deliver(ServerPlayer server, GuRecipe recipe, boolean vital) {
        boolean sole = recipe.guResultCount() == 1;
        int slot = 0;

        for (ItemStack stack : recipe.results()) {
            ItemStack made = stack.copy();
            if (made.getItem() instanceof TendedGuItem gu) {
                gu.bornRefined(server, made);
                if (vital && sole) inherit(server, made, gu);
            }
            while (slot < RefinementMenu.OUTPUT_SIZE && !output.getItem(slot).isEmpty()) slot++;
            if (slot >= RefinementMenu.OUTPUT_SIZE) return;
            output.setItem(slot, made);
        }
    }

    private static void inherit(ServerPlayer server, ItemStack made, TendedGuItem gu) {
        GuItem.bind(made, server, ApertureData.PRIMARY);
        ApertureService.setPrimaryPath(server, ApertureData.PRIMARY, gu.path());
    }

    private boolean eatsVital(int[] taken) {
        for (int i = 0; i < taken.length; i++) {
            if (taken[i] > 0 && GuItem.isVital(input.getItem(i))) return true;
        }
        return false;
    }
    //endregion

    private void publishRun(GuRecipe recipe) {
        craftData.set(RefinementMenu.DATA_RUNNING, 1);
        craftData.set(RefinementMenu.DATA_STAGE, stage);
        craftData.set(RefinementMenu.DATA_STAGES, recipe.windowCount());
        craftData.set(RefinementMenu.DATA_PHASE_LEFT, phaseLeft);
        craftData.set(RefinementMenu.DATA_IN_WINDOW, inWindow ? 1 : 0);
        craftData.set(RefinementMenu.DATA_STONES_IN, stonesThisWindow);
        craftData.set(RefinementMenu.DATA_STONES_NEEDED, recipe.stonesFor(stage));
    }
}
