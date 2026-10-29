package net.alex.guzhenren.item.gu;

import com.google.common.math.LongMath;
import java.util.Map;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.DaoMarks;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One Gu's numbers, given as a builder chain at registration so that a registration reads as a table row.
 *
 * <p>The single source of truth for every Gu's numerical identity: refine cost, channel/cost-per-use,
 * hunger bar geometry (or a {@link #recoverBar} that refills with the world clock and never eats), feed tags,
 * health, cooldown, the usage type [类型] ({@link GuType}, absent for a Gu that has none), and the path effect
 * [流派效果] it lays down: the carrier it works on and the Dao marks [道痕] it carries on the Gu's path.
 * Stored on {@link MortalGuItem} at construction; read by {@link TendedGuItem}, {@link GuClock}, and the tooltip/HUD
 * renderers.
 *
 * <p>{@link #effect(EffectCarrier)} takes the rank's default strands [丝] ({@link #getDefaultStrands}: 12 at rank one,
 * four times as many each rank); {@link #effect(EffectCarrier, long)} names them, and
 * {@link #effect(EffectCarrier, DaoMarks)} names both counters. A Gu without the call lays down no effect and
 * carries no marks -- an instant Gu, or one that only changes a number. {@link #createEffect} builds the effect a
 * family puts on with {@code PathEffectService.apply}.
 *
 * <p>⚠ {@link #validate} runs at registration and refuses to start the game, naming the Gu, when
 * the channel numbers do not divide evenly, or a food worth more than one point does not divide the bar, a use,
 * a day and the health, or an effect carries no marks, or a mortal Gu's effect carries whole marks [条]: a mortal
 * Gu lays down strands only. It is the first line of defense; never route around it.
 *
 * @author Alex
 * @version 1.0.0
 * @see MortalGuItem
 * @see GuClock
 * @since 1.0.0
 */

public final class GuSpec {

    private final Rank rank;
    private final GuPath path;
    private StrengthPathBranch strengthPathBranch = StrengthPathBranch.NORMAL;
    private @Nullable GuType type;
    private int refineCost;
    private int essencePerRound;
    private boolean channels;
    private int maxHunger;
    private int recoverTicks;
    private int unitsPerHunger = 1;
    private int essencePerHunger;
    private int hungerPerUse = 1;
    private int hungerPerDay = 1;
    private int maxHealth = TendedGuItem.GU_MAX_HEALTH;
    private @Nullable TagKey<Item> feedTag;
    private int feedUnits;
    private int effectCooldownTicks;
    private int itemCooldownTicks;
    private static final long DEFAULT_STRANDS = 12L;
    private static final long STRANDS_PER_RANK = 4L;
    private @Nullable EffectCarrier effectCarrier;
    private DaoMarks effectMarks = DaoMarks.NONE;

    private GuSpec(Rank rank, GuPath path) {
        this.rank = rank;
        this.path = path;
    }

    public static GuSpec of(Rank rank, GuPath path) { return new GuSpec(rank, path); }

    //region the chain -- one call per fact, so a registration reads as a table row
    public GuSpec refine(int cost) {
        this.refineCost = cost;
        return this;
    }

    public GuSpec channel(int essencePerRound) {
        this.essencePerRound = essencePerRound;
        this.channels = true;
        return this;
    }

    public GuSpec costPerUse(int essence) {
        this.essencePerRound = essence;
        this.channels = false;
        return this;
    }

    public GuSpec hungerBar(int max, int unitsPerHunger) {
        this.maxHunger = max;
        this.unitsPerHunger = unitsPerHunger;
        return this;
    }

    public GuSpec recoverBar(int max, int ticksPerPoint) {
        this.maxHunger = max;
        this.recoverTicks = ticksPerPoint;
        return this;
    }

    public GuSpec essencePerHunger(int essence) {
        this.essencePerHunger = essence;
        return this;
    }

    public GuSpec hungerPerUse(int points) {
        this.hungerPerUse = points;
        return this;
    }

    public GuSpec hungerPerDay(int points) {
        this.hungerPerDay = points;
        return this;
    }

    public GuSpec health(int max) {
        this.maxHealth = max;
        return this;
    }

    public GuSpec feed(TagKey<Item> tag, int units) {
        this.feedTag = tag;
        this.feedUnits = units;
        return this;
    }

    public GuSpec cooldown(int ticks) {
        this.effectCooldownTicks = ticks;
        this.itemCooldownTicks = ticks;
        return this;
    }

    public GuSpec cooldown(int effectTicks, int itemTicks) {
        this.effectCooldownTicks = effectTicks;
        this.itemCooldownTicks = itemTicks;
        return this;
    }

    public GuSpec strengthPathBranch(StrengthPathBranch branch) {
        this.strengthPathBranch = branch;
        return this;
    }

    public GuSpec type(GuType type) {
        this.type = type;
        return this;
    }

    public GuSpec effect(EffectCarrier carrier) { return effect(carrier, getDefaultStrands(rank)); }

    public GuSpec effect(EffectCarrier carrier, long strands) { return effect(carrier, DaoMarks.ofStrands(strands)); }

    public GuSpec effect(EffectCarrier carrier, DaoMarks marks) {
        this.effectCarrier = carrier;
        this.effectMarks = marks;
        return this;
    }
    //endregion

    //region what the base classes read
    public Rank getRank() { return rank; }

    public GuPath getPath() { return path; }

    public StrengthPathBranch getStrengthPathBranch() { return strengthPathBranch; }

    public @Nullable GuType getType() { return type; }

    public int getRefineCost() { return refineCost; }

    public int getEssencePerRound() { return essencePerRound; }

    public boolean isChanneled() { return channels; }

    public int getEffectCooldownTicks() { return effectCooldownTicks; }

    public int getItemCooldownTicks() { return itemCooldownTicks; }

    public int getUnitsPerHunger() { return unitsPerHunger; }

    public int getFeedUnits() { return feedUnits; }

    public int getMaxHealth() { return maxHealth; }

    public @Nullable EffectCarrier getEffectCarrier() { return effectCarrier; }

    public DaoMarks getEffectMarks() { return effectMarks; }

    public Map<GuPath, DaoMarks> getEffectPaths() {
        return effectCarrier == null ? Map.of() : Map.of(path, effectMarks);
    }

    public static long getDefaultStrands(Rank rank) {
        int steps = Math.max(0, rank.ordinal() - Rank.LOWEST.ordinal());
        return LongMath.saturatedMultiply(DEFAULT_STRANDS, LongMath.saturatedPow(STRANDS_PER_RANK, steps));
    }

    public <P> PathEffect<P> createEffect(PathEffectType<P> effectType, ResourceLocation source, long endTick,
                                          P payload) {
        if (effectCarrier == null) throw new IllegalStateException("Gu spec declares no effect for " + source);
        return PathEffect.of(effectType, effectCarrier, source, getEffectPaths(), endTick, payload);
    }

    public GuClock buildClock() {
        if (recoverTicks > 0) return new GuClock.RecoverBar(maxHunger, recoverTicks, hungerPerUse);
        if (maxHunger > 0) {
            return new GuClock.HungerBar(maxHunger, unitsPerHunger, essencePerHunger, hungerPerUse, hungerPerDay);
        }
        return new GuClock.NoClock();
    }

    public int feedUnits(ItemStack food) {
        return feedTag != null && food.is(feedTag) ? feedUnits : 0;
    }
    //endregion

    //region the startup check -- a typo here should never reach the game
    public void validate(String id) {
        if (refineCost < 0) throw fault(id, "refine cost is negative");
        if (essencePerRound < 0) throw fault(id, "essence per round is negative");
        if (maxHealth <= 0) throw fault(id, "has no health");
        if (hungerPerDay <= 0) throw fault(id, "drains no hunger a day");
        if (recoverTicks > 0 && maxHunger <= 0) throw fault(id, "recovers with time but has no bar");
        if (recoverTicks > 0 && (channels || feedTag != null)) {
            throw fault(id, "recovers with time but channels or feeds");
        }
        if (feedTag != null && feedUnits > unitsPerHunger) checkWholeMeals(id);
        if (effectCarrier != null) checkEffect(id);

        if (!channels) return;

        if (essencePerRound == 0) throw fault(id, "channels but pours nothing");
        if (essencePerHunger <= 0) throw fault(id, "channels but costs no hunger");
        if (essencePerRound % essencePerHunger != 0) {
            throw fault(id, "round %d does not divide by hunger rate %d"
                    .formatted(essencePerRound, essencePerHunger));
        }
    }

    private void checkEffect(String id) {
        if (effectMarks.isEmpty()) throw fault(id, "lays down an effect with no marks");
        if (rank.ordinal() <= Rank.HIGHEST.ordinal() && effectMarks.marks() > 0L) {
            throw fault(id, "is mortal but declares %d whole marks; a mortal Gu carries strands only"
                    .formatted(effectMarks.marks()));
        }
    }

    // GuMeal feeds whole items and never past the need, so a gap smaller than one food would stay unfillable.
    private void checkWholeMeals(String id) {
        if (feedUnits % unitsPerHunger != 0) {
            throw fault(id, "food of %d units is not whole points at rate %d".formatted(feedUnits, unitsPerHunger));
        }
        int food = feedUnits / unitsPerHunger;
        int[] sizes = {maxHunger, hungerPerUse, hungerPerDay, maxHealth};
        String[] names = {"bar", "use", "day", "health"};
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] % food != 0) {
                throw fault(id, "%s %d does not divide by a food of %d".formatted(names[i], sizes[i], food));
            }
        }
    }

    private static IllegalStateException fault(String id, String what) {
        return new IllegalStateException("Gu spec for '%s' %s".formatted(id, what));
    }
    //endregion
}
