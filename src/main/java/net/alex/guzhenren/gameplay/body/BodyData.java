package net.alex.guzhenren.gameplay.body;

import com.google.common.math.LongMath;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.alex.guzhenren.core.ModStreamCodecs;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Body [肉身] state: cumulative physiques, race, age and lifespan. Immutable record attachment keyed
 * {@code body_data}; {@link BodyService} is the only
 * writer. The concrete Ten-Extreme physique belongs here; the aperture stores only its base-essence
 * value. Zombie and half-zombie are mutually exclusive; Extreme may coexist with either one.
 *
 * <p>⚠ Ten components, so the {@code STREAM_CODEC} is handwritten and the encoder mirrors the decoder
 * order exactly. ⚠ The time anchors default to {@code UNTRACKED} (-1), never {@code 0}: zero is a real
 * game time. ⚠ Every caller speaks YEARS; only this file knows parts exist.
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @since 1.0.0
 */

public record BodyData(
        Set<Physique> physiques,
        ExtremePhysique extremePhysique,
        Race race,
        long ageParts,
        long lifespanParts,
        long lastDayIndex,
        long deathQiLifespanLost,
        long halfZombieEndTick,
        int zombieTier,
        long lastBilledTick
) {

    public static final long UNTRACKED = -1L;
    public static final int NO_ZOMBIE_TIER = -1;
    public static final long PARTS_PER_TICK = 6L;
    public static final long PARTS_PER_YEAR = Ticks.DAY * PARTS_PER_TICK;
    public static final long DEFAULT_AGE = 14L;
    public static final long DEFAULT_LIFESPAN = 86L;
    public static final long ZOMBIE_LIFESPAN = 1L;
    public static final long RELAPSE_WINDOW_TICKS = 5L * Ticks.MINUTE;
    public static final BodyData DEFAULT = new BodyData(Set.of(), ExtremePhysique.NONE, Race.HUMAN,
            parts(DEFAULT_AGE), parts(DEFAULT_LIFESPAN), UNTRACKED, 0L, UNTRACKED, NO_ZOMBIE_TIER, UNTRACKED);

    public static long parts(long years) { return LongMath.saturatedMultiply(years, PARTS_PER_YEAR); }

    private static final Codec<Set<Physique>> PHYSIQUES_CODEC = Physique.CODEC.listOf()
            .xmap(BodyData::normalizePhysiques, ArrayList::new);
    private static final Codec<BodyData> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PHYSIQUES_CODEC.optionalFieldOf("physiques", Set.of()).forGetter(BodyData::physiques),
            ExtremePhysique.CODEC.optionalFieldOf("extreme_physique", ExtremePhysique.NONE)
                    .forGetter(BodyData::extremePhysique),
            Race.CODEC.optionalFieldOf("race", Race.HUMAN).forGetter(BodyData::race),
            Codec.LONG.optionalFieldOf("age_parts", parts(DEFAULT_AGE)).forGetter(BodyData::ageParts),
            Codec.LONG.optionalFieldOf("lifespan_parts", parts(DEFAULT_LIFESPAN)).forGetter(BodyData::lifespanParts),
            Codec.LONG.optionalFieldOf("last_day_index", UNTRACKED).forGetter(BodyData::lastDayIndex),
            Codec.LONG.optionalFieldOf("death_qi_lifespan_lost", 0L).forGetter(BodyData::deathQiLifespanLost),
            Codec.LONG.optionalFieldOf("half_zombie_end_tick", UNTRACKED).forGetter(BodyData::halfZombieEndTick),
            Codec.INT.optionalFieldOf("zombie_tier", NO_ZOMBIE_TIER).forGetter(BodyData::zombieTier),
            Codec.LONG.optionalFieldOf("last_billed_tick", UNTRACKED).forGetter(BodyData::lastBilledTick)
    ).apply(instance, BodyData::new));
    private static final Codec<BodyData> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LegacyLifeForm.CODEC.optionalFieldOf("life_form", LegacyLifeForm.ALIVE).forGetter(data ->
                    data.hasPhysique(Physique.ZOMBIE) ? LegacyLifeForm.ZOMBIE
                            : data.hasPhysique(Physique.HALF_ZOMBIE) ? LegacyLifeForm.HALF_ZOMBIE
                            : LegacyLifeForm.ALIVE),
            Race.CODEC.optionalFieldOf("race", Race.HUMAN).forGetter(BodyData::race),
            Codec.LONG.optionalFieldOf("age_parts", parts(DEFAULT_AGE)).forGetter(BodyData::ageParts),
            Codec.LONG.optionalFieldOf("lifespan_parts", parts(DEFAULT_LIFESPAN)).forGetter(BodyData::lifespanParts),
            Codec.LONG.optionalFieldOf("last_day_index", UNTRACKED).forGetter(BodyData::lastDayIndex),
            Codec.LONG.optionalFieldOf("death_qi_lifespan_lost", 0L).forGetter(BodyData::deathQiLifespanLost),
            Codec.LONG.optionalFieldOf("half_zombie_end_tick", UNTRACKED).forGetter(BodyData::halfZombieEndTick),
            Codec.INT.optionalFieldOf("zombie_tier", NO_ZOMBIE_TIER).forGetter(BodyData::zombieTier),
            Codec.LONG.optionalFieldOf("last_billed_tick", UNTRACKED).forGetter(BodyData::lastBilledTick)
    ).apply(instance, BodyData::fromLegacy));
    private static final Decoder<BodyData> DECODER = new Decoder<>() {
        @Override
        public <T> DataResult<Pair<BodyData, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map ->
                    (map.get("life_form") != null ? LEGACY_CODEC : CURRENT_CODEC).decode(ops, input));
        }
    };
    public static final Codec<BodyData> CODEC = Codec.of(CURRENT_CODEC, DECODER);
    private static final StreamCodec<ByteBuf, Set<Physique>> PHYSIQUES = ModStreamCodecs.enumSet(Physique.class);
    private static final StreamCodec<ByteBuf, ExtremePhysique> EXTREME =
            ModStreamCodecs.ofEnum(ExtremePhysique.class);
    private static final StreamCodec<ByteBuf, Race> RACE = ModStreamCodecs.ofEnum(Race.class);
    public static final StreamCodec<ByteBuf, BodyData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull BodyData decode(@NotNull ByteBuf buf) {
            return new BodyData(
                    PHYSIQUES.decode(buf),
                    EXTREME.decode(buf),
                    RACE.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf));
        }

        @Override
        public void encode(@NotNull ByteBuf buf, @NotNull BodyData value) {
            PHYSIQUES.encode(buf, value.physiques());
            EXTREME.encode(buf, value.extremePhysique());
            RACE.encode(buf, value.race());
            ByteBufCodecs.VAR_LONG.encode(buf, value.ageParts());
            ByteBufCodecs.VAR_LONG.encode(buf, value.lifespanParts());
            ByteBufCodecs.VAR_LONG.encode(buf, value.lastDayIndex());
            ByteBufCodecs.VAR_LONG.encode(buf, value.deathQiLifespanLost());
            ByteBufCodecs.VAR_LONG.encode(buf, value.halfZombieEndTick());
            ByteBufCodecs.VAR_INT.encode(buf, value.zombieTier());
            ByteBufCodecs.VAR_LONG.encode(buf, value.lastBilledTick());
        }
    };

    public BodyData {
        EnumSet<Physique> normalized = EnumSet.noneOf(Physique.class);
        normalized.addAll(physiques);
        if (normalized.contains(Physique.ZOMBIE)) normalized.remove(Physique.HALF_ZOMBIE);
        if (extremePhysique == ExtremePhysique.NONE) normalized.remove(Physique.EXTREME);
        else normalized.add(Physique.EXTREME);
        physiques = Collections.unmodifiableSet(normalized);
        ageParts = Math.max(0L, ageParts);
        deathQiLifespanLost = Math.max(0L, deathQiLifespanLost);
        if (!normalized.contains(Physique.ZOMBIE) && !normalized.contains(Physique.HALF_ZOMBIE)) {
            zombieTier = NO_ZOMBIE_TIER;
        }
    }

    private static Set<Physique> normalizePhysiques(List<Physique> values) {
        return Set.copyOf(values);
    }

    private static BodyData fromLegacy(LegacyLifeForm form, Race race, long ageParts, long lifespanParts,
                                       long lastDayIndex, long deathQiLifespanLost, long halfZombieEndTick,
                                       int zombieTier, long lastBilledTick) {
        Set<Physique> physiques = switch (form) {
            case ZOMBIE -> Set.of(Physique.ZOMBIE);
            case HALF_ZOMBIE -> Set.of(Physique.HALF_ZOMBIE);
            case ALIVE, DEAD -> Set.of();
        };
        return new BodyData(physiques, ExtremePhysique.NONE, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick);
    }

    public boolean isExhausted() { return lifespanParts <= 0L; }

    public boolean hasPhysique(Physique physique) { return physiques.contains(physique); }

    public boolean isZombie() { return hasPhysique(Physique.ZOMBIE); }

    public boolean isHalfZombie() { return hasPhysique(Physique.HALF_ZOMBIE); }

    public boolean isZombieOrHalfZombie() { return isZombie() || isHalfZombie(); }

    public boolean isExtreme() { return hasPhysique(Physique.EXTREME); }

    public double ageYears() { return ageParts / (double) PARTS_PER_YEAR; }

    public double lifespanYears() { return lifespanParts / (double) PARTS_PER_YEAR; }

    public boolean halfZombieRanOut(long now) { return halfZombieEndTick != UNTRACKED && now >= halfZombieEndTick; }

    public long halfZombieTicksLeft(long now) { return Math.max(0L, halfZombieEndTick - now); }

    public boolean withinRelapseWindow(long now) {
        return halfZombieEndTick != UNTRACKED && now < halfZombieEndTick + RELAPSE_WINDOW_TICKS;
    }

    public BodyData withPhysiques(Set<Physique> v) {
        return new BodyData(v, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick);
    }

    public BodyData withExtremePhysique(ExtremePhysique v) {
        return new BodyData(physiques, v, race, ageParts, lifespanParts, lastDayIndex, deathQiLifespanLost,
                halfZombieEndTick, zombieTier, lastBilledTick);
    }

    public BodyData revived() {
        EnumSet<Physique> next = EnumSet.noneOf(Physique.class);
        next.addAll(physiques);
        next.remove(Physique.ZOMBIE);
        next.remove(Physique.HALF_ZOMBIE);
        return new BodyData(next, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, UNTRACKED, NO_ZOMBIE_TIER, lastBilledTick);
    }

    public BodyData withRace(Race v) {
        return new BodyData(physiques, extremePhysique, v, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick);
    }

    public BodyData withAgeParts(long v) {
        return new BodyData(physiques, extremePhysique, race, v, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick);
    }

    public BodyData withLifespanParts(long v) {
        return new BodyData(physiques, extremePhysique, race, ageParts, v, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick);
    }

    public BodyData withLastDayIndex(long v) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, v,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick);
    }

    public BodyData withDeathQiLifespanLost(long v) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, v,
                halfZombieEndTick, zombieTier, lastBilledTick);
    }

    public BodyData withHalfZombieEndTick(long v) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, v, zombieTier, lastBilledTick);
    }

    public BodyData withZombieTier(int v) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, v, lastBilledTick);
    }

    public BodyData withLastBilledTick(long v) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, v);
    }

    public BodyData lived(long parts, long billedTick) {
        return new BodyData(physiques, extremePhysique, race, LongMath.saturatedAdd(ageParts, parts),
                LongMath.saturatedSubtract(lifespanParts, parts),
                lastDayIndex, deathQiLifespanLost, halfZombieEndTick, zombieTier, billedTick);
    }

    private enum LegacyLifeForm implements StringRepresentable {

        ALIVE,
        DEAD,
        ZOMBIE,
        HALF_ZOMBIE;

        private static final Codec<LegacyLifeForm> CODEC = StringRepresentable.fromEnum(LegacyLifeForm::values);

        @Override
        public @NotNull String getSerializedName() { return name().toLowerCase(); }
    }
}
