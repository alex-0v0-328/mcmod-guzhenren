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
 * 肉身状态，附件 {@code body_data}：累积体质、种族、筋骨皮肉、年龄、寿元。
 * <ul>
 *   <li>仅 {@code BodyService} 写；具体十绝体存此，空窍[Aperture]只存其基础真元[Essence]。</li>
 *   <li>⚠ 14 个组件手写 {@code encode}/{@code decode}，两者顺序须严格一致。</li>
 *   <li>⚠ 时间锚点默认 {@code UNTRACKED}（-1），不用 {@code 0}：0 是真实游戏时间。</li>
 *   <li>⚠ 调用方一律按年；仅本文件知细分单位（parts）。</li>
 *   <li>僵尸与临时僵尸化互斥，十绝体可与任一共存；构造器遇僵尸丢临时僵尸化：{@link #with with(ZOMBIE)} 无需先移除，
 *       {@code with(TEMP_ZOMBIE)} 须先 {@code without(ZOMBIE)}；两者皆无则清僵尸档位。</li>
 *   <li>皮一格，默认原生，带结束 tick：玉皮穿到 {@code skinEndTick}（{@code SkinService} 心跳查），{@link #withRevived}
 *       随不死形态脱；铜皮、铁皮永久，重生保留。</li>
 *   <li>{@code JADE_SKIN_GU} / {@code WHITE_JADE_GU} 效果只是玉皮投影；筋、骨、肉为三个永久格，默认原生。</li>
 * </ul>
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
        long tempZombieEndTick,
        int zombieTier,
        long lastBilledTick,
        Skin skin,
        long skinEndTick,
        Tendon tendon,
        Bone bone,
        Flesh flesh
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
            toParts(DEFAULT_AGE), toParts(DEFAULT_LIFESPAN), UNTRACKED, UNTRACKED, NO_ZOMBIE_TIER, UNTRACKED,
            Skin.NATIVE, UNTRACKED, Tendon.NATIVE, Bone.NATIVE, Flesh.NATIVE);

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
            Codec.LONG.optionalFieldOf("temp_zombie_end_tick", UNTRACKED).forGetter(BodyData::tempZombieEndTick),
            Codec.INT.optionalFieldOf("zombie_tier", NO_ZOMBIE_TIER).forGetter(BodyData::zombieTier),
            Codec.LONG.optionalFieldOf("last_billed_tick", UNTRACKED).forGetter(BodyData::lastBilledTick),
            Skin.CODEC.optionalFieldOf("skin", Skin.NATIVE).forGetter(BodyData::skin),
            Codec.LONG.optionalFieldOf("skin_end_tick", UNTRACKED).forGetter(BodyData::skinEndTick),
            Tendon.CODEC.optionalFieldOf("tendon", Tendon.NATIVE).forGetter(BodyData::tendon),
            Bone.CODEC.optionalFieldOf("bone", Bone.NATIVE).forGetter(BodyData::bone),
            Flesh.CODEC.optionalFieldOf("flesh", Flesh.NATIVE).forGetter(BodyData::flesh)
    ).apply(instance, BodyData::new));
    public static @NotNull BodyData decode(@NotNull FriendlyByteBuf buf) {
        return new BodyData(ModByteBufs.readEnumSet(buf, Physique.class), ModByteBufs.readEnum(buf,
                ExtremePhysique.class), ModByteBufs.readEnum(buf, Race.class), buf.readVarLong(), buf.readVarLong(),
                buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readVarLong(), ModByteBufs.readEnum(buf,
                Skin.class), buf.readVarLong(), ModByteBufs.readEnum(buf, Tendon.class), ModByteBufs.readEnum(buf,
                Bone.class), ModByteBufs.readEnum(buf, Flesh.class));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull BodyData value) {
        ModByteBufs.writeEnumSet(buf, value.physiques());
        ModByteBufs.writeEnum(buf, value.extremePhysique());
        ModByteBufs.writeEnum(buf, value.race());
        buf.writeVarLong(value.ageParts());
        buf.writeVarLong(value.lifespanParts());
        buf.writeVarLong(value.lastDayIndex());
        buf.writeVarLong(value.tempZombieEndTick());
        buf.writeVarInt(value.zombieTier());
        buf.writeVarLong(value.lastBilledTick());
        ModByteBufs.writeEnum(buf, value.skin());
        buf.writeVarLong(value.skinEndTick());
        ModByteBufs.writeEnum(buf, value.tendon());
        ModByteBufs.writeEnum(buf, value.bone());
        ModByteBufs.writeEnum(buf, value.flesh());
    }

    public BodyData {
        EnumSet<Physique> normalized = EnumSet.noneOf(Physique.class);
        normalized.addAll(physiques);
        if (normalized.contains(Physique.ZOMBIE)) normalized.remove(Physique.TEMP_ZOMBIE);
        if (extremePhysique == ExtremePhysique.NONE) normalized.remove(Physique.EXTREME);
        else normalized.add(Physique.EXTREME);
        physiques = Collections.unmodifiableSet(normalized);
        ageParts = Math.max(0L, ageParts);
        if (!normalized.contains(Physique.ZOMBIE) && !normalized.contains(Physique.TEMP_ZOMBIE)) {
            zombieTier = NO_ZOMBIE_TIER;
        }
        if (!skin.isTimed()) skinEndTick = UNTRACKED;
    }

    public boolean isExhausted() { return lifespanParts <= 0L; }

    public boolean hasPhysique(Physique physique) { return physiques.contains(physique); }

    public boolean isZombie() { return hasPhysique(Physique.ZOMBIE); }

    public boolean isTempZombie() { return hasPhysique(Physique.TEMP_ZOMBIE); }

    public boolean isZombieOrTempZombie() { return isZombie() || isTempZombie(); }

    public boolean isExtreme() { return hasPhysique(Physique.EXTREME); }

    public boolean hasNativeSkin() { return skin.isNative(); }

    public boolean hasSkinRunOut(long now) { return skin.isTimed() && now >= skinEndTick; }

    public long getSkinTicksLeft(long now) { return skin.isTimed() ? Math.max(0L, skinEndTick - now) : 0L; }

    public double ageYears() { return ageParts / (double) PARTS_PER_YEAR; }

    public double getLifespanYears() { return lifespanParts / (double) PARTS_PER_YEAR; }

    public boolean hasTempZombieRunOut(long now) { return tempZombieEndTick != UNTRACKED && now >= tempZombieEndTick; }

    public long getTempZombieTicksLeft(long now) { return Math.max(0L, tempZombieEndTick - now); }

    public boolean isWithinRelapseWindow(long now) {
        return tempZombieEndTick != UNTRACKED && now < tempZombieEndTick + RELAPSE_WINDOW_TICKS;
    }

    public BodyData withPhysiques(Set<Physique> value) {
        return new BodyData(value, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh);
    }

    public BodyData withExtremePhysique(ExtremePhysique physique) {
        return new BodyData(physiques, physique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh);
    }

    public BodyData withRevived() {
        return without(Physique.ZOMBIE).without(Physique.TEMP_ZOMBIE).withTempZombieEndTick(UNTRACKED)
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
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh);
    }

    public BodyData withAgeParts(long value) {
        return new BodyData(physiques, extremePhysique, race, value, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh);
    }

    public BodyData withLifespanParts(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, value, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh);
    }

    public BodyData withLastDayIndex(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, value, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh);
    }

    public BodyData withTempZombieEndTick(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, value, zombieTier,
                lastBilledTick, skin, skinEndTick, tendon, bone, flesh);
    }

    public BodyData withZombieTier(int value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                value, lastBilledTick, skin, skinEndTick, tendon, bone, flesh);
    }

    public BodyData withSkin(Skin value, long endTick) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, value, endTick, tendon, bone, flesh);
    }

    public BodyData withTendon(Tendon value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, value, bone, flesh);
    }

    public BodyData withBone(Bone value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, value, flesh);
    }

    public BodyData withLastBilledTick(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, value, skin, skinEndTick, tendon, bone, flesh);
    }

    public BodyData withLived(long parts, long billedTick) {
        return new BodyData(physiques, extremePhysique, race, LongMath.saturatedAdd(ageParts, parts),
                LongMath.saturatedSubtract(lifespanParts, parts), lastDayIndex, tempZombieEndTick, zombieTier,
                billedTick, skin, skinEndTick, tendon, bone, flesh);
    }
}
