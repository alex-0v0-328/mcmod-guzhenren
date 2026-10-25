package net.alex.guzhenren.item.gu;

import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A tended Gu [需照顾]: wild, then refined [炼化], then fed and used, all on one shared state record. The
 * middle class between {@link MortalGuItem} and every leaf that needs feeding: a leaf answers
 * {@code checkPayoutGate} / {@code payOut} plus any limit it bends; owns the charge ladder, channeling [灌注]
 * and hunger/health billing. Meals are {@link GuMeal}, cooldown stamps {@link GuCooldowns}, the
 * day-rollover walk {@link GuUpkeep}.
 *
 * <p>Still past 300 lines on purpose: what is left is the vanilla {@code Item} overrides and the template
 * hooks every leaf overrides, and channeling is this item's own {@code onUseTick}, reading and writing ten
 * of its members -- pulled out, all of them would have to open up.
 *
 * <p>⚠ {@link #isHoldingFood} asks only "refined, and the offhand holds this Gu's food". It routes the gate,
 * {@code apply}, the charge length, the channel step and {@code hasSneakUse}, so the hunger bar and the
 * cooldowns stay out of it: inside it they would change what the same click does.
 *
 * <p>⚠ {@code checkCooldown} is the one cooldown gate for the plain use, the sneak use and {@link #tryAutoUse}:
 * the larger of the effect cooldown (waived by {@code canUseDuringEffectCooldown}) and the post-refine guard. A
 * refusal puts the ticks left back on the vanilla cooldown, so a lost sweep redraws. {@code spend} runs
 * {@code super.spend} first and only then puts the largest of the item cooldown, the post-refine guard and the
 * effect cooldown: {@code ItemCooldowns.addCooldown} overwrites unconditionally, so the other order would lose
 * the long cooldown to the base's short one.
 *
 * <p>A use on an empty hunger bar is forced ({@code trySpendAndGrant}): the effect lands, then the Gu dies of
 * exhaustion, and the 1 that {@code drive} returns is that death, not a payout.
 *
 * <p>⚠ Refining and channeling advance one step every {@code STEP_TICKS}, at two paces that stay separate. A
 * holder above the Gu's rank is Gu-paced: a channel step pours at most
 * {@code getEssencePerHungerPoint() * STEP_TICKS}, scaled by {@code TimeFlow}, and a refining step may pour the
 * whole pool. At the Gu's rank or below, the pool sets the pace: 1/5, 1/4, 1/3, then 1/2 of the pool per step,
 * 80% of it a second at normal time flow. Each channel step eats from the offhand before it sizes the essence,
 * because the step is capped by the essence above the hunger floor, which the meal raises.
 *
 * <p>⚠ {@link #canBeVital}: a Gu taken by its own use may never be bound, or the slot would lose it on
 * the very first click. Gu health is {@link #GU_MAX_HEALTH} for every tended Gu, overridden through
 * {@link #getMaxHealth}; it stays off {@code Rank}, whose {@code getMaxHealth} is the player's. Healing from food
 * reuses the hunger rate: one health point costs what one hunger point does ({@code GuSpec.unitsPerHunger}).
 *
 * <p>{@code announceDeath} is the one death funnel ({@link #starve}, {@link #exhaust}, {@link #ruin}) and the
 * only poster of {@link VitalGuLostEvent}; a new way for a Gu to die goes through it.
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
    protected abstract @Nullable Refusal checkPayoutGate(Player player, ItemStack stack);

    protected abstract void payOut(ServerPlayer player, ItemStack stack);

    protected int feedUnits(ItemStack food) { return spec.feedUnits(food); }

    public boolean canBeVital() { return true; }
    //endregion

    //region state
    public static RefinedGuState getState(ItemStack stack) {
        return ModDataComponents.getOrDefault(stack, ModDataComponents.REFINED_GU_STATE, RefinedGuState.WILD);
    }

    private void store(ItemStack stack, RefinedGuState state) {
        ModDataComponents.set(stack, ModDataComponents.REFINED_GU_STATE, new RefinedGuState(state.refined(),
                Math.min(state.refineProgress(), getRefineCost()),
                Math.min(state.investedEssence(), spec.getEssencePerRound()),
                state.hunger(),
                Mth.clamp(state.damageTaken(), 0, getMaxHealth())));
    }

    public boolean isRefined(ItemStack stack) { return getState(stack).refined(); }

    public boolean isHungry(ServerPlayer player, ItemStack stack) {
        return isRefined(stack) && clock.isHungry(player, stack);
    }
    //endregion

    //region Gu health [蛊虫生命值] -- stored as damage TAKEN, so an untouched 野生 Gu reads as full
    public static final int GU_MAX_HEALTH = 12;

    public int getMaxHealth() { return GU_MAX_HEALTH; }

    public int getHealth(ItemStack stack) { return getMaxHealth() - getState(stack).damageTaken(); }

    public boolean takeDamage(ServerPlayer holder, ItemStack stack, int amount) {
        if (amount <= 0) return false;

        RefinedGuState s = getState(stack);
        int taken = Math.min(getMaxHealth(), s.damageTaken() + amount);
        store(stack, s.withDamageTaken(taken));
        if (taken < getMaxHealth() || isVital(stack)) return false;

        ruin(holder, stack);
        return true;
    }

    protected void heal(ItemStack stack, int amount) {
        RefinedGuState s = getState(stack);
        if (amount <= 0 || !s.refined()) return;

        store(stack, s.withDamageTaken(Math.max(0, s.damageTaken() - amount)));
    }

    private void healFrom(ServerPlayer player, ItemStack stack, ItemStack food) {
        int units = feedUnits(food);
        int hurt = getState(stack).damageTaken();
        if (units <= 0 || hurt <= 0) return;

        GuMeal meal = GuMeal.createPortion(food.getCount(), hurt, spec.getUnitsPerHunger(), units);
        if (meal.gained() <= 0) return;

        if (!player.getAbilities().instabuild) {
            GuMeal.returnEmptyContainers(player, food, meal.eaten());
            food.shrink(meal.eaten());
        }
        heal(stack, meal.gained());
    }
    //endregion

    //region refining [炼化] -- paid in instalments; what it buys is a lasting bond
    public static final int POST_REFINE_COOLDOWN_TICKS = Ticks.SECOND;

    private void refineStep(ServerPlayer player, ItemStack stack, int invest) {
        RefinedGuState s = getState(stack);
        int next = s.refineProgress() + invest;
        if (next < getRefineCost()) {
            store(stack, s.withRefine(next));
            return;
        }
        refineAtBirth(player, stack);
    }

    public void refineAtBirth(ServerPlayer player, ItemStack stack) {
        store(stack, new RefinedGuState(true, getRefineCost(), 0, 0, getState(stack).damageTaken()));
        clock.bind(player, stack);
        ModDataComponents.set(stack, ModDataComponents.REFINED_AT,
                GuCooldowns.createCooldownStamp(player, POST_REFINE_COOLDOWN_TICKS));
        GuCooldowns.applyPostRefineCooldown(player.getCooldowns(), this,
                TimeFlow.shortenWait(player, POST_REFINE_COOLDOWN_TICKS));
    }
    //endregion

    //region channeling [灌注] -- Human Jun [人力钧力流] and the boars
    private static final int CHANNEL_MAX_TICKS = 72_000;
    private static final int STEP_TICKS = 5;
    private static final int STEPS_PER_SECOND = Ticks.SECOND / STEP_TICKS;

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack,
                          int remaining) {
        if (!(entity instanceof ServerPlayer player)) return;

        int elapsed = CHANNEL_MAX_TICKS - remaining;
        if (elapsed < 0 || elapsed % STEP_TICKS != 0) return;

        if (!isRefined(stack)) {
            refineTick(player, stack, elapsed);
            return;
        }
        if (!spec.isChanneled()) return;

        if (isHoldingFood(player, stack)) eat(player, stack);
        PrimevalStoneSupply.fillFromOffhand(player);
        int take = stepAmount(player, stack, elapsed);
        if (take <= 0 || !ApertureEssenceService.consume(player, take)) {
            player.stopUsingItem();
            return;
        }
        pour(player, stack, take);
    }

    private void refineTick(ServerPlayer player, ItemStack stack, int elapsed) {
        PrimevalStoneSupply.fillFromOffhand(player);
        int left = getRefineCost() - getState(stack).refineProgress();
        int take = (int) Math.min(left, getPoured(player, elapsed));
        if (take <= 0 || !ApertureEssenceService.consume(player, take)) {
            player.stopUsingItem();
            return;
        }
        refineStep(player, stack, take);
        if (isRefined(stack)) player.stopUsingItem();
    }

    private boolean isGuPaced(Player player) { return getRankGap(player) > 0; }

    private int stepAmount(ServerPlayer player, ItemStack stack, int elapsed) {
        long pool = ApertureEssenceService.getSpendable(player);
        if (pool < ESSENCE_FLOOR) return 0;

        int round = spec.getEssencePerRound();
        int leftInThisRound = round - getState(stack).investedEssence() % round;
        long thisStep = Math.min(getMostThisStepMaySpend(player, pool, elapsed),
                clock.getEssenceAboveHungerFloor(stack));

        return (int) Math.min(thisStep, leftInThisRound);
    }

    private long getMostThisStepMaySpend(ServerPlayer player, long pool, int elapsed) {
        if (isGuPaced(player)) {
            return Math.min(pool, TimeFlow.scale(player, (long) clock.getEssencePerHungerPoint() * STEP_TICKS));
        }
        return getPoolPacedStep(player, pool, elapsed);
    }

    private long getPoured(ServerPlayer player, int elapsed) {
        long pool = ApertureEssenceService.getSpendable(player);
        if (pool < ESSENCE_FLOOR) return 0L;
        return isGuPaced(player) ? pool : getPoolPacedStep(player, pool, elapsed);
    }

    private static long getPoolPacedStep(ServerPlayer player, long pool, int elapsed) {
        int stepIndex = (elapsed % Ticks.SECOND) / STEP_TICKS;
        return Math.min(pool, TimeFlow.scale(player, pool / (STEPS_PER_SECOND + 1 - stepIndex)));
    }

    private void pour(ServerPlayer player, ItemStack stack, int amount) {
        int pouredBefore = getState(stack).investedEssence();
        int pouredAfter = pouredBefore + amount;

        clock.billHungerForEssence(stack, pouredBefore, pouredAfter);
        store(stack, getState(stack).withInvested(pouredAfter));

        if (pouredAfter >= spec.getEssencePerRound()) {
            grant(player, stack);
            store(stack, getState(stack).withInvested(0));
            if (checkPayoutGate(player, stack) != null) player.stopUsingItem();
        }
    }

    private void grant(ServerPlayer player, ItemStack stack) {
        payOut(player, stack);
        if (spec.getEffectCooldownTicks() > 0) {
            ModDataComponents.set(stack, ModDataComponents.USED_AT,
                    GuCooldowns.createCooldownStamp(player, spec.getEffectCooldownTicks()));
        }
    }
    //endregion

    //region the long cooldown -- vanilla draws the sweep, the stack's stamp is the truth
    private static final String FAILED_COOLDOWN = "guzhenren.item.failed.gu_cooldown";

    private int postRefineCooldownLeft(Player player, ItemStack stack) {
        Long refinedAt = ModDataComponents.get(stack, ModDataComponents.REFINED_AT);
        return GuCooldowns.getCooldownLeft(player, refinedAt, POST_REFINE_COOLDOWN_TICKS);
    }

    private @Nullable Refusal checkCooldown(Player player, ItemStack stack) {
        int left = Math.max(canUseDuringEffectCooldown() ? 0 : getEffectCooldownLeft(player, stack),
                postRefineCooldownLeft(player, stack));
        if (left <= 0) return null;

        if (player instanceof ServerPlayer server) server.getCooldowns().addCooldown(this, left);
        long seconds = (left + Ticks.SECOND - 1) / Ticks.SECOND;
        return new Refusal(FAILED_COOLDOWN, Component.literal(String.valueOf(seconds)));
    }

    @Override
    protected void spend(ServerPlayer player, ItemStack stack, int count) {
        super.spend(player, stack, count);
        int left = Math.max(TimeFlow.shortenWait(player, spec.getItemCooldownTicks()),
                postRefineCooldownLeft(player, stack));
        if (!canUseDuringEffectCooldown()) left = Math.max(left, getEffectCooldownLeft(player, stack));
        if (left > 0) player.getCooldowns().addCooldown(this, left);
    }
    //endregion

    //region the clicks
    @Override
    protected boolean canFeedFromOffhand() { return true; }

    @Override
    protected final @Nullable Refusal checkGate(Player player, ItemStack stack) {
        if (isHoldingFood(player, stack)) return null;
        if (!isRefined(stack)) {
            return checkEssenceGate(player, Math.min(ESSENCE_FLOOR, getRefineCost()), FAILED_REFINE_ESSENCE);
        }
        Refusal poor = checkEssenceGate(player, useThreshold(stack), FAILED_ESSENCE);
        if (poor != null) return poor;

        if (spec.isChanneled() && clock.getEssenceAboveHungerFloor(stack) <= 0) return new Refusal(FAILED_STARVING);

        Refusal cooling = checkCooldown(player, stack);
        return cooling != null ? cooling : checkUseGate(player, stack);
    }

    protected long useThreshold(ItemStack stack) {
        return spec.isChanneled() ? ESSENCE_FLOOR : spec.getEssencePerRound();
    }

    @Override
    protected @Nullable Refusal checkUseGate(Player player, ItemStack stack) { return checkPayoutGate(player, stack); }

    @Override
    protected final int apply(ServerPlayer player, ItemStack stack) {
        if (!isRefined(stack)) return 0;
        if (isHoldingFood(player, stack) && !isCrouching(player)) {
            eat(player, stack);
            return 0;
        }
        return useApply(player, stack);
    }

    @Override
    protected int useApply(ServerPlayer player, ItemStack stack) {
        return spec.isChanneled() ? 0 : drive(player, stack);
    }

    @Override
    protected final int useDurationTicks(Player player, ItemStack stack) {
        if (!isRefined(stack)) return CHANNEL_MAX_TICKS;
        if (isHoldingFood(player, stack) && !isCrouching(player)) return 0;
        return spec.isChanneled() ? CHANNEL_MAX_TICKS : useChargeTicks(player, stack);
    }

    protected int useChargeTicks(Player player, ItemStack stack) { return useChargeByGap(player); }

    protected int drive(ServerPlayer player, ItemStack stack) {
        if (trySpendAndGrant(player, stack)) return 1;
        eat(player, stack);
        if (isHungry(player, stack)) clock.warn(player, stack, 1L);
        return 0;
    }

    protected final boolean trySpendAndGrant(ServerPlayer player, ItemStack stack) {
        ApertureEssenceService.consume(player, spec.getEssencePerRound());
        boolean drivenOnAnEmptyBar = clock.isSpendForced(stack, getHungerCostMultiplier(player, stack));
        grant(player, stack);
        if (!drivenOnAnEmptyBar || player.getAbilities().instabuild) return false;

        exhaust(player, stack);
        return true;
    }

    @Override
    protected boolean hasSneakUse(Player player, ItemStack stack) { return isHoldingFood(player, stack); }

    @Override
    protected @Nullable Refusal checkSneakGate(Player player, ItemStack stack) {
        Refusal poor = checkEssenceGate(player, useThreshold(stack), FAILED_ESSENCE);
        if (poor != null) return poor;

        Refusal cooling = checkCooldown(player, stack);
        return cooling != null ? cooling : checkPayoutGate(player, stack);
    }

    @Override
    protected int sneakApply(ServerPlayer player, ItemStack stack) {
        eat(player, stack);
        return spec.isChanneled() ? 0 : drive(player, stack);
    }

    protected boolean isHoldingFood(Player player, ItemStack stack) {
        return isRefined(stack) && feedUnits(player.getOffhandItem()) > 0;
    }

    protected boolean canUseDuringEffectCooldown() { return false; }

    protected int getHungerCostMultiplier(Player player, ItemStack stack) { return 1; }

    protected final int getEffectCooldownLeft(Player player, ItemStack stack) {
        return GuCooldowns.getCooldownLeft(player, ModDataComponents.get(stack, ModDataComponents.USED_AT),
                spec.getEffectCooldownTicks());
    }

    public final boolean tryAutoUse(ServerPlayer player, ItemStack stack) {
        if (!isRefined(stack) || spec.isChanneled() || player.getCooldowns().isOnCooldown(this)) return false;
        if (checkEssenceGate(player, useThreshold(stack), FAILED_ESSENCE) != null
                || checkCooldown(player, stack) != null
                || checkUseGate(player, stack) != null) return false;
        spend(player, stack, useApply(player, stack));
        return true;
    }

    protected void eat(ServerPlayer player, ItemStack stack) {
        ItemStack food = player.getOffhandItem();
        healFrom(player, stack, food);
        clock.tryEat(this, player, stack, food);
    }
    //endregion

    //region display
    @Override
    public Component chargeCaption(ItemStack stack, int remainingTicks) {
        if (!isRefined(stack)) return refineCaption(getState(stack).refineProgress());
        return spec.isChanneled()
                ? Component.translatable(CAPTION_CHANNELING, getState(stack).investedEssence(),
                spec.getEssencePerRound())
                : Component.translatable(CAPTION_USING_PLAIN);
    }

    @Override
    public @Nullable Float chargeFraction(ItemStack stack, int remainingTicks) {
        if (!isRefined(stack)) return refineFraction(stack);
        if (!spec.isChanneled()) return null;
        return getState(stack).investedEssence() / (float) spec.getEssencePerRound();
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return isRefined(stack) ? super.getName(stack) : ModDisplayText.formatWild(super.getName(stack));
    }

    @Override
    protected void appendTooltip(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                 @NotNull TooltipFlag flag) {
        super.appendTooltip(stack, level, tooltip, flag);
        if (getState(stack).damageTaken() <= 0) return;

        tooltip.add(Component.translatable(TOOLTIP_HEALTH, getHealth(stack), getMaxHealth())
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected @Nullable MutableComponent formatProgressLine(ItemStack stack) {
        if (!isRefined(stack)) {
            return Component.translatable(TOOLTIP_REFINE, getState(stack).refineProgress(), getRefineCost());
        }
        if (spec.isChanneled()) {
            return Component.translatable(TOOLTIP_INVESTED, getState(stack).investedEssence(),
                    spec.getEssencePerRound());
        }
        return clock instanceof GuClock.HungerBar bar
                ? Component.translatable(TOOLTIP_HUNGER, getState(stack).hunger(), bar.max())
                : null;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return isRefined(stack) ? clock.isBarVisible(stack) : getState(stack).refineProgress() > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) { return Math.round(getBarFraction(stack) * 13.0F); }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return Mth.hsvToRgb(getBarFraction(stack) / 3.0F, 1.0F, 1.0F);
    }

    private float getBarFraction(ItemStack stack) {
        return isRefined(stack) ? clock.getBarFraction(stack) : refineFraction(stack);
    }

    private float refineFraction(ItemStack stack) {
        return getRefineCost() <= 0 ? 0.0F : getState(stack).refineProgress() / (float) getRefineCost();
    }
    //endregion

    //region the day clock -- GuUpkeep walks the containers; a leaf may pay its own upkeep
    protected void payOwnUpkeep(ServerPlayer player, ItemStack stack) {}

    public boolean tryAutoFeed(ServerPlayer player, ItemStack stack) {
        if (!clock.isHungry(player, stack)) return false;

        Inventory inventory = player.getInventory();
        boolean ate = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (clock.tryEat(this, player, stack, inventory.getItem(slot))) ate = true;
        }
        return ate;
    }
    //endregion

    //region the three deaths -- one funnel, three messages; a Vital Gu starves or exhausts, never ruins
    public static void announce(ServerPlayer player, ItemStack stack, String key) {
        player.sendSystemMessage(Component.translatable(key, stack.getHoverName()));
    }

    public static void announceHungry(ServerPlayer player, ItemStack stack) { announce(player, stack, MSG_HUNGRY); }

    private static void announceDeath(ServerPlayer holder, ItemStack stack, String key) {
        announce(holder, stack, key);
        UUID uuid = getOwner(stack);
        if (uuid == null) return;

        ModEvents.post(new VitalGuLostEvent(holder.server, uuid, stack));
    }

    public static void starve(ServerPlayer holder, ItemStack stack) { announceDeath(holder, stack, MSG_STARVED); }

    public static void exhaust(ServerPlayer holder, ItemStack stack) { announceDeath(holder, stack, MSG_EXHAUSTED); }

    public static void ruin(ServerPlayer holder, ItemStack stack) { announceDeath(holder, stack, MSG_RUINED); }
    //endregion
}
