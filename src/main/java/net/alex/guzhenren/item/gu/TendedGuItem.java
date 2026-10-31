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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A tended Gu [需照顾]: wild, then refined [炼化], then fed and used, all on one shared state record. The
 * lifecycle host of every Gu that needs refining and feeding: its {@link GuModule}s answer {@code checkEffect} /
 * {@code applyEffect} and any limit they bend, or a leaf with a lifecycle of its own overrides them; it owns the
 * charge ladder, channeling [灌注] and billing. Hunger and the meals from the bag are {@link GuHunger}, health
 * {@link GuHealth}, cooldown stamps {@link GuCooldowns}, the day-rollover walk {@link GuUpkeep}.
 *
 * <p>Still past 300 lines on purpose: what is left is the vanilla {@code Item} overrides and the hooks its
 * modules answer, and channeling is this item's own {@code onUseTick}, reading and writing ten of its members --
 * pulled out, all of them would have to open up.
 *
 * <p>⚠ A refined Gu feeds itself from the holder's bag through one meal, {@link #feed} (every slot, offhand
 * included; it mends first, fills the bar only once whole, and runs only while the bar has room), at two moments:
 * before a use, and right after every drop, back to full. The drops are a use ({@link #drive}), a partner's bill
 * ({@link #billAsPartner}), each channel step, the day clock ({@link GuUpkeep}, carried or kept in an aperture,
 * which also eats before its drain) and the Little Light Gu's bright drain; nothing else eats, so a full bar never
 * eats just to mend. Food never routes a
 * click, so there is no feeding gesture; a channeled Gu at its hunger floor is refused only when the bag holds none
 * of its food ({@link GuHunger#carriesFood}). ⚠ Creative ignores the floor: the bar still drains to 0 for show, and
 * the Gu keeps channeling, as a forced use there never exhausts it.
 *
 * <p>⚠ {@code checkCooldown} is the one cooldown gate for the use, the tap use and {@link #tryAutoUse}:
 * the larger of the effect cooldown (waived by {@code canUseDuringEffectCooldown}) and the post-refine guard. A
 * refusal puts the ticks left back on the vanilla cooldown, so a lost sweep redraws. {@code spend} runs
 * {@code super.spend} first and only then puts the largest of the item cooldown, the post-refine guard and the
 * effect cooldown: {@code ItemCooldowns.addCooldown} overwrites unconditionally, so the other order would lose
 * the long cooldown to the base's short one.
 *
 * <p>A use on an empty hunger bar is forced ({@code trySpendAndApply}): the effect lands, then the Gu dies of
 * exhaustion, and the 1 that {@code drive} returns is that death, not a payout. {@link #billAsPartner} is the one
 * bill for a Gu that only backs another's use (the Amplify Effect Gu [功倍蛊], a killing move's [杀招] partner): its
 * essence, the meals around it, and on an empty bar the same death, after the use it backed.
 *
 * <p>⚠ Refining and channeling advance one step every {@code STEP_TICKS}, at two paces that stay separate. A
 * holder above the Gu's rank is Gu-paced: a channel step pours at most
 * {@code getEssencePerHungerPoint() * STEP_TICKS}, scaled by {@code TimeFlow}, and a refining step may pour the
 * whole pool. At the Gu's rank or below, the pool sets the pace: 1/5, 1/4, 1/3, then 1/2 of the pool per step,
 * 80% of it a second at normal time flow. Every essence this Gu pays goes out through {@code
 * PrimevalStoneSupply.pay}; a step is sized from the pool, so it first tops up to at least {@code ESSENCE_FLOOR}
 * ({@code topUpFor}), and a channel step eats before it sizes the essence, because the step is capped by the
 * essence above the hunger floor, which the meal raises.
 *
 * <p>⚠ {@link #canBeVital}: a Gu taken by its own use may never be bound, or the slot would lose it on
 * the very first click. Gu health is {@link GuHealth}, {@link #GU_MAX_HEALTH} unless the spec sets it.
 *
 * <p>{@code announceDeath} is the one death funnel ({@link #starve}, {@link #exhaust}, {@link #ruin}) and the
 * only poster of {@link VitalGuLostEvent}; a new way for a Gu to die goes through it.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuSpec
 * @see GuModule
 * @see GuHunger
 * @see RefinedGuState
 * @since 1.0.0
 */

public class TendedGuItem extends MortalGuItem {

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
    protected final GuHunger hunger;
    protected final GuHealth health;

    public TendedGuItem(Properties properties, GuSpec spec, GuModule... modules) {
        super(properties, spec, modules);
        this.hunger = spec.buildHunger();
        this.health = spec.buildHealth();
    }

    //region what the modules answer -- a leaf with a lifecycle of its own overrides them
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) { return checkModules(player, stack); }

    protected void applyEffect(ServerPlayer player, ItemStack stack) { applyModules(player, stack); }

    protected int feedUnits(ItemStack food) { return spec.feedUnits(food); }

    public boolean canBeVital() { return true; }
    //endregion

    //region state
    public static RefinedGuState getState(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.REFINED_GU_STATE, RefinedGuState.WILD);
    }

    void store(ItemStack stack, RefinedGuState state) {
        stack.set(ModDataComponents.REFINED_GU_STATE, new RefinedGuState(state.refined(),
                Math.min(state.refineProgress(), getRefineCost()),
                Math.min(state.investedEssence(), spec.getEssencePerRound()),
                state.hunger(),
                Mth.clamp(state.damageTaken(), 0, getMaxHealth())));
    }

    public boolean isRefined(ItemStack stack) { return getState(stack).refined(); }

    public boolean isHungry(ServerPlayer player, ItemStack stack) {
        return isRefined(stack) && hunger.isHungry(player, stack);
    }
    //endregion

    //region Gu health [蛊虫生命值] -- GuHealth holds the rules, stored as damage TAKEN
    public static final int GU_MAX_HEALTH = 12;

    public int getMaxHealth() { return health.max(); }

    public int getHealth(ItemStack stack) { return health.get(stack); }

    public boolean takeDamage(ServerPlayer holder, ItemStack stack, int amount) {
        return health.takeDamage(this, holder, stack, amount);
    }

    protected void heal(ItemStack stack, int amount) { health.heal(this, stack, amount); }
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
        hunger.bind(player, stack);
        stack.set(ModDataComponents.REFINED_AT,
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

        feed(player, stack);
        PrimevalStoneSupply.topUpFor(player, ESSENCE_FLOOR);
        int take = stepAmount(player, stack, elapsed);
        if (take <= 0 || !PrimevalStoneSupply.pay(player, take)) {
            player.stopUsingItem();
            return;
        }
        pour(player, stack, take);
        feed(player, stack);
    }

    private void refineTick(ServerPlayer player, ItemStack stack, int elapsed) {
        PrimevalStoneSupply.topUpFor(player, ESSENCE_FLOOR);
        int left = getRefineCost() - getState(stack).refineProgress();
        int take = (int) Math.min(left, getPoured(player, elapsed));
        if (take <= 0 || !PrimevalStoneSupply.pay(player, take)) {
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
                getEssenceAboveHungerFloor(player, stack));

        return (int) Math.min(thisStep, leftInThisRound);
    }

    private long getEssenceAboveHungerFloor(Player player, ItemStack stack) {
        return player.getAbilities().instabuild ? Long.MAX_VALUE : hunger.getEssenceAboveHungerFloor(stack);
    }

    private long getMostThisStepMaySpend(ServerPlayer player, long pool, int elapsed) {
        if (isGuPaced(player)) {
            return Math.min(pool, TimeFlow.scale(player, (long) hunger.getEssencePerHungerPoint() * STEP_TICKS));
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

        hunger.billHungerForEssence(stack, pouredBefore, pouredAfter);
        store(stack, getState(stack).withInvested(pouredAfter));

        if (pouredAfter >= spec.getEssencePerRound()) {
            applyEffectWithCooldown(player, stack);
            store(stack, getState(stack).withInvested(0));
            if (checkEffect(player, stack) != null) player.stopUsingItem();
        }
    }

    private void applyEffectWithCooldown(ServerPlayer player, ItemStack stack) {
        applyEffect(player, stack);
        if (spec.getEffectCooldownTicks() > 0) {
            stack.set(ModDataComponents.USED_AT,
                    GuCooldowns.createCooldownStamp(player, spec.getEffectCooldownTicks()));
        }
    }
    //endregion

    //region the long cooldown -- vanilla draws the sweep, the stack's stamp is the truth
    private static final String FAILED_COOLDOWN = "guzhenren.item.failed.gu_cooldown";

    private int postRefineCooldownLeft(Player player, ItemStack stack) {
        Long refinedAt = stack.get(ModDataComponents.REFINED_AT);
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
    protected boolean keepsRefusedClick() { return true; }

    @Override
    protected final @Nullable Refusal checkUse(Player player, ItemStack stack) {
        if (!isRefined(stack)) {
            return checkEssenceGate(player, Math.min(ESSENCE_FLOOR, getRefineCost()), FAILED_REFINE_ESSENCE);
        }
        Refusal poor = checkEssenceGate(player, useThreshold(stack), FAILED_ESSENCE);
        if (poor != null) return poor;

        if (spec.isChanneled() && getEssenceAboveHungerFloor(player, stack) <= 0 && !hunger.carriesFood(this, player)) {
            return new Refusal(FAILED_STARVING);
        }

        Refusal cooling = checkCooldown(player, stack);
        return cooling != null ? cooling : checkOwnUse(player, stack);
    }

    protected long useThreshold(ItemStack stack) {
        return spec.isChanneled() ? ESSENCE_FLOOR : spec.getEssencePerRound();
    }

    @Override
    protected @Nullable Refusal checkOwnUse(Player player, ItemStack stack) { return checkEffect(player, stack); }

    @Override
    protected final int performUse(ServerPlayer player, ItemStack stack) {
        return isRefined(stack) ? performOwnUse(player, stack) : 0;
    }

    @Override
    protected int performOwnUse(ServerPlayer player, ItemStack stack) {
        return spec.isChanneled() ? 0 : drive(player, stack);
    }

    @Override
    protected final int useDurationTicks(Player player, ItemStack stack) {
        if (!isRefined(stack)) return CHANNEL_MAX_TICKS;
        return spec.isChanneled() ? CHANNEL_MAX_TICKS : useChargeTicks(player, stack);
    }

    protected int useChargeTicks(Player player, ItemStack stack) { return useChargeByGap(player); }

    protected int drive(ServerPlayer player, ItemStack stack) {
        feed(player, stack);
        if (trySpendAndApply(player, stack)) return 1;
        feed(player, stack);
        if (isHungry(player, stack)) hunger.warn(player, stack, 1L);
        return 0;
    }

    protected final boolean trySpendAndApply(ServerPlayer player, ItemStack stack) {
        PrimevalStoneSupply.pay(player, spec.getEssencePerRound());
        boolean drivenOnAnEmptyBar = hunger.isSpendForced(stack, getHungerCostMultiplier(player, stack));
        applyEffectWithCooldown(player, stack);
        if (!drivenOnAnEmptyBar || player.getAbilities().instabuild) return false;

        exhaust(player, stack);
        return true;
    }

    /** Pays one use backing another Gu's effect, with none of its own: false only when pool and stones cannot. */
    public final boolean billAsPartner(ServerPlayer player, ItemStack stack) {
        if (!PrimevalStoneSupply.pay(player, spec.getEssencePerRound())) return false;

        feed(player, stack);
        if (hunger.isSpendForced(stack) && !player.getAbilities().instabuild) {
            exhaust(player, stack);
            stack.shrink(1);
            return true;
        }
        feed(player, stack);
        if (isHungry(player, stack)) hunger.warn(player, stack, 1L);
        return true;
    }

    @Override
    protected final @Nullable Refusal checkTapUse(Player player, ItemStack stack) {
        Refusal cooling = checkCooldown(player, stack);
        return cooling != null ? cooling : checkOwnTapUse(player, stack);
    }

    protected @Nullable Refusal checkOwnTapUse(Player player, ItemStack stack) { return null; }

    protected boolean canUseDuringEffectCooldown() {
        return getModules().stream().anyMatch(GuModule::canUseDuringEffectCooldown);
    }

    protected int getHungerCostMultiplier(Player player, ItemStack stack) {
        int multiplier = 1;
        for (GuModule module : getModules()) {
            multiplier = Math.max(multiplier, module.getHungerCostMultiplier(this, player, stack));
        }
        return multiplier;
    }

    public final boolean tryAutoUse(ServerPlayer player, ItemStack stack) {
        if (!isRefined(stack) || spec.isChanneled() || player.getCooldowns().isOnCooldown(this)) return false;
        if (checkInPlaceUse(player, stack) != null) return false;
        spend(player, stack, performOwnUse(player, stack));
        return true;
    }

    @Override
    protected @Nullable Refusal checkInPlaceUse(Player player, ItemStack stack) {
        Refusal poor = checkEssenceGate(player, useThreshold(stack), FAILED_ESSENCE);
        if (poor != null) return poor;

        Refusal cooling = checkCooldown(player, stack);
        return cooling != null ? cooling : checkOwnUse(player, stack);
    }

    @Override
    protected int performInPlaceUse(ServerPlayer player, ItemStack stack) {
        return isRefined(stack) && !spec.isChanneled() ? performOwnUse(player, stack) : 0;
    }

    protected void feed(ServerPlayer player, ItemStack stack) { hunger.feed(this, player, stack); }

    /** Takes {@code points} off the hunger bar for a cost that is no use (a killing move's start); false when short. */
    public final boolean takeHunger(ItemStack stack, int points) {
        RefinedGuState state = getState(stack);
        if (state.hunger() < points) return false;
        store(stack, state.withHunger(state.hunger() - points));
        return true;
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
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
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
        if (hunger instanceof GuHunger.HungerBar bar) {
            return Component.translatable(TOOLTIP_HUNGER, getState(stack).hunger(), bar.max());
        }
        if (hunger instanceof GuHunger.RecoverBar bar) {
            return Component.translatable(TOOLTIP_HUNGER, getState(stack).hunger(), bar.max());
        }
        return null;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return isRefined(stack) ? hunger.isBarVisible(stack) : getState(stack).refineProgress() > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) { return Math.round(getBarFraction(stack) * 13.0F); }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return Mth.hsvToRgb(getBarFraction(stack) / 3.0F, 1.0F, 1.0F);
    }

    private float getBarFraction(ItemStack stack) {
        return isRefined(stack) ? hunger.getBarFraction(stack) : refineFraction(stack);
    }

    private float refineFraction(ItemStack stack) {
        return getRefineCost() <= 0 ? 0.0F : getState(stack).refineProgress() / (float) getRefineCost();
    }
    //endregion

    //region the day clock -- GuUpkeep walks the containers; a leaf may pay its own upkeep
    protected void payOwnUpkeep(ServerPlayer player, ItemStack stack) {}
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
