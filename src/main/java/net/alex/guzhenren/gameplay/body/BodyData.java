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
 * 玩家肉身状态附件（键 {@code body_data}）：承载累积体质、十绝体[Ten Extreme Physiques]、种族、筋骨皮肉档位、肢体与七窍
 * （{@link Anatomy}）以及年龄与寿元。
 * <ul>
 *   <li>运行时仅由 {@link BodyService} 写入；具体十绝体枚举存于此处，空窍仅存储代表十绝天资的满值基础真元。</li>
 *   <li>⚠ 15 个组件使用手写 {@link #encode} 与 {@link #decode} 同步，两者的字段读写顺序必须严格一致。</li>
 *   <li>⚠ 所有时间戳锚点未启用时统一填 {@link #UNTRACKED}（{@code -1}），切勿用 {@code 0}（{@code 0} 是新世界合法游戏
 *       时间）。</li>
 *   <li>⚠ 外部调用方一律以「年」为单位传参，细分的「份」（{@code parts}，每年 {@link #PARTS_PER_YEAR} 份）仅在本类与
 *       {@link BodyService} 内部换算。</li>
 *   <li>僵尸（{@link Physique#ZOMBIE}）与临时僵尸化（{@link Physique#TEMP_ZOMBIE}）互斥，十绝体可与任一并存；紧凑构造器
 *       在同时存在时自动移除 {@code TEMP_ZOMBIE}（因此 {@code with(ZOMBIE)} 无需先手动摘除临时僵尸，而
 *       {@code with(TEMP_ZOMBIE)} 需先 {@code without(ZOMBIE)}），当两者均无时自动将 {@code zombieTier} 重置为
 *       {@link #NO_ZOMBIE_TIER}。</li>
 *   <li>皮肤占一槽，限时皮肤（玉皮、白玉皮）带 {@code skinEndTick} 到期时刻（{@code JADE_SKIN_GU} /
 *       {@code WHITE_JADE_GU} 药水效果仅作其状态栏图标投影），由 {@link SkinService} 在心跳中检查脱下；铜皮、铁皮及筋、
 *       骨、肉为永久改造档位，死亡重生后保留。</li>
 *   <li>{@link #withRevived} 用于复活或脱僵：一并移除僵尸与临时僵尸化、脱下限时皮肤、清空肢体与七窍伤势，但保留永久炼体
 *       档位与附加部位。</li>
 * </ul>
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
        Flesh flesh,
        Anatomy anatomy
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
            Skin.NATIVE, UNTRACKED, Tendon.NATIVE, Bone.NATIVE, Flesh.NATIVE, Anatomy.DEFAULT);

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
            Flesh.CODEC.optionalFieldOf("flesh", Flesh.NATIVE).forGetter(BodyData::flesh),
            Anatomy.CODEC.optionalFieldOf("anatomy", Anatomy.DEFAULT).forGetter(BodyData::anatomy)
    ).apply(instance, BodyData::new));
    public static @NotNull BodyData decode(@NotNull FriendlyByteBuf buf) {
        return new BodyData(ModByteBufs.readEnumSet(buf, Physique.class), ModByteBufs.readEnum(buf,
                ExtremePhysique.class), ModByteBufs.readEnum(buf, Race.class), buf.readVarLong(), buf.readVarLong(),
                buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readVarLong(), ModByteBufs.readEnum(buf,
                Skin.class), buf.readVarLong(), ModByteBufs.readEnum(buf, Tendon.class), ModByteBufs.readEnum(buf,
                Bone.class), ModByteBufs.readEnum(buf, Flesh.class), Anatomy.decode(buf));
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
        Anatomy.encode(buf, value.anatomy());
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
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh, anatomy);
    }

    public BodyData withExtremePhysique(ExtremePhysique physique) {
        return new BodyData(physiques, physique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh, anatomy);
    }

    public BodyData withRevived() {
        return without(Physique.ZOMBIE).without(Physique.TEMP_ZOMBIE).withTempZombieEndTick(UNTRACKED)
                .withSkin(skin.isTimed() ? Skin.NATIVE : skin, UNTRACKED).withAnatomy(anatomy.withoutInjuries());
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
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh, anatomy);
    }

    public BodyData withAgeParts(long value) {
        return new BodyData(physiques, extremePhysique, race, value, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh, anatomy);
    }

    public BodyData withLifespanParts(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, value, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh, anatomy);
    }

    public BodyData withLastDayIndex(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, value, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh, anatomy);
    }

    public BodyData withTempZombieEndTick(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, value, zombieTier,
                lastBilledTick, skin, skinEndTick, tendon, bone, flesh, anatomy);
    }

    public BodyData withZombieTier(int value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                value, lastBilledTick, skin, skinEndTick, tendon, bone, flesh, anatomy);
    }

    public BodyData withSkin(Skin value, long endTick) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, value, endTick, tendon, bone, flesh, anatomy);
    }

    public BodyData withTendon(Tendon value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, value, bone, flesh, anatomy);
    }

    public BodyData withBone(Bone value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, value, flesh, anatomy);
    }

    public BodyData withAnatomy(Anatomy value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, lastBilledTick, skin, skinEndTick, tendon, bone, flesh, value);
    }

    public BodyData withLastBilledTick(long value) {
        return new BodyData(physiques, extremePhysique, race, ageParts, lifespanParts, lastDayIndex, tempZombieEndTick,
                zombieTier, value, skin, skinEndTick, tendon, bone, flesh, anatomy);
    }

    public BodyData withLived(long parts, long billedTick) {
        return new BodyData(physiques, extremePhysique, race, LongMath.saturatedAdd(ageParts, parts),
                LongMath.saturatedSubtract(lifespanParts, parts), lastDayIndex, tempZombieEndTick, zombieTier,
                billedTick, skin, skinEndTick, tendon, bone, flesh, anatomy);
    }
}
