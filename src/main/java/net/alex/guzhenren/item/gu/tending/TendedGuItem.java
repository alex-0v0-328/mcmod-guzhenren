package net.alex.guzhenren.item.gu.tending;

import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.alex.guzhenren.item.gu.stack.GuCooldowns;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
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
 * A tended Gu [需照顾]: wild, then refined [炼化], then fed and used, all on one shared state record, so the
 * host stacks it to one ({@code Item.Properties} is the host's, never the registration's). The
 * lifecycle host of every Gu that needs refining and feeding: its {@link GuBehavior}s answer {@code checkEffect} /
 * {@code applyEffect} and any limit they bend, or a leaf with a lifecycle of its own overrides them; it owns the
 * charge ladder, channeling [灌注] and billing. Hunger and the meals from the bag are {@link GuHunger}, health
 * {@link GuHealth}, cooldown stamps {@link GuCooldowns}, the day-rollover walk {@link GuUpkeep}, the channeling
 * quota {@link GuChanneling}, the long cooldown {@link GuRefineCooldown}.
 *
 * <p>Still past 300 lines on purpose: what is left is the vanilla {@code Item} overrides and the hooks its
 * behaviors answer, and channeling is this item's own {@code onUseTick}, which {@code GuChanneling} serves with the
 * numbers and the pour but cannot own: it drives the meal, the payment and the effect.
 *
 * <p>⚠ A refined Gu feeds itself from the holder's bag through one meal, {@link #feed} (every slot, offhand
 * included; it mends first, fills the bar only once whole, and runs only while the bar has room), at two moments:
 * before a use, and right after every drop, back to full. The drops are a use ({@link #drive}), a partner's bill
 * ({@link #billAsPartner}), each channel step, the day clock ({@link GuUpkeep}, carried or kept in an aperture,
 * which also eats before its drain) and the Little Light Gu's switch-on; nothing else eats, so a full bar never
 * eats just to mend. Food never routes a
 * click, so there is no feeding gesture; a channeled Gu at its hunger floor is refused only when the bag holds none
 * of its food ({@link GuHunger#carriesFood}). ⚠ Creative ignores the floor: the bar still drains to 0 for show, and
 * the Gu keeps channeling, as a forced use there never exhausts it.
 *
 * <p>⚠ A use short of hunger after the bag's meal ({@code GuHunger.isShortOfUse}, the hunger multiplier included)
 * is forced only on confirmation ({@link GuForcedUse}): the first {@code drive} warns in red and pays nothing, and
 * answers {@code WARNED}, on which {@code spend} puts no cooldown; the same Gu used again within five seconds is
 * forced ({@code trySpendAndApply}): the effect lands, then the Gu dies of exhaustion, and the 1 that {@code drive}
 * returns is that death, not a payout; a behavior may defer that death to the end of its round
 * ({@code GuBehavior.defersForcedDeath}). A Vital Gu is only refused, never forced; creative is never warned and never
 * dies; {@link #tryAutoUse} skips a short use in silence. {@link #billAsPartner} is the one bill for a Gu that only
 * backs another's use (the Amplify Effect Gu [功倍蛊], a killing move's [杀招] partner): its essence, the meals around
 * it, and on an empty bar the same death, after the use it backed.
 *
 * <p>⚠ Refining and channeling advance one step every {@code GuChanneling.STEP_TICKS}, at the two paces {@link
 * GuChanneling} keeps apart. Every essence this Gu pays goes out through {@code PrimevalStoneSupply.pay}; a step is
 * sized from the pool, so it first tops up to at least {@code GuChanneling.ESSENCE_FLOOR} ({@code topUpFor}), and a
 * channel step eats before it sizes the essence, because the step is capped by the essence above the hunger floor,
 * which the meal raises.
 *
 * <p>⚠ {@link #canBeVital}: a Gu taken by its own use may never be bound, or the slot would lose it on
 * the very first click. Gu health is {@link GuHealth}, 4 x rank + 4 from the properties.
 *
 * <p>{@code announceDeath} is the one death funnel ({@link #starve}, {@link #exhaust}, {@link #ruin}) and the
 * only poster of {@link VitalGuLostEvent}; a new way for a Gu to die goes through it.
 *
 * <p>Refining is paid in instalments; what it buys is a lasting bond.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuProperties
 * @see GuBehavior
 * @see GuHunger
 * @see GuChanneling
 * @see GuRefineCooldown
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
    private static final String FAILED_SHORT = "guzhenren.item.failed.gu_short";
    private static final String FAILED_VITAL_SHORT = "guzhenren.item.failed.vital_gu_short";
    private static final String MSG_HUNGRY = "guzhenren.item.gu.hungry";
    private static final String MSG_STARVED = "guzhenren.item.gu.starved";
    private static final String MSG_EXHAUSTED = "guzhenren.item.gu.exhausted";
    private static final String MSG_RUINED = "guzhenren.item.gu.ruined";
    protected static final int WARNED = -1;
    protected final GuHunger hunger;
    protected final GuHealth health;
    private final GuChanneling channeling;
    private final GuRefineCooldown refineCooldown;

    public TendedGuItem(GuProperties guProperties, GuBehavior... behaviors) {
        super(new Properties().stacksTo(1), guProperties, behaviors);
        this.hunger = guProperties.buildHunger();
        this.health = guProperties.buildHealth();
        this.channeling = new GuChanneling(guProperties, hunger);
        this.refineCooldown = new GuRefineCooldown(guProperties);
    }

    //region what the behaviors answer [行为应答]
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) { return checkModules(player, stack); }

    protected void applyEffect(ServerPlayer player, ItemStack stack) { applyModules(player, stack); }

    protected int feedUnits(ItemStack food) { return guProperties.feedUnits(food); }

    public boolean canBeVital() { return true; }
    //endregion

    //region state [状态]
    public static RefinedGuState getState(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.REFINED_GU_STATE, RefinedGuState.WILD);
    }

    void store(ItemStack stack, RefinedGuState state) {
        stack.set(ModDataComponents.REFINED_GU_STATE, new RefinedGuState(state.refined(),
                Math.min(state.refineProgress(), getRefineCost()),
                Math.min(state.investedEssence(), guProperties.getEssencePerRound()),
                state.hunger(),
                Mth.clamp(state.damageTaken(), 0, getMaxHealth())));
    }

    public boolean isRefined(ItemStack stack) { return getState(stack).refined(); }

    public boolean isHungry(ServerPlayer player, ItemStack stack) {
        return isRefined(stack) && hunger.isHungry(stack);
    }
    //endregion

    //region Gu health [蛊虫生命值]
    public int getMaxHealth() { return health.max(); }

    public int getHealth(ItemStack stack) { return health.get(stack); }

    public boolean takeDamage(ServerPlayer holder, ItemStack stack, int amount) {
        return health.takeDamage(this, holder, stack, amount);
    }

    protected void heal(ItemStack stack, int amount) { health.heal(this, stack, amount); }
    //endregion

    //region refining [炼化]
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

    //region channeling [灌注]
    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack,
                          int remaining) {
        if (!(entity instanceof ServerPlayer player)) return;

        int elapsed = GuChanneling.CHANNEL_MAX_TICKS - remaining;
        if (elapsed < 0 || elapsed % GuChanneling.STEP_TICKS != 0) return;

        if (!isRefined(stack)) {
            refineTick(player, stack, elapsed);
            return;
        }
        if (!guProperties.isChanneled()) return;

        feed(player, stack);
        PrimevalStoneSupply.topUpFor(player, GuChanneling.ESSENCE_FLOOR);
        int take = channeling.stepAmount(player, stack, elapsed, isGuPaced(player));
        if (take <= 0 || !PrimevalStoneSupply.pay(player, take)) {
            player.stopUsingItem();
            return;
        }
        channeling.pour(this, player, stack, take);
        feed(player, stack);
    }

    private void refineTick(ServerPlayer player, ItemStack stack, int elapsed) {
        PrimevalStoneSupply.topUpFor(player, GuChanneling.ESSENCE_FLOOR);
        int left = getRefineCost() - getState(stack).refineProgress();
        int take = (int) Math.min(left, channeling.getPoured(player, elapsed, isGuPaced(player)));
        if (take <= 0 || !PrimevalStoneSupply.pay(player, take)) {
            player.stopUsingItem();
            return;
        }
        refineStep(player, stack, take);
        if (isRefined(stack)) player.stopUsingItem();
    }

    private boolean isGuPaced(Player player) { return getRankGap(player) > 0; }

    void applyEffectWithCooldown(ServerPlayer player, ItemStack stack) {
        applyEffect(player, stack);
        if (guProperties.getEffectCooldownTicks() > 0) {
            stack.set(ModDataComponents.USED_AT,
                    GuCooldowns.createCooldownStamp(player, guProperties.getEffectCooldownTicks()));
        }
    }
    //endregion

    //region the long cooldown [长冷却]
    @Override
    protected void spend(ServerPlayer player, ItemStack stack, int count) {
        if (count == WARNED) return;

        super.spend(player, stack, count);
        refineCooldown.put(this, player, stack);
    }
    //endregion

    //region the clicks [点击]
    @Override
    protected boolean keepsRefusedClick() { return true; }

    @Override
    protected final @Nullable Refusal checkUse(Player player, ItemStack stack) {
        if (!isRefined(stack)) {
            return checkEssenceGate(player, Math.min(GuChanneling.ESSENCE_FLOOR, getRefineCost()),
                    FAILED_REFINE_ESSENCE);
        }
        Refusal poor = checkEssenceGate(player, useThreshold(stack), FAILED_ESSENCE);
        if (poor != null) return poor;

        if (guProperties.isChanneled() && channeling.getEssenceAboveHungerFloor(player, stack) <= 0
                && !hunger.carriesFood(this, player)) {
            return new Refusal(FAILED_STARVING);
        }
        return checkCooldownThenOwnUse(player, stack);
    }

    private @Nullable Refusal checkCooldownThenOwnUse(Player player, ItemStack stack) {
        Refusal cooling = refineCooldown.check(this, player, stack);
        return cooling != null ? cooling : checkOwnUse(player, stack);
    }

    protected long useThreshold(ItemStack stack) {
        return guProperties.isChanneled() ? GuChanneling.ESSENCE_FLOOR : guProperties.getEssencePerRound();
    }

    @Override
    protected @Nullable Refusal checkOwnUse(Player player, ItemStack stack) { return checkEffect(player, stack); }

    @Override
    protected final int performUse(ServerPlayer player, ItemStack stack) {
        return isRefined(stack) ? performOwnUse(player, stack) : 0;
    }

    @Override
    protected int performOwnUse(ServerPlayer player, ItemStack stack) {
        return guProperties.isChanneled() ? 0 : drive(player, stack);
    }

    @Override
    protected final int useDurationTicks(Player player, ItemStack stack) {
        if (!isRefined(stack)) return GuChanneling.CHANNEL_MAX_TICKS;
        return guProperties.isChanneled() ? GuChanneling.CHANNEL_MAX_TICKS : useChargeTicks(player, stack);
    }

    protected int useChargeTicks(Player player, ItemStack stack) { return useChargeByGap(player); }

    protected int drive(ServerPlayer player, ItemStack stack) {
        feed(player, stack);
        if (stopsShortUse(player, stack)) return WARNED;
        if (trySpendAndApply(player, stack)) return 1;
        feedAndWarn(player, stack);
        return 0;
    }

    /** True when a use short of hunger stops here: warned, or refused outright for a Vital Gu. */
    protected final boolean stopsShortUse(ServerPlayer player, ItemStack stack) {
        return isShortOfUse(player, stack) && !confirmForcedUse(player, stack);
    }

    protected final boolean isShortOfUse(ServerPlayer player, ItemStack stack) {
        return !player.getAbilities().instabuild
                && hunger.isShortOfUse(stack, getHungerCostMultiplier(player, stack));
    }

    private boolean confirmForcedUse(ServerPlayer player, ItemStack stack) {
        if (VitalGuState.isVital(stack)) {
            ActionBar.refuse(player, FAILED_VITAL_SHORT);
            return false;
        }
        if (GuForcedUse.confirm(player, stack)) return true;

        ActionBar.refuse(player, FAILED_SHORT);
        return false;
    }

    protected final boolean trySpendAndApply(ServerPlayer player, ItemStack stack) {
        PrimevalStoneSupply.pay(player, guProperties.getEssencePerRound());
        boolean drivenOnAnEmptyBar = hunger.isSpendForced(stack, getHungerCostMultiplier(player, stack));
        applyEffectWithCooldown(player, stack);
        if (!drivenOnAnEmptyBar || player.getAbilities().instabuild || defersForcedDeath(player, stack)) return false;

        exhaust(player, stack);
        return true;
    }

    private boolean defersForcedDeath(ServerPlayer player, ItemStack stack) {
        return getModules().stream().anyMatch(behavior -> behavior.defersForcedDeath(this, player, stack));
    }

    /** Pays one use backing another Gu's effect, with none of its own: false only when pool and stones cannot. */
    public final boolean billAsPartner(ServerPlayer player, ItemStack stack) {
        if (!PrimevalStoneSupply.pay(player, guProperties.getEssencePerRound())) return false;

        feed(player, stack);
        if (hunger.isSpendForcedAsPartner(stack) && !player.getAbilities().instabuild) {
            exhaust(player, stack);
            stack.shrink(1);
            return true;
        }
        feedAndWarn(player, stack);
        return true;
    }

    @Override
    protected final @Nullable Refusal checkTapUse(Player player, ItemStack stack) {
        Refusal cooling = refineCooldown.check(this, player, stack);
        return cooling != null ? cooling : checkOwnTapUse(player, stack);
    }

    protected @Nullable Refusal checkOwnTapUse(Player player, ItemStack stack) { return null; }

    protected boolean canUseDuringEffectCooldown() {
        return getModules().stream().anyMatch(GuBehavior::canUseDuringEffectCooldown);
    }

    protected int getHungerCostMultiplier(Player player, ItemStack stack) {
        int multiplier = 1;
        for (GuBehavior behavior : getModules()) {
            multiplier = Math.max(multiplier, behavior.getHungerCostMultiplier(this, player, stack));
        }
        return multiplier;
    }

    public final boolean tryAutoUse(ServerPlayer player, ItemStack stack) {
        if (!isRefined(stack) || guProperties.isChanneled() || player.getCooldowns().isOnCooldown(this)) return false;
        if (checkInPlaceUse(player, stack) != null) return false;

        feed(player, stack);
        if (isShortOfUse(player, stack)) return false;
        spend(player, stack, performOwnUse(player, stack));
        return true;
    }

    @Override
    protected @Nullable Refusal checkInPlaceUse(Player player, ItemStack stack) {
        Refusal poor = checkEssenceGate(player, useThreshold(stack), FAILED_ESSENCE);
        return poor != null ? poor : checkCooldownThenOwnUse(player, stack);
    }

    @Override
    protected int performInPlaceUse(ServerPlayer player, ItemStack stack) {
        return isRefined(stack) && !guProperties.isChanneled() ? performOwnUse(player, stack) : 0;
    }

    protected void feed(ServerPlayer player, ItemStack stack) { hunger.feed(this, player, stack); }

    /** The meal right after a drop, then the hungry warning when the next day rollover would kill it. */
    protected final void feedAndWarn(ServerPlayer player, ItemStack stack) {
        feed(player, stack);
        if (isHungry(player, stack)) announce(player, stack, MSG_HUNGRY);
    }

    /** Takes {@code points} off the hunger bar for a cost that is no use (a killing move's start); false when short. */
    public final boolean takeHunger(ItemStack stack, int points) {
        RefinedGuState state = getState(stack);
        if (state.hunger() < points) return false;
        store(stack, state.withHunger(state.hunger() - points));
        return true;
    }

    //endregion

    //region display [显示]
    @Override
    public Component chargeCaption(ItemStack stack, int remainingTicks) {
        if (!isRefined(stack)) return refineCaption(getState(stack).refineProgress());
        return guProperties.isChanneled()
                ? Component.translatable(CAPTION_CHANNELING, getState(stack).investedEssence(),
                guProperties.getEssencePerRound())
                : Component.translatable(CAPTION_USING_PLAIN);
    }

    @Override
    public @Nullable Float chargeFraction(ItemStack stack, int remainingTicks) {
        if (!isRefined(stack)) return refineFraction(stack);
        if (!guProperties.isChanneled()) return null;
        return getState(stack).investedEssence() / (float) guProperties.getEssencePerRound();
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
        if (guProperties.isChanneled()) {
            return Component.translatable(TOOLTIP_INVESTED, getState(stack).investedEssence(),
                    guProperties.getEssencePerRound());
        }
        return switch (hunger) {
            case GuHunger.HungerBar bar -> Component.translatable(TOOLTIP_HUNGER, getState(stack).hunger(), bar.max());
            case GuHunger.RecoverBar bar -> Component.translatable(TOOLTIP_HUNGER, getState(stack).hunger(), bar.max());
            case GuHunger.NoClock ignored -> null;
        };
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

    //region the day clock [日钟]
    protected void payOwnUpkeep(ServerPlayer player, ItemStack stack) {}
    //endregion

    //region the three deaths [三种死亡]
    private static void announce(ServerPlayer player, ItemStack stack, String key) {
        player.sendSystemMessage(Component.translatable(key, stack.getHoverName()));
    }

    private static void announceDeath(ServerPlayer holder, ItemStack stack, String key) {
        announce(holder, stack, key);
        UUID uuid = VitalGuState.getOwner(stack);
        if (uuid == null) return;

        ModEvents.post(new VitalGuLostEvent(holder.server, uuid, stack));
    }

    public static void starve(ServerPlayer holder, ItemStack stack) { announceDeath(holder, stack, MSG_STARVED); }

    public static void exhaust(ServerPlayer holder, ItemStack stack) { announceDeath(holder, stack, MSG_EXHAUSTED); }

    public static void ruin(ServerPlayer holder, ItemStack stack) { announceDeath(holder, stack, MSG_RUINED); }
    //endregion
}
