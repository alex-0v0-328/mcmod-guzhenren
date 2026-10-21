package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * Strength [力道]: the beast and human strengths a player has refined into themselves. Immutable record
 * attachment keyed {@code strength_data}; {@code
 * PathStrengthService} is the only runtime writer. A {@code Set}
 * of {@link BeastStrength} plus a sparse {@code Map} of {@link HumanStrength} layer counts, ctor-capped.
 *
 * <p>⚠ The ctor must build {@code new EnumMap<>(Class)} + {@code putAll} / {@code noneOf} + {@code
 * addAll} -- the {@code EnumMap(Map)} copy constructor throws on the empty maps {@code DEFAULT} uses. ⚠
 * The four layer caps (9/9/30/30) sum to exactly 9,999 jin; changing one breaks that identity. ⚠
 * {@code beastReadings()} groups by {@code BeastStrengthFamily}, not species -- two boars share one.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthService
 * @since 1.0.0
 */

public record PathStrengthData(Set<BeastStrength> beasts, Map<HumanStrength, Integer> humanStrength) {

    public static final PathStrengthData DEFAULT = new PathStrengthData(Set.of(), Map.of());
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

    public int humanStrengthCount(HumanStrength kind) { return humanStrength.getOrDefault(kind, 0); }

    public boolean isEmpty() { return beasts.isEmpty() && humanStrength.isEmpty(); }

    public PathStrengthData with(BeastStrength beast) { return rebuilt(beast, true); }

    public PathStrengthData without(BeastStrength beast) { return rebuilt(beast, false); }

    public Map<BeastStrengthFamily, Integer> beastReadings() {
        Map<BeastStrengthFamily, Integer> readings = new EnumMap<>(BeastStrengthFamily.class);
        for (BeastStrength beast : beasts) readings.merge(beast.getFamily(), beast.getReading(), Integer::sum);
        return readings;
    }

    public int totalJin() {
        int total = 0;
        for (HumanStrength kind : HumanStrength.values()) total += humanStrengthCount(kind) * kind.getJin();
        return total;
    }

    public int junReading() { return reading(HumanStrength.JUN, HumanStrength.TEN_JUN); }

    public int jinReading() { return reading(HumanStrength.JIN, HumanStrength.TEN_JIN); }

    private int reading(HumanStrength base, HumanStrength ten) {
        return humanStrengthCount(base) + HumanStrength.TEN_FACTOR * humanStrengthCount(ten);
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

    private PathStrengthData rebuilt(BeastStrength beast, boolean present) {
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
