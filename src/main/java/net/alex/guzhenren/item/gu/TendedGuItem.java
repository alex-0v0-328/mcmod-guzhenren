package net.alex.guzhenren.item.gu;

import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.lifecycle.PlayerDataService;
import net.alex.guzhenren.gameplay.path.time.PathTimeFlowService;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A tended Gu [需照顾]: wild, then refined [炼化], then fed and used, all on one shared state record. The
 * middle class between {@link MortalGuItem} and every leaf that needs feeding: a leaf answers
 * {@code payoutGate} / {@code payout} plus any limit it bends; owns the charge ladder, channeling [灌注],
 * hunger/health billing, cooldown stamps, and the container day-rollover walk.
 *
 * <p>⚠ The billing step runs decay, then auto-feed, then warn. Nothing in the code makes that order
 * look load-bearing, and swapping any two of them changes which Gu survive a day.
 *
 * <p>⚠ {@link #canBeVital}: a Gu taken by its own use may never be bound, or the slot would lose it on
 * the very first click. ⚠ {@link #cooldownStamp}: stamped BACK by the share a hastened clock has
 * already served, because the stamp is read again long after the form has ended -- scaling the window
 * on the way out would recompute a live cooldown.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuSpec
 * @see GuClock
 * @see RefinedGuState
 * @since 1.0.0
 */

public abstract class TendedGuItem extends MortalGuItem {

    private static final String TOOLTIP_REFINE = "guzhenren.item.gu.refine_progress";
    private static final String TOOLTIP_INVESTED = "guzhenren.item.gu.invested";
    private static final String TOOLTIP_HUNGER = "guzhenren.item.gu.hunger_progress";
    private static final String TOOLTIP_HEALTH = "guzhenren.item.gu.health";
    private static final String CAPTION_CHANNELING = "guzhenren.hud.using";
    private static final String FAILED_STARVING = "guzhenren.item.failed.gu_starving";
    private static final String MSG_HUNGRY = "guzhenren.item.gu.hungry";
    private static final String MSG_STARVED = "guzhenren.item.gu.starved";
    private static final String MSG_EXHAUSTED = "guzhenren.item.gu.exhausted";
    private static final String MSG_RUINED = "guzhenren.item.gu.ruined";
    private static final int ESSENCE_FLOOR = 20;
    protected final GuClock clock;

    protected TendedGuItem(Properties properties, GuSpec spec) {
        super(properties, spec);
        this.clock = spec.buildClock();
    }

    //region what a leaf must answer
    protected abstract @Nullable Refusal payoutGate(Player player, ItemStack stack);

    protected abstract void payout(ServerPlayer player, ItemStack stack);

    protected int feedUnits(ItemStack food) { return spec.feedUnits(food); }

    public boolean canBeVital() { return true; }
    //endregion

    //region state
    public static RefinedGuState state(ItemStack s) {
        return s.getOrDefault(ModDataComponents.REFINED_GU_STATE.get(), RefinedGuState.WILD);
    }

    private void store(ItemStack stack, RefinedGuState s) {
        stack.set(ModDataComponents.REFINED_GU_STATE.get(), new RefinedGuState(s.refined(),
                Math.min(s.refineProgress(), refineCost()),
                Math.min(s.investedEssence(), spec.essencePerRound()),
                s.hunger(),
                Math.clamp(s.damageTaken(), 0, maxHealth())));
    }

    public boolean refined(ItemStack stack) { return state(stack).refined(); }

    public boolean hungry(ServerPlayer p, ItemStack s) { return refined(s) && clock.hungry(p, s); }
    //endregion

    //region Gu health [蛊虫生命值] -- stored as damage TAKEN, so an untouched 野生 Gu reads as full
    public static final int HEALTH_PER_RANK = 12;

    public int maxHealth() { return HEALTH_PER_RANK; }

    public int health(ItemStack stack) { return maxHealth() - state(stack).damageTaken(); }

    public boolean damageKills(ServerPlayer holder, ItemStack stack, int amount) {
        if (amount <= 0) return false;

        RefinedGuState s = state(stack);
        int taken = Math.min(maxHealth(), s.damageTaken() + amount);
        store(stack, s.withDamageTaken(taken));
        if (taken < maxHealth() || isVital(stack)) return false;

        ruined(holder, stack);
        return true;
    }

    protected void heal(ItemStack stack, int amount) {
        RefinedGuState s = state(stack);
        if (amount <= 0 || !s.refined()) return;

        store(stack, s.withDamageTaken(Math.max(0, s.damageTaken() - amount)));
    }

    record Meal(int items, int gained, int eaten) {}

    static Meal portion(int count, int need, int per, int units) {
        int items = Math.min(count, need * per / units);
        int gained = items * units / per;
        return new Meal(items, gained, gained <= 0 ? 0 : (gained * per + units - 1) / units);
    }

    private void healFrom(ServerPlayer player, ItemStack stack, ItemStack food) {
        int units = feedUnits(food);
        int hurt = state(stack).damageTaken();
        if (units <= 0 || hurt <= 0) return;

        Meal meal = portion(food.getCount(), hurt, spec.unitsPerHealth(), units);
        if (meal.gained() <= 0) return;

        if (!player.hasInfiniteMaterials()) {
            returnEmptyContainers(player, food, meal.eaten());
            food.shrink(meal.eaten());
        }
        heal(stack, meal.gained());
    }

    public static void returnEmptyContainers(ServerPlayer player, ItemStack food, int eaten) {
        if (!food.hasCraftingRemainingItem()) return;

        ItemStack empties = food.getCraftingRemainingItem().copyWithCount(eaten);
        if (!player.getInventory().add(empties)) player.drop(empties, false);
    }
    //endregion

    //region refining [炼化] -- paid in instalments; what it buys is a lasting bond
    public static final int POST_REFINE_COOLDOWN_TICKS = Ticks.SECOND;

    private void refineStep(ServerPlayer player, ItemStack stack, int invest) {
        RefinedGuState s = state(stack);
        int next = s.refineProgress() + invest;
        if (next < refineCost()) {
            store(stack, s.withRefine(next));
            return;
        }
        bornRefined(player, stack);
    }

    public void bornRefined(ServerPlayer player, ItemStack stack) {
        store(stack, new RefinedGuState(true, refineCost(), 0, 0, state(stack).damageTaken()));
        clock.bind(player, stack);
        stack.set(ModDataComponents.REFINED_AT.get(), cooldownStamp(player, POST_REFINE_COOLDOWN_TICKS));
        applyPostRefineCooldown(player.getCooldowns(), this,
                PathTimeFlowService.waited(player, POST_REFINE_COOLDOWN_TICKS));
    }
    //endregion

    //region channeling [灌注] -- Human Jun [人力钧力流] and the boars
    private static final int CHANNEL_MAX_TICKS = 72_000;
    private static final int GU_PACED_STEP_TICKS = 1;
    private static final int POOL_PACED_STEP_TICKS = 5;
    private static final int POOL_PACED_STEPS = Ticks.SECOND / POOL_PACED_STEP_TICKS;

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack,
                          int remaining) {
        if (!(entity instanceof ServerPlayer player)) return;

        int elapsed = CHANNEL_MAX_TICKS - remaining;
        if (elapsed < 0 || elapsed % stepTicks(player) != 0) return;

        if (!refined(stack)) {
            refineTick(player, stack, elapsed);
            return;
        }
        if (!spec.channels()) return;

        if (holdingFood(player, stack)) eat(player, stack);
        drawFromOffhandStones(player);
        int take = stepAmount(player, stack, elapsed);
        if (take <= 0 || !ApertureEssenceService.consume(player, take)) {
            player.stopUsingItem();
            return;
        }
        pour(player, stack, take);
    }

    private void refineTick(ServerPlayer player, ItemStack stack, int elapsed) {
        drawFromOffhandStones(player);
        int left = refineCost() - state(stack).refineProgress();
        int take = (int) Math.min(left, poured(player, elapsed));
        if (take <= 0 || !ApertureEssenceService.consume(player, take)) {
            player.stopUsingItem();
            return;
        }
        refineStep(player, stack, take);
        if (refined(stack)) player.stopUsingItem();
    }

    private boolean guPaced(Player player) { return rankGap(player) > 0; }

    private int stepTicks(Player player) {
        return guPaced(player) ? GU_PACED_STEP_TICKS : POOL_PACED_STEP_TICKS;
    }

    private void drawFromOffhandStones(ServerPlayer player) {
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof PrimevalStoneItem stone)) return;

        int used = stone.used(player, offhand);
        if (used <= 0) return;

        ApertureEssenceService.add(player, stone.essence() * used);
        if (!player.hasInfiniteMaterials()) offhand.shrink(used);
    }

    private int stepAmount(ServerPlayer player, ItemStack stack, int elapsed) {
        long pool = ApertureEssenceService.spendable(player);
        if (pool < ESSENCE_FLOOR) return 0;

        int round = spec.essencePerRound();
        int leftInThisRound = round - state(stack).investedEssence() % round;
        long thisStep = Math.min(mostThisStepMaySpend(player, pool, elapsed), clock.essenceAboveHungerFloor(stack));

        return (int) Math.min(thisStep, leftInThisRound);
    }

    private long mostThisStepMaySpend(ServerPlayer player, long pool, int elapsed) {
        if (guPaced(player)) {
            return Math.min(pool, PathTimeFlowService.perStep(player, clock.essencePerHungerPoint()));
        }
        return poolPacedStep(player, pool, elapsed);
    }

    private long poured(ServerPlayer player, int elapsed) {
        long pool = ApertureEssenceService.spendable(player);
        if (pool < ESSENCE_FLOOR) return 0L;
        return guPaced(player) ? pool : poolPacedStep(player, pool, elapsed);
    }

    private static long poolPacedStep(ServerPlayer player, long pool, int elapsed) {
        int stepIndex = (elapsed % Ticks.SECOND) / POOL_PACED_STEP_TICKS;
        return Math.min(pool, PathTimeFlowService.perStep(player, pool / (POOL_PACED_STEPS + 1 - stepIndex)));
    }

    private void pour(ServerPlayer player, ItemStack stack, int amount) {
        int pouredBefore = state(stack).investedEssence();
        int pouredAfter = pouredBefore + amount;

        clock.billHungerForEssence(stack, pouredBefore, pouredAfter);
        store(stack, state(stack).withInvested(pouredAfter));

        if (pouredAfter >= spec.essencePerRound()) {
            grant(player, stack);
            store(stack, state(stack).withInvested(0));
            if (payoutGate(player, stack) != null) player.stopUsingItem();
        }
    }

    private void grant(ServerPlayer player, ItemStack stack) {
        payout(player, stack);
        if (spec.effectCooldownTicks() > 0) {
            stack.set(ModDataComponents.USED_AT.get(), cooldownStamp(player, spec.effectCooldownTicks()));
        }
    }
    //endregion

    //region the long cooldown -- vanilla draws the sweep, the stack's stamp is the truth
    private static final String FAILED_COOLDOWN = "guzhenren.item.failed.gu_cooldown";

    private static long gameTime(ServerPlayer player) { return player.server.overworld().getGameTime(); }

    private static long cooldownStamp(ServerPlayer player, int window) {
        return gameTime(player) - (window - PathTimeFlowService.waited(player, window));
    }

    static int stampCooldownLeft(long now, @Nullable Long stamp, int window) {
        if (stamp == null || window <= 0) return 0;

        long elapsed = now - stamp;
        return elapsed < 0L || elapsed >= window ? 0 : (int) (window - elapsed);
    }

    static void applyPostRefineCooldown(ItemCooldowns cooldowns, Item item, int wanted) {
        if (!cooldowns.isOnCooldown(item)) cooldowns.addCooldown(item, wanted);
    }

    private int cooldownLeft(Player player, @Nullable Long stamp, int window) {
        MinecraftServer server = player.getServer();
        return server == null ? 0 : stampCooldownLeft(server.overworld().getGameTime(), stamp, window);
    }

    private int useCooldownLeft(Player p, ItemStack s) {
        return cooldownLeft(p, s.get(ModDataComponents.USED_AT.get()), spec.effectCooldownTicks());
    }

    private @Nullable Refusal cooldownRefusal(Player player, ItemStack stack) {
        int left = Math.max(allowsUseDuringEffectCooldown() ? 0 : useCooldownLeft(player, stack),
                cooldownLeft(player, stack.get(ModDataComponents.REFINED_AT.get()), POST_REFINE_COOLDOWN_TICKS));
        if (left <= 0) return null;

        if (player instanceof ServerPlayer server) server.getCooldowns().addCooldown(this, left);
        long seconds = (left + Ticks.SECOND - 1) / Ticks.SECOND;
        return new Refusal(FAILED_COOLDOWN, Component.literal(String.valueOf(seconds)));
    }

    @Override
    protected void spend(ServerPlayer player, ItemStack stack, int count) {
        super.spend(player, stack, count);
        int left = Math.max(PathTimeFlowService.waited(player, spec.itemCooldownTicks()),
                cooldownLeft(player, stack.get(ModDataComponents.REFINED_AT.get()), POST_REFINE_COOLDOWN_TICKS));
        if (left > 0) player.getCooldowns().addCooldown(this, left);
    }
    //endregion

    //region the clicks
    @Override
    protected boolean feedsFromOffhand() { return true; }

    @Override
    protected final @Nullable Refusal gate(Player player, ItemStack stack) {
        if (holdingFood(player, stack)) return null;
        if (!refined(stack)) {
            return essenceGate(player, Math.min(ESSENCE_FLOOR, refineCost()), FAILED_REFINE_ESSENCE);
        }
        Refusal poor = essenceGate(player, useThreshold(stack), FAILED_ESSENCE);
        if (poor != null) return poor;

        if (spec.channels() && clock.essenceAboveHungerFloor(stack) <= 0) return new Refusal(FAILED_STARVING);

        Refusal cooling = cooldownRefusal(player, stack);
        return cooling != null ? cooling : useGate(player, stack);
    }

    protected long useThreshold(ItemStack stack) {
        return spec.channels() ? ESSENCE_FLOOR : spec.essencePerRound();
    }

    @Override
    protected @Nullable Refusal useGate(Player player, ItemStack stack) { return payoutGate(player, stack); }

    @Override
    protected final int apply(ServerPlayer player, ItemStack stack) {
        if (!refined(stack)) return 0;
        if (holdingFood(player, stack) && !crouching(player)) {
            eat(player, stack);
            return 0;
        }
        return useApply(player, stack);
    }

    @Override
    protected int useApply(ServerPlayer player, ItemStack stack) {
        return spec.channels() ? 0 : drive(player, stack);
    }

    @Override
    protected final int useDurationTicks(Player player, ItemStack stack) {
        if (!refined(stack)) return CHANNEL_MAX_TICKS;
        if (holdingFood(player, stack) && !crouching(player)) return 0;
        return spec.channels() ? CHANNEL_MAX_TICKS : useChargeTicks(player, stack);
    }

    protected int useChargeTicks(Player player, ItemStack stack) { return useChargeByGap(player); }

    protected int drive(ServerPlayer player, ItemStack stack) {
        ApertureEssenceService.consume(player, spec.essencePerRound());
        boolean drivenOnAnEmptyBar = clock.spendWasForced(stack, hungerCostMultiplier(player, stack));
        grant(player, stack);

        if (drivenOnAnEmptyBar && !player.hasInfiniteMaterials()) {
            exhausted(player, stack);
            return 1;
        }
        eat(player, stack);
        if (hungry(player, stack)) clock.warn(player, stack, 1L);
        return 0;
    }

    @Override
    protected boolean hasSneakUse(Player player, ItemStack stack) { return holdingFood(player, stack); }

    @Override
    protected @Nullable Refusal sneakGate(Player player, ItemStack stack) {
        Refusal poor = essenceGate(player, useThreshold(stack), FAILED_ESSENCE);
        if (poor != null) return poor;

        Refusal cooling = cooldownRefusal(player, stack);
        return cooling != null ? cooling : payoutGate(player, stack);
    }

    @Override
    protected int sneakApply(ServerPlayer player, ItemStack stack) {
        eat(player, stack);
        return spec.channels() ? 0 : drive(player, stack);
    }

    protected boolean holdingFood(Player player, ItemStack stack) {
        return refined(stack) && feedUnits(player.getOffhandItem()) > 0;
    }

    protected boolean allowsUseDuringEffectCooldown() { return false; }

    protected int hungerCostMultiplier(Player player, ItemStack stack) { return 1; }

    protected final int effectCooldownLeft(Player player, ItemStack stack) {
        return cooldownLeft(player, stack.get(ModDataComponents.USED_AT.get()), spec.effectCooldownTicks());
    }

    public final boolean autoUse(ServerPlayer player, ItemStack stack) {
        if (!refined(stack) || spec.channels() || player.getCooldowns().isOnCooldown(this)) return false;
        if (essenceGate(player, useThreshold(stack), FAILED_ESSENCE) != null
                || cooldownRefusal(player, stack) != null
                || useGate(player, stack) != null) return false;
        spend(player, stack, useApply(player, stack));
        return true;
    }

    protected void eat(ServerPlayer player, ItemStack stack) {
        ItemStack food = player.getOffhandItem();
        healFrom(player, stack, food);
        clock.eat(this, player, stack, food);
    }
    //endregion

    //region display
    @Override
    public Component chargeCaption(ItemStack stack, int remainingTicks) {
        if (!refined(stack)) return refineCaption(state(stack).refineProgress());
        return spec.channels()
                ? Component.translatable(CAPTION_CHANNELING, state(stack).investedEssence(),
                spec.essencePerRound())
                : Component.translatable(CAPTION_USING_PLAIN);
    }

    @Override
    public @Nullable Float chargeFraction(ItemStack stack, int remainingTicks) {
        if (!refined(stack)) return refineFraction(stack);
        if (!spec.channels()) return null;
        return state(stack).investedEssence() / (float) spec.essencePerRound();
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return refined(stack) ? super.getName(stack) : ModDisplayText.wild(super.getName(stack));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (state(stack).damageTaken() <= 0) return;

        tooltip.add(Component.translatable(TOOLTIP_HEALTH, health(stack), maxHealth())
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected @Nullable MutableComponent progressLine(ItemStack stack) {
        if (!refined(stack)) {
            return Component.translatable(TOOLTIP_REFINE, state(stack).refineProgress(), refineCost());
        }
        if (spec.channels()) {
            return Component.translatable(TOOLTIP_INVESTED, state(stack).investedEssence(),
                    spec.essencePerRound());
        }
        return clock instanceof GuClock.HungerBar bar
                ? Component.translatable(TOOLTIP_HUNGER, state(stack).hunger(), bar.max())
                : null;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return refined(stack) ? clock.barVisible(stack) : state(stack).refineProgress() > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) { return Math.round(barFraction(stack) * 13.0F); }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return Mth.hsvToRgb(barFraction(stack) / 3.0F, 1.0F, 1.0F);
    }

    private float barFraction(ItemStack stack) {
        return refined(stack) ? clock.barFraction(stack) : refineFraction(stack);
    }

    private float refineFraction(ItemStack stack) {
        return refineCost() <= 0 ? 0.0F : state(stack).refineProgress() / (float) refineCost();
    }
    //endregion

    //region the day clock -- three containers bill through here, on either clock
    public static boolean tickInContainer(ServerPlayer player, ItemStack stack, long days) {
        return tickOne(player, stack, days, true);
    }

    private static boolean tickOne(ServerPlayer player, ItemStack stack, long days, boolean autoFeeds) {
        if (!(stack.getItem() instanceof TendedGuItem gu) || !gu.refined(stack)) return false;

        gu.payOwnUpkeep(player, stack);
        if (gu.clock.starves(player, stack, days)) return true;

        if (autoFeeds && gu.autoFeed(player, stack)) return false;
        if (gu.clock.hungry(player, stack)) gu.clock.warn(player, stack, days);
        return false;
    }

    protected void payOwnUpkeep(ServerPlayer player, ItemStack stack) {}

    public static void tickCarried(ServerPlayer player, long days) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (tickOne(player, stack, days, false)) {
                inventory.setItem(slot, ItemStack.EMPTY);
                starved(player, stack);
            }
        }
    }

    public boolean autoFeed(ServerPlayer player, ItemStack stack) {
        if (!clock.hungry(player, stack)) return false;

        Inventory inventory = player.getInventory();
        boolean ate = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (clock.eat(this, player, stack, inventory.getItem(slot))) ate = true;
        }
        return ate;
    }
    //endregion

    //region the two deaths -- one funnel, two messages, and a Vital Gu's owner pays either way
    public static void announce(ServerPlayer player, ItemStack stack, String key) {
        player.sendSystemMessage(Component.translatable(key, stack.getHoverName()));
    }

    public static void announceHungry(ServerPlayer p, ItemStack s) { announce(p, s, MSG_HUNGRY); }

    private static void died(ServerPlayer holder, ItemStack stack, String key) {
        announce(holder, stack, key);
        UUID uuid = owner(stack);
        if (uuid == null) return;

        ServerPlayer owner = holder.server.getPlayerList().getPlayer(uuid);
        if (owner != null) PlayerDataService.onVitalGuLost(owner, stack);
        else PlayerDataService.recordOfflineVitalLoss(holder.server, uuid, stack);
    }

    public static void starved(ServerPlayer holder, ItemStack s) { died(holder, s, MSG_STARVED); }

    public static void exhausted(ServerPlayer holder, ItemStack s) { died(holder, s, MSG_EXHAUSTED); }

    public static void ruined(ServerPlayer holder, ItemStack s) { died(holder, s, MSG_RUINED); }
    //endregion
}
