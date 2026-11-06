package net.alex.guzhenren.item.gu;

import com.google.common.math.LongMath;
import java.util.Map;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.gu.tending.GuHealth;
import net.alex.guzhenren.item.gu.tending.GuHunger;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
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
 * cooldown, the usage type [类型] ({@link GuType}, absent for a Gu that has none), and the path effect
 * [流派效果] it lays down: the carrier it works on and the path marks [道痕] it carries on the Gu's path.
 * Stored on {@link MortalGuItem} at construction; read by {@link TendedGuItem} and the tooltip/HUD renderers, and
 * it builds the Gu's {@link GuHunger} and {@link GuHealth}. What only one kind of Gu needs (a duration, a grade)
 * stays on its {@link GuBehavior}. Gu health is no chain call: it is 4 x rank + 4 (8 to 24 at ranks I to V), for
 * every Gu alike.
 *
 * <p>{@link #effect(EffectCarrier)} takes the rank's default strands [丝] ({@link #getDefaultStrands}: 12 at rank one,
 * four times as many each rank); {@link #effect(EffectCarrier, long)} names them, and
 * {@link #effect(EffectCarrier, PathMarks)} names both counters. A Gu without the call lays down no effect and
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
 * @see GuHunger
 * @since 1.0.0
 */

public final class GuProperties {

    private final Rank rank;
    private final GuPath path;
    private final int maxHealth;
    private StrengthPathBranch strengthPathBranch = StrengthPathBranch.NORMAL;
    private @Nullable GuType type;
    private int refineCost;
    private int essencePerRound;
    private boolean channels;
    private int maxHunger;
    private int recoverTicks;
    private int unitsPerHunger = 1;
    private int hungerPerRound;
    private int hungerPerUse = 1;
    private int hungerPerDay = 1;
    private @Nullable TagKey<Item> feedTag;
    private int feedUnits;
    private int effectCooldownTicks;
    private int itemCooldownTicks;
    private static final long DEFAULT_STRANDS = 12L;
    private static final long STRANDS_PER_RANK = 4L;
    private @Nullable EffectCarrier effectCarrier;
    private PathMarks effectMarks = PathMarks.NONE;

    private GuProperties(Rank rank, GuPath path) {
        this.rank = rank;
        this.path = path;
        this.maxHealth = 4 * rank.ordinal() + 4;
    }

    public static GuProperties of(Rank rank, GuPath path) { return new GuProperties(rank, path); }

    //region the chain [链]
    public GuProperties refine(int cost) {
        this.refineCost = cost;
        return this;
    }

    public GuProperties channel(int essencePerRound) {
        this.essencePerRound = essencePerRound;
        this.channels = true;
        return this;
    }

    public GuProperties costPerUse(int essence) {
        this.essencePerRound = essence;
        this.channels = false;
        return this;
    }

    public GuProperties hungerBar(int max, int unitsPerHunger) {
        this.maxHunger = max;
        this.unitsPerHunger = unitsPerHunger;
        return this;
    }

    public GuProperties recoverBar(int max, int ticksPerPoint) {
        this.maxHunger = max;
        this.recoverTicks = ticksPerPoint;
        return this;
    }

    public GuProperties hungerPerRound(int points) {
        this.hungerPerRound = points;
        return this;
    }

    public GuProperties hungerPerUse(int points) {
        this.hungerPerUse = points;
        return this;
    }

    public GuProperties hungerPerDay(int points) {
        this.hungerPerDay = points;
        return this;
    }

    public GuProperties feed(TagKey<Item> tag, int units) {
        this.feedTag = tag;
        this.feedUnits = units;
        return this;
    }

    public GuProperties cooldown(int ticks) {
        this.effectCooldownTicks = ticks;
        this.itemCooldownTicks = ticks;
        return this;
    }

    public GuProperties cooldown(int effectTicks, int itemTicks) {
        this.effectCooldownTicks = effectTicks;
        this.itemCooldownTicks = itemTicks;
        return this;
    }

    public GuProperties strengthPathBranch(StrengthPathBranch branch) {
        this.strengthPathBranch = branch;
        return this;
    }

    public GuProperties type(GuType type) {
        this.type = type;
        return this;
    }

    public GuProperties effect(EffectCarrier carrier) { return effect(carrier, getDefaultStrands(rank)); }

    public GuProperties effect(EffectCarrier carrier, long strands) { return effect(carrier, PathMarks.ofStrands(strands)); }

    public GuProperties effect(EffectCarrier carrier, PathMarks marks) {
        this.effectCarrier = carrier;
        this.effectMarks = marks;
        return this;
    }
    //endregion

    //region what the base classes read [基类读取]
    public Rank getRank() { return rank; }

    public GuPath getPath() { return path; }

    public StrengthPathBranch getStrengthPathBranch() { return strengthPathBranch; }

    public @Nullable GuType getType() { return type; }

    public int getRefineCost() { return refineCost; }

    public int getEssencePerRound() { return essencePerRound; }

    public boolean isChanneled() { return channels; }

    public int getEffectCooldownTicks() { return effectCooldownTicks; }

    public int getItemCooldownTicks() { return itemCooldownTicks; }

    public int getMaxHunger() { return maxHunger; }

    public int getUnitsPerHunger() { return unitsPerHunger; }

    public int getHungerPerUse() { return hungerPerUse; }

    public int getHungerPerRound() { return hungerPerRound; }

    public int getFeedUnits() { return feedUnits; }

    public @Nullable EffectCarrier getEffectCarrier() { return effectCarrier; }

    public PathMarks getEffectMarks() { return effectMarks; }

    public Map<GuPath, PathMarks> getEffectPaths() {
        return effectCarrier == null ? Map.of() : Map.of(path, effectMarks);
    }

    public static long getDefaultStrands(Rank rank) {
        int steps = Math.max(0, rank.ordinal() - Rank.LOWEST.ordinal());
        return LongMath.saturatedMultiply(DEFAULT_STRANDS, LongMath.saturatedPow(STRANDS_PER_RANK, steps));
    }

    public <P> PathEffect<P> createEffect(PathEffectType<P> effectType, ResourceLocation source, long endTick,
                                          P payload) {
        if (effectCarrier == null) throw new IllegalStateException("Gu properties declare no effect for " + source);
        return PathEffect.of(effectType, effectCarrier, source, getEffectPaths(), endTick, payload);
    }

    public GuHunger buildHunger() {
        if (recoverTicks > 0) return new GuHunger.RecoverBar(maxHunger, recoverTicks, hungerPerUse);
        if (maxHunger > 0) {
            return new GuHunger.HungerBar(maxHunger, unitsPerHunger, essencePerRound, hungerPerRound, hungerPerUse,
                    hungerPerDay);
        }
        return new GuHunger.NoClock();
    }

    public GuHealth buildHealth() { return new GuHealth(maxHealth, unitsPerHunger); }

    public int feedUnits(ItemStack food) {
        return feedTag != null && food.is(feedTag) ? feedUnits : 0;
    }
    //endregion

    //region the startup check [启动校验]
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
        if (effectCarrier != null) validateEffect(id);

        if (!channels) return;

        if (essencePerRound == 0) throw fault(id, "channels but pours nothing");
        if (hungerPerRound <= 0) throw fault(id, "channels but costs no hunger");
        if (essencePerRound % hungerPerRound != 0) {
            throw fault(id, "round %d does not divide by %d hunger points"
                    .formatted(essencePerRound, hungerPerRound));
        }
    }

    private void validateEffect(String id) {
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
        return new IllegalStateException("Gu properties for '%s' %s".formatted(id, what));
    }
    //endregion
}
