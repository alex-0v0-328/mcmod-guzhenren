package net.alex.guzhenren.item.gu;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One Gu's numbers, given as a builder chain at registration so that a registration reads as a table row.
 *
 * <p>The single source of truth for every Gu's numerical identity: refine cost, channel/cost-per-use,
 * hunger bar geometry, feed tags, and cooldown. Stored on {@link MortalGuItem} at
 * construction; read by {@link TendedGuItem}, {@link GuClock}, and the tooltip/HUD renderers.
 *
 * <p>⚠ {@link #validate} runs at registration and refuses to start the game, naming the Gu, when
 * the channel numbers do not divide evenly. It is the first line of defense; never route around it.
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
    private int refineCost;
    private int essencePerRound;
    private boolean channels;
    private int maxHunger;
    private int unitsPerHunger = 1;
    private int essencePerHunger;
    private int hungerPerUse = 1;
    private @Nullable TagKey<Item> feedTag;
    private int feedUnits;
    private int effectCooldownTicks;
    private int itemCooldownTicks;

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

    public GuSpec essencePerHunger(int essence) {
        this.essencePerHunger = essence;
        return this;
    }

    public GuSpec hungerPerUse(int points) {
        this.hungerPerUse = points;
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
    //endregion

    //region what the base classes read
    public Rank rank() { return rank; }

    public GuPath path() { return path; }

    public StrengthPathBranch strengthPathBranch() { return strengthPathBranch; }

    public int refineCost() { return refineCost; }

    public int essencePerRound() { return essencePerRound; }

    public boolean channels() { return channels; }

    public int effectCooldownTicks() { return effectCooldownTicks; }

    public int itemCooldownTicks() { return itemCooldownTicks; }

    public int unitsPerHunger() { return unitsPerHunger; }

    public GuClock buildClock() {
        if (maxHunger > 0) {
            return new GuClock.HungerBar(maxHunger, unitsPerHunger, essencePerHunger, hungerPerUse);
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

        if (!channels) return;

        if (essencePerRound == 0) throw fault(id, "channels but pours nothing");
        if (essencePerHunger <= 0) throw fault(id, "channels but costs no hunger");
        if (essencePerRound % essencePerHunger != 0) {
            throw fault(id, "round %d does not divide by hunger rate %d"
                    .formatted(essencePerRound, essencePerHunger));
        }
    }

    private static IllegalStateException fault(String id, String what) {
        return new IllegalStateException("Gu spec for '%s' %s".formatted(id, what));
    }
    //endregion
}
