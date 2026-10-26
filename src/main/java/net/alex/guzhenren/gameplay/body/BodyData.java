package net.alex.guzhenren.gameplay.body;

import com.google.common.math.LongMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * Body [肉身] state: cumulative physiques, race, skin, tendons, bones, age and lifespan. Immutable record
 * attachment keyed {@code body_data}; {@code BodyService} is the only writer. The concrete Ten-Extreme physique
 * belongs here; the aperture stores only its base-essence value. Zombie and half-zombie are mutually exclusive;
 * Extreme may coexist with either one. The constructor drops half-zombie whenever zombie is present, so
 * {@link #with with(ZOMBIE)} needs no removal first, while {@code with(HALF_ZOMBIE)} must follow
 * {@code without(ZOMBIE)}; it also clears the zombie tier once neither is left.
 *
 * <p>The skin [皮肤] is one slot, native by default, with its own end tick: a jade skin is worn until
 * {@code skinEndTick}, which {@code SkinService} checks on the heartbeat, and {@link #withRevived} drops it with
 * the undead forms; a bronze or iron skin is permanent, keeps no end tick and survives a respawn. The
 * {@code JADE_SKIN_GU} / {@code WHITE_JADE_GU} effects are only the jade skins' projection. The tendons [筋]
 * and bones [骨] are two more permanent slots, native by default.
 *
 * <p>⚠ Fourteen components, so {@code encode}/{@code decode} are handwritten and the encoder mirrors the decoder
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
        long lastBilledTick,
        Skin skin,
        long skinEndTick,
        Tendon tendon,
        Bone bone
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
            toParts(DEFAULT_AGE), toParts(DEFAULT_LIFESPAN), UNTRACKED, 0L, UNTRACKED, NO_ZOMBIE_TIER, UNTRACKED,
            Skin.NATIVE, UNTRACKED, Tendon.NATIVE, Bone.NATIVE);

    public static long toParts(long years) { return LongMath.saturatedMultiply(years, PARTS_PER_YEAR); }

    private static final Codec<Set<Physique>> PHYSIQUES_CODEC = Physique.CODEC.listOf()
            .xmap(Set::copyOf, ArrayList::new);
    public static final Codec<BodyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PHYSIQUES_CODEC.optionalFieldOf("physiques", Set.of()).forGetter(BodyData::physiques),
            ExtremePhysique.CODEC.optionalFieldOf("extreme_physique", ExtremePhysique.NONE)
                    .forGetter(BodyData::extremePhysique),
            Race.CODEC.optionalFieldOf("race", Race.HUMAN).forGetter(BodyData::race),
            Codec.LONG.optionalFieldOf("age_parts", toParts(DEFAULT_AGE)).forGetter(BodyData::ageParts),
            Codec.LONG.optionalFieldOf("lifespan_parts", toParts(DEFAULT_LIFESPAN)).forGetter(BodyData::lifespanParts),
            Codec.LONG.optionalFieldOf("last_day_index", UNTRACKED).forGetter(BodyData::lastDayIndex),
            Codec.LONG.optionalFieldOf("death_qi_lifespan_lost", 0L).forGetter(BodyData::deathQiLifespanLost),
            Codec.LONG.optionalFieldOf("half_zombie_end_tick", UNTRACKED).forGetter(BodyData::halfZombieEndTick),
            Codec.INT.optionalFieldOf("zombie_tier", NO_ZOMBIE_TIER).forGetter(BodyData::zombieTier),
            Codec.LONG.optionalFieldOf("last_billed_tick", UNTRACKED).forGetter(BodyData::lastBilledTick),
            Skin.CODEC.optionalFieldOf("skin", Skin.NATIVE).forGetter(BodyData::skin),
            Codec.LONG.optionalFieldOf("skin_end_tick", UNTRACKED).forGetter(BodyData::skinEndTick),
            Tendon.CODEC.optionalFieldOf("tendon", Tendon.NATIVE).forGetter(BodyData::tendon),
            Bone.CODEC.optionalFieldOf("bone", Bone.NATIVE).forGetter(BodyData::bone)
    ).apply(instance, BodyData::new));
    public static @NotNull BodyData decode(@NotNull FriendlyByteBuf buf) {
        return new BodyData(
                ModByteBufs.readEnumSet(buf, Physique.class),
                ModByteBufs.readEnum(buf, ExtremePhysique.class),
                ModByteBufs.readEnum(buf, Race.class),
                buf.readVarLong(),
                buf.readVarLong(),
                buf.readVarLong(),
                buf.readVarLong(),
                buf.readVarLong(),
                buf.readVarInt(),
                buf.readVarLong(),
                ModByteBufs.readEnum(buf, Skin.class),
                buf.readVarLong(),
                ModByteBufs.readEnum(buf, Tendon.class),
                ModByteBufs.readEnum(buf, Bone.class));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull BodyData value) {
        ModByteBufs.writeEnumSet(buf, value.physiques());
        ModByteBufs.writeEnum(buf, value.extremePhysique());
        ModByteBufs.writeEnum(buf, value.race());
        buf.writeVarLong(value.ageParts());
        buf.writeVarLong(value.lifespanParts());
        buf.writeVarLong(value.lastDayIndex());
        buf.writeVarLong(value.deathQiLifespanLost());
        buf.writeVarLong(value.halfZombieEndTick());
        buf.writeVarInt(value.zombieTier());
        buf.writeVarLong(value.lastBilledTick());
        ModByteBufs.writeEnum(buf, value.skin());
        buf.writeVarLong(value.skinEndTick());
        ModByteBufs.writeEnum(buf, value.tendon());
        ModByteBufs.writeEnum(buf, value.bone());
    }

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
        if (!skin.isTimed()) skinEndTick = UNTRACKED;
    }

    public boolean isExhausted() { return lifespanParts <= 0L; }

    public boolean hasPhysique(Physique physique) { return physiques.contains(physique); }

    public boolean isZombie() { return hasPhysique(Physique.ZOMBIE); }

    public boolean isHalfZombie() { return hasPhysique(Physique.HALF_ZOMBIE); }

    public boolean isZombieOrHalfZombie() { return isZombie() || isHalfZombie(); }

    public boolean isExtreme() { return hasPhysique(Physique.EXTREME); }

    public boolean hasNativeSkin() { return skin.isNative(); }

    public boolean hasSkinRunOut(long now) { return skin.isTimed() && now >= skinEndTick; }

    public long getSkinTicksLeft(long now) { return skin.isTimed() ? Math.max(0L, skinEndTick - now) : 0L; }

    public double ageYears() { return ageParts / (double) PARTS_PER_YEAR; }

    public double getLifespanYears() { return lifespanParts / (double) PARTS_PER_YEAR; }

    public boolean hasHalfZombieRunOut(long now) { return halfZombieEndTick != UNTRACKED && now >= halfZombieEndTick; }

    public long getHalfZombieTicksLeft(long now) { return Math.max(0L, halfZombieEndTick - now); }

    public boolean isWithinRelapseWindow(long now) {
        return halfZombieEndTick != UNTRACKED && now < halfZombieEndTick + RELAPSE_WINDOW_TICKS;
    }

    public BodyData withPhysiques(Set<Physique> value) {
        return new BodyData(value, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone);
    }

    public BodyData withExtremePhysique(ExtremePhysique physique) {
        return new BodyData(physiques, physique, race, ageParts, lifespanParts, lastDayIndex, deathQiLifespanLost,
                halfZombieEndTick, zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone);
    }

    public BodyData withRevived() {
        return without(Physique.ZOMBIE).without(Physique.HALF_ZOMBIE).withHalfZombieEndTick(UNTRACKED)
                .withSkin(skin.isTimed() ? Skin.NATIVE : skin, UNTRACKED);
    }

    public BodyData with(Physique physique) { return withPhysiques(getChanged(physique, true)); }

    public BodyData without(Physique physique) { return withPhysiques(getChanged(physique, false)); }

    private Set<Physique> getChanged(Physique physique, boolean present) {
        EnumSet<Physique> next = physiques.isEmpty() ? EnumSet.noneOf(Physique.class) : EnumSet.copyOf(physiques);
        if (present) next.add(physique);
        else next.remove(physique);
        return next;
    }

    public BodyData withRace(Race race) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone);
    }

    public BodyData withAgeParts(long value) {
        return new BodyData(physiques, extremePhysique, race, value, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone);
    }

    public BodyData withLifespanParts(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, value, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone);
    }

    public BodyData withLastDayIndex(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, value,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone);
    }

    public BodyData withDeathQiLifespanLost(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, value,
                halfZombieEndTick, zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone);
    }

    public BodyData withHalfZombieEndTick(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, value, zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone);
    }

    public BodyData withZombieTier(int value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, value, lastBilledTick, skin, skinEndTick, tendon, bone);
    }

    public BodyData withSkin(Skin value, long endTick) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick, value, endTick, tendon, bone);
    }

    public BodyData withTendon(Tendon value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick, skin, skinEndTick, value, bone);
    }

    public BodyData withBone(Bone value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, lastBilledTick, skin, skinEndTick, tendon, value);
    }

    public BodyData withLastBilledTick(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex,
                deathQiLifespanLost, halfZombieEndTick, zombieTier, value, skin, skinEndTick, tendon, bone);
    }

    public BodyData withLived(long parts, long billedTick) {
        return new BodyData(physiques, extremePhysique, race, LongMath.saturatedAdd(ageParts, parts),
                LongMath.saturatedSubtract(lifespanParts, parts),
                lastDayIndex, deathQiLifespanLost, halfZombieEndTick, zombieTier, billedTick, skin, skinEndTick,
                tendon, bone);
    }
}
