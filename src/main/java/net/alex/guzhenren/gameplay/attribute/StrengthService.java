package net.alex.guzhenren.gameplay.attribute;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthAmplifier;
import net.alex.guzhenren.gameplay.path.strength.StrengthAmplifierProvider;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The strength settlement [力道结算]: how many of a body's strength points [力量点] it brings to bear. Static,
 * read-only service above the aperture and the body, since the capacity reads both. The points come in three
 * kinds: the permanent record ({@link PathStrengthData}: human jin and beast strengths by family), the running
 * {@link StrengthAmplifier}s ({@link #getAmplifiers}: timed beast buffs and every path effect payload that is a
 * {@link StrengthAmplifierProvider}), and the points a round's beast release [解放] counts past the capacity
 * ({@link ReleaseSource}, set by the round keeper). {@link #snapshot} gathers the first two kinds and what they
 * settle to once ({@link StrengthSnapshot}); the capacity, the total and the usable points of a player each read one.
 *
 * <p>The normal carrying capacity [常态承受力] is {@link #getNormalCapacity}: {@link #getBaseCapacity}, which is
 * {@link #MORTAL_CAPACITY} plus {@link #CAPACITY_PER_RANK} per rank [转] (16, then 24 to 56 for ranks one to five)
 * times the physique's multiplier, plus what the worn skin, tendons and bones add and what the held beasts carry
 * (the stone turtle's {@link BeastStrength#getCapacityBonus}) -- tempering and beasts sit outside the multiplier.
 * The current capacity ({@link #getCapacity}) adds every amplifier's capacity bonus (the Hardship Strength
 * [苦力] one among them) and, while the All-Out Effort Gu [全力以赴] runs on a body holding the human jun or the beast
 * phantom branch, {@link #getAllOutCapacityBonus}: 50/100/200% of the normal capacity for the rank three/four/five Gu,
 * read from the effect's amplifier, which is the Gu's tier ({@link #getAllOutTier}, {@link #NO_ALL_OUT} otherwise;
 * {@link #isAllOut} is that same reading as a yes or no, for the area impact's chance and the Crash Gu dash, which
 * reads it on the owning client from the player's own synced effect and strength data). Every bonus is added on the
 * normal capacity, never multiplied with another. A body uses {@code min(total, capacity)} of the CURRENT capacity
 * ({@link #getUsablePoints}) plus the body overload [肉体过载] plus the release extra, and keeps the rest. The overload
 * ({@link #getOverloadPoints}) is the part of the timed beasts' points past the current capacity, counted on top of the
 * permanent beasts: it is brought to bear in full and paid for in health per fist hit ({@code BodyOverload}). A
 * permanent beast seated past the capacity by a command and the human jin never overload; they are only cut.
 *
 * <p>{@link #getReleasedExtra} is a round's full release [全面解放], a critical hit of sorts: the released families'
 * PERMANENT points count {@link #FULL_RELEASE_FACTOR} times, every other point once, and the whole is still cut by the
 * current capacity; the extra is what that adds over the plain {@code min(total, capacity)}. {@link #canAdmit} is the
 * gate of a new permanent beast strength: the permanent beasts that stay beside it plus the new one may not pass the
 * NORMAL capacity, which counts a staying beast's capacity bonus but neither the new one's own nor that of the beast
 * it replaces ({@code PathStrengthData#withoutReplacedBy}); amplifiers and the All-Out lift admit nothing.
 *
 * <p>⚠ The {@code int} overloads are deliberate seams so the arithmetic is unit-testable without a {@link Player}.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthService
 * @see AttackDamageService
 * @since 1.0.0
 */

public final class StrengthService {

    private StrengthService() {}

    public static final int MORTAL_CAPACITY = 16;
    public static final int CAPACITY_PER_RANK = 8;
    public static final int NO_ALL_OUT = -1;
    public static final int ALL_OUT_LOWEST_TIER = Rank.THREE.ordinal() - Rank.ONE.ordinal();
    private static final int[] ALL_OUT_CAPACITY_PERCENT = { 50, 100, 200 };
    public static final int FULL_RELEASE_FACTOR = 3;
    private static ReleaseSource releaseSource = (player, snapshot) -> 0;

    /** The round keeper's answer: the points a release lets past the capacity right now, zero outside a round. */
    @FunctionalInterface
    public interface ReleaseSource {

        int getReleasedExtra(@NotNull Player player, @NotNull StrengthSnapshot snapshot);
    }

    public static void setReleaseSource(@NotNull ReleaseSource source) { releaseSource = source; }

    public static int getBaseCapacity(@NotNull Rank rank, @NotNull ExtremePhysique physique) {
        int rankIndex = Math.min(rank.ordinal(), Rank.HIGHEST.ordinal());
        return (MORTAL_CAPACITY + CAPACITY_PER_RANK * rankIndex) * physique.getCapacityMultiplier();
    }

    public static int getNormalCapacity(@NotNull Player player) {
        BodyData body = BodyService.get(player);
        return getBaseCapacity(ApertureService.getHealthRank(player), body.extremePhysique())
                + body.skin().getCapacityBonus()
                + body.tendon().getCapacityBonus()
                + body.bone().getCapacityBonus()
                + PathStrengthService.get(player).getBeastCapacityBonus();
    }

    public static int getCapacity(@NotNull Player player) { return snapshot(player).capacity(); }

    public static int getCapacity(int normalCapacity, int amplifierBonus, int allOutTier) {
        return normalCapacity + amplifierBonus + getAllOutCapacityBonus(normalCapacity, allOutTier);
    }

    public static int getAllOutCapacityBonus(int normalCapacity, int allOutTier) {
        if (allOutTier < ALL_OUT_LOWEST_TIER) return 0;

        int percent = ALL_OUT_CAPACITY_PERCENT[Math.min(allOutTier - ALL_OUT_LOWEST_TIER,
                ALL_OUT_CAPACITY_PERCENT.length - 1)];
        return (int) ((long) Math.max(0, normalCapacity) * percent / 100L);
    }

    public static int getAllOutTier(@NotNull Player player) {
        MobEffectInstance effect = player.getEffect(ModEffects.ALL_OUT_EFFORT_GU);
        if (effect == null || PathStrengthService.getHeldBranch(player) == StrengthPathBranch.NORMAL) return NO_ALL_OUT;
        return Math.max(ALL_OUT_LOWEST_TIER, effect.getAmplifier());
    }

    public static boolean isAllOut(@NotNull Player player) { return getAllOutTier(player) != NO_ALL_OUT; }

    public static int getPermanentPoints(@NotNull Player player) {
        return PathStrengthService.get(player).getStrengthPoints();
    }

    /** Every running amplifier: the timed beast buffs, then every path effect payload that provides one. */
    public static @NotNull List<StrengthAmplifier> getAmplifiers(@NotNull Player player) {
        return getAmplifiers(player, getPermanentPoints(player), getNormalCapacity(player));
    }

    private static List<StrengthAmplifier> getAmplifiers(Player player, int permanent, int normal) {
        List<StrengthAmplifier> amplifiers = new ArrayList<>(PathStrengthService.getBeastBuffs(player));
        for (PathEffect<?> effect : PathEffectService.getEffects(player)) {
            if (!(effect.payload() instanceof StrengthAmplifierProvider provider)) continue;

            StrengthAmplifier amplifier = provider.getAmplifier(player, permanent, normal);
            if (amplifier != null) amplifiers.add(amplifier);
        }
        return amplifiers;
    }

    public static int getAmplifierPoints(@NotNull List<StrengthAmplifier> amplifiers) {
        int total = 0;
        for (StrengthAmplifier amplifier : amplifiers) total += amplifier.points();
        return total;
    }

    public static int getTimedBeastPoints(@NotNull List<StrengthAmplifier> amplifiers) {
        int total = 0;
        for (StrengthAmplifier amplifier : amplifiers) {
            if (amplifier.isBeast()) total += Math.max(0, amplifier.points());
        }
        return total;
    }

    public static int getCapacityBonus(@NotNull List<StrengthAmplifier> amplifiers) {
        int total = 0;
        for (StrengthAmplifier amplifier : amplifiers) total += amplifier.capacityBonus();
        return total;
    }

    /** The whole strength: permanent points plus every amplifier's points. */
    public static int getStrengthPoints(@NotNull Player player) { return snapshot(player).totalPoints(); }

    public static @NotNull StrengthSnapshot snapshot(@NotNull Player player) {
        int normal = getNormalCapacity(player);
        PathStrengthData data = PathStrengthService.get(player);
        int permanent = data.getStrengthPoints();
        return StrengthSnapshot.of(normal, permanent, data.getBeastPoints(), getAmplifiers(player, permanent, normal),
                getAllOutTier(player));
    }

    public static int getUsablePoints(@NotNull Player player) { return getUsablePoints(player, snapshot(player)); }

    public static int getUsablePoints(@NotNull Player player, @NotNull StrengthSnapshot snapshot) {
        return getUsablePoints(snapshot.capacity(), snapshot.totalPoints()) + snapshot.overloadPoints()
                + releaseSource.getReleasedExtra(player, snapshot);
    }

    public static int getUsablePoints(int capacity, int total) { return Math.min(capacity, total); }

    public static int getOverloadPoints(int capacity, int permanentBeastPoints, int timedBeastPoints) {
        int timed = Math.max(0, timedBeastPoints);
        long past = (long) Math.max(0, permanentBeastPoints) + timed - capacity;
        return (int) Math.clamp(past, 0L, timed);
    }

    public static int getReleasedExtra(int capacity, int total, int fullReleasePoints) {
        long released = Math.max(0, total) + (long) (FULL_RELEASE_FACTOR - 1) * Math.max(0, fullReleasePoints);
        return Math.max(0, (int) Math.min(capacity, released) - getUsablePoints(capacity, total));
    }

    /** Whether the normal capacity has room for one more permanent beast strength beside those held. */
    public static boolean canAdmit(@NotNull Player player, @NotNull BeastStrength beast) {
        PathStrengthData held = PathStrengthService.get(player);
        PathStrengthData kept = held.withoutReplacedBy(beast);
        int normal = getNormalCapacity(player) - held.getBeastCapacityBonus() + kept.getBeastCapacityBonus();
        return canAdmit(normal, kept.getBeastPoints(), beast.getPoints());
    }

    public static boolean canAdmit(int normalCapacity, int heldBeastPoints, int newBeastPoints) {
        return heldBeastPoints + newBeastPoints <= normalCapacity;
    }
}
