package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Strength [力道]: the beast and human strengths a player has refined into themselves. Immutable record
 * attachment keyed {@code strength_data}; {@code
 * PathStrengthService} is the only runtime writer. A {@code Set}
 * of {@link BeastStrength} plus a sparse {@code Map} of {@link HumanStrength} layer counts, ctor-capped.
 * {@link #getStrengthPoints} is the one unit both sides share: a jin of human strength is one strength point
 * [力量点], a beast its declared points, and a point is {@link #ATTACK_PER_POINT} attack. A running beast buff adds
 * its points as a {@link StrengthAmplifier} at {@code StrengthService.getStrengthPoints}, not here: this record
 * holds only what is permanent. {@link #with} first takes out what the new beast {@link BeastStrength#replaces}, so a
 * family holds one strength save the paired boars; {@link #getCovering} names the held beast that makes a new one
 * pointless, and {@link #withoutReplacedBy} is what stays beside it.
 *
 * <p>⚠ The ctor must build {@code new EnumMap<>(Class)} + {@code putAll} / {@code noneOf} + {@code
 * addAll} -- the {@code EnumMap(Map)} copy constructor throws on the empty maps {@code DEFAULT} uses. ⚠
 * The five layer caps (9/9/30/30/30) sum to exactly 99,999 jin; changing one breaks that identity. ⚠
 * {@code getBeastReadings()} counts by {@code BeastStrengthFamily}, not species -- the two boars share one. The ctor
 * keeps whatever set it is given; only {@link #with} applies the replacement.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthService
 * @since 1.0.0
 */

public record PathStrengthData(Set<BeastStrength> beasts, Map<HumanStrength, Integer> humanStrength) {

    public static final PathStrengthData DEFAULT = new PathStrengthData(Set.of(), Map.of());
    public static final double ATTACK_PER_POINT = 0.125D;
    public static final Codec<PathStrengthData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BeastStrength.CODEC.listOf().optionalFieldOf("beasts", List.of())
                    .forGetter(data -> List.copyOf(data.beasts())),
            Codec.unboundedMap(HumanStrength.CODEC, Codec.INT).optionalFieldOf("human_strength", Map.of())
                    .forGetter(PathStrengthData::humanStrength)
    ).apply(instance, (beasts, humanStrength) -> new PathStrengthData(Set.copyOf(beasts), humanStrength)));

    public PathStrengthData {
        EnumSet<BeastStrength> takenBeasts = EnumSet.noneOf(BeastStrength.class);
        takenBeasts.addAll(beasts);
        beasts = Collections.unmodifiableSet(takenBeasts);

        Map<HumanStrength, Integer> pruned = new EnumMap<>(HumanStrength.class);
        humanStrength.forEach((kind, count) -> {
            if (count != null && count > 0) pruned.put(kind, Math.min(count, kind.getMaxLayers()));
        });
        humanStrength = Collections.unmodifiableMap(pruned);
    }

    public static @NotNull PathStrengthData decode(@NotNull FriendlyByteBuf buf) {
        return new PathStrengthData(
                ModByteBufs.readEnumSet(buf, BeastStrength.class),
                ModByteBufs.readEnumMap(buf, HumanStrength.class, FriendlyByteBuf::readVarInt));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull PathStrengthData value) {
        ModByteBufs.writeEnumSet(buf, value.beasts());
        ModByteBufs.writeEnumMap(buf, value.humanStrength(), FriendlyByteBuf::writeVarInt);
    }

    public boolean has(BeastStrength beast) { return beasts.contains(beast); }

    public int getHumanStrengthCount(HumanStrength kind) { return humanStrength.getOrDefault(kind, 0); }

    public boolean isEmpty() { return beasts.isEmpty() && humanStrength.isEmpty(); }

    public PathStrengthData with(BeastStrength beast) { return withoutReplacedBy(beast).rebuild(beast, true); }

    public PathStrengthData withoutReplacedBy(BeastStrength beast) {
        EnumSet<BeastStrength> kept = EnumSet.noneOf(BeastStrength.class);
        for (BeastStrength held : beasts) if (!beast.replaces(held)) kept.add(held);
        return new PathStrengthData(kept, humanStrength);
    }

    public @Nullable BeastStrength getCovering(BeastStrength beast) {
        for (BeastStrength held : beasts) if (beast.isCoveredBy(held)) return held;
        return null;
    }

    public PathStrengthData without(BeastStrength beast) { return rebuild(beast, false); }

    public Map<BeastStrengthFamily, Integer> getBeastReadings() {
        Map<BeastStrengthFamily, Integer> readings = new EnumMap<>(BeastStrengthFamily.class);
        for (BeastStrength beast : beasts) readings.merge(beast.getFamily(), 1, Integer::sum);
        return readings;
    }

    public @Nullable BeastStrength getInnate(BeastStrengthFamily family) {
        for (BeastStrength beast : beasts) {
            if (beast.getFamily() == family && beast.getForm() == BeastStrength.Form.INNATE) return beast;
        }
        return null;
    }

    public int getTotalJin() {
        int total = 0;
        for (HumanStrength kind : HumanStrength.values()) total += getHumanStrengthCount(kind) * kind.getJin();
        return total;
    }

    public int getBeastPoints() {
        int total = 0;
        for (BeastStrength beast : beasts) total += beast.getPoints();
        return total;
    }

    public int getBeastPoints(BeastStrengthFamily family) {
        int total = 0;
        for (BeastStrength beast : beasts) if (beast.getFamily() == family) total += beast.getPoints();
        return total;
    }

    public int getBeastCapacityBonus() {
        int total = 0;
        for (BeastStrength beast : beasts) total += beast.getCapacityBonus();
        return total;
    }

    public int getBeastDurationBonus() {
        int total = 0;
        for (BeastStrength beast : beasts) total += beast.getDurationBonusTicks();
        return total;
    }

    public float getBeastChanceBonus() {
        float total = 0.0F;
        for (BeastStrength beast : beasts) total += beast.getChanceBonus();
        return total;
    }

    public double getBeastDashBonus() {
        double total = 0.0D;
        for (BeastStrength beast : beasts) total += beast.getDashBonus();
        return total;
    }

    public float getShieldMultiplier() {
        float total = 1.0F;
        for (BeastStrength beast : beasts) total += beast.getShieldBonus();
        return total;
    }

    public boolean hasTwoBoars() { return has(BeastStrength.WHITE_BOAR) && has(BeastStrength.BLACK_BOAR); }

    public double getBeastSprintBonus() {
        double total = 0.0D;
        for (BeastStrength beast : beasts) total += beast.getSprintBonus();
        return total;
    }

    public double getBeastSprintBonus(Collection<BeastStrengthFamily> families) {
        double total = 0.0D;
        for (BeastStrength beast : beasts) if (families.contains(beast.getFamily())) total += beast.getSprintBonus();
        return total;
    }

    /** The permanent strength points [力量点]: every jin plus every refined beast's points. */
    public int getStrengthPoints() { return getTotalJin() + getBeastPoints(); }

    public int getJunReading() {
        return getReading(HumanStrength.JUN, HumanStrength.TEN_JUN)
                + HumanStrength.HUNDRED_FACTOR * getHumanStrengthCount(HumanStrength.HUNDRED_JUN);
    }

    public int getJinReading() { return getReading(HumanStrength.JIN, HumanStrength.TEN_JIN); }

    private int getReading(HumanStrength base, HumanStrength ten) {
        return getHumanStrengthCount(base) + HumanStrength.TEN_FACTOR * getHumanStrengthCount(ten);
    }

    public boolean hasPathBranch(StrengthPathBranch branch) {
        return switch (branch) {
            case BEAST_STRENGTH_PHANTOM -> !beasts.isEmpty();
            case HUMAN_JUN_STRENGTH -> !humanStrength.isEmpty();
            case ATMOSPHERIC_HEAVEN_AND_EARTH, NORMAL -> false;
        };
    }

    public PathStrengthData withHumanStrength(HumanStrength kind, int count) {
        Map<HumanStrength, Integer> next = new EnumMap<>(HumanStrength.class);
        next.putAll(humanStrength);
        next.put(kind, Math.max(0, count));
        return new PathStrengthData(beasts, next);
    }

    private PathStrengthData rebuild(BeastStrength beast, boolean present) {
        EnumSet<BeastStrength> next = EnumSet.noneOf(BeastStrength.class);
        next.addAll(beasts);
        if (present) {
            next.add(beast);
        } else {
            next.remove(beast);
        }
        return new PathStrengthData(next, humanStrength);
    }
}
