package net.alex.guzhenren.registry.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectCategory;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.BigStrength;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.SelfRelianceHeal;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 全部流派效果类型（{@link PathEffectType}）常量表以及按 ID 解析的 {@link #CODEC} 与 {@link #EFFECT_CODEC}。
 * <ul>
 *   <li>采用本模组封闭静态列表而非 NeoForge 注册表：由本类静态常量统一驱动存档与网络编解码器，不开放给外部模组扩展；家
 *       族共用的工厂 {@link #registerNamed}、{@link #registerUnit} 与 {@link #registerTimedBuff} 分别生成单枚举载荷、无
 *       载荷单位与限时增益形状，所有载荷类型均保留在本包或所属领域。</li>
 *   <li>⚠ {@link #BY_ID} 映射必须在全部类型常量之前声明（类初始化时按字段顺序注册），且每个注册 ID 即存档键名，任何改名
 *       都会导致旧存档中的对应流派效果全部丢失。</li>
 *   <li>先天与炼体永久增益：{@link #RACE_TALENT}（变异种族赋予对应流派 10 条整道痕[Path Marks]）、
 *       {@link #EXTREME_PHYSIQUE}（十绝体质向各天赋流派分摊 1000 条整道痕）、{@link #BODY_SKIN} / {@link #BODY_TENDON}
 *       / {@link #BODY_BONE}（炼体皮筋骨互斥增益，新等级直接覆盖旧等级）与 {@link #STONE_APERTURE}（空窍石化永久增益，
 *       锁定态存于 {@code Aperture}）。</li>
 *   <li>力道与限时肉身效果：{@link #BEAST_STRENGTH}（每兽族一项永久兽力[Beast Strength]）、{@link #HUMAN_STRENGTH}（每
 *       单位一项永久人力，按层累加）、{@link #BUFF}（各类限时增益投影，承载兽力、冲撞与全
 *       力以赴蛊[All-Out Effort Gu]）、{@link #SELF_RELIANCE} / {@link #HARDSHIP_STRENGTH} / {@link #BIG_STRENGTH}（对
 *       应互斥力道状态）、{@link #TIMED_SKIN} / {@link #MUGWORT_SHIELD} / {@link #VITALITY_LEAF}（限时皮盾与治疗）、
 *       {@link #WATCH_RATE}（更蛊时间倍率，每催动叠一层）、{@link #ZOMBIE}（临时僵尸减益，永久尸化时消散）与
 *       {@link #LIQUOR_DISTILLING}（酒虫[Liquor Worm]精炼 1 日增益）。</li>
 *   <li>脑海限时状态：{@link #CASUAL_THOUGHT}（随意念增益）与 {@link #MALICIOUS_THOUGHT}（恶念减益），载荷结构同
 *       {@link #BUFF}。</li>
 * </ul>
 */

public final class ModPathEffectTypes {

    private ModPathEffectTypes() {}

    private static final Map<ResourceLocation, PathEffectType<?>> BY_ID = new LinkedHashMap<>();
    //region Born with [天生]
    public static final PathEffectType<Race> RACE_TALENT =
            registerNamed("race_talent", "race", Race.class, Race.CODEC, true);
    public static final PathEffectType<ExtremePhysique> EXTREME_PHYSIQUE =
            registerNamed("extreme_physique", "physique", ExtremePhysique.class, ExtremePhysique.CODEC, true);
    //endregion

    //region Body tempering [炼体]
    public static final PathEffectType<Skin> BODY_SKIN =
            registerNamed("body_skin", "grade", Skin.class, Skin.CODEC, true);
    public static final PathEffectType<Tendon> BODY_TENDON =
            registerNamed("body_tendon", "grade", Tendon.class, Tendon.CODEC, true);
    public static final PathEffectType<Bone> BODY_BONE =
            registerNamed("body_bone", "grade", Bone.class, Bone.CODEC, true);
    //endregion

    //region Strength Path [力道]
    public static final PathEffectType<BeastStrength> BEAST_STRENGTH =
            registerNamed("beast_strength", "beast", BeastStrength.class, BeastStrength.CODEC, false);
    public static final PathEffectType<HumanStrength> HUMAN_STRENGTH = register(new PathEffectType<>(
            Guzhenren.id("human_strength"), HumanStrength.CODEC.fieldOf("kind"),
            ModByteBufs.enumCodec(HumanStrength.class), PathEffectCategory.BENEFICIAL, false, null,
            effect -> Component.translatable("guzhenren.path_effect.human_strength",
                    Component.translatable(effect.payload().getTranslationKey()), effect.layers())));
    //endregion

    //region Timed body effects [限时肉体效果]
    public static final PathEffectType<TimedBuff> BUFF = registerTimedBuff("timed_buff", PathEffectCategory.BENEFICIAL);
    public static final PathEffectType<SelfRelianceHeal> SELF_RELIANCE = register(PathEffectType.projecting(
            Guzhenren.id("self_reliance"), SelfRelianceHeal.CODEC, SelfRelianceHeal.STREAM_CODEC,
            PathEffectCategory.BENEFICIAL, true,
            effect -> new PathEffectType.Projection(ModEffects.SELF_RELIANCE_GU, effect.payload().grade()),
            effect -> Component.translatable("guzhenren.path_effect.timed_buff",
                    ModEffects.SELF_RELIANCE_GU.value().getDisplayName())));
    public static final PathEffectType<HardshipStrength.State> HARDSHIP_STRENGTH = register(PathEffectType.projecting(
            Guzhenren.id("hardship_strength"), HardshipStrength.State.CODEC, HardshipStrength.State.STREAM_CODEC,
            PathEffectCategory.BENEFICIAL, true,
            effect -> new PathEffectType.Projection(ModEffects.HARDSHIP_STRENGTH_GU, 0),
            effect -> Component.translatable("guzhenren.path_effect.timed_buff",
                    ModEffects.HARDSHIP_STRENGTH_GU.value().getDisplayName())));
    public static final PathEffectType<BigStrength.State> BIG_STRENGTH = register(PathEffectType.projecting(
            Guzhenren.id("big_strength"), BigStrength.State.CODEC, BigStrength.State.STREAM_CODEC,
            PathEffectCategory.BENEFICIAL, true,
            effect -> new PathEffectType.Projection(ModEffects.BIG_STRENGTH_GU, 0),
            effect -> Component.translatable("guzhenren.path_effect.timed_buff",
                    ModEffects.BIG_STRENGTH_GU.value().getDisplayName())));
    public static final PathEffectType<Skin> TIMED_SKIN =
            registerNamed("timed_skin", "skin", Skin.class, Skin.CODEC, true);
    public static final PathEffectType<Unit> MUGWORT_SHIELD =
            registerUnit("mugwort_shield", PathEffectCategory.BENEFICIAL, false, null);
    public static final PathEffectType<Unit> VITALITY_LEAF =
            registerUnit("vitality_leaf", PathEffectCategory.BENEFICIAL, false, () -> ModEffects.VITALITY_LEAF_GU);
    public static final PathEffectType<ResourceLocation> WATCH_RATE = register(PathEffectType.projecting(
            Guzhenren.id("watch_rate"), ResourceLocation.CODEC.fieldOf("form"), ResourceLocation.STREAM_CODEC.mapStream(
                    buf -> buf), PathEffectCategory.BENEFICIAL, false,
            effect -> new PathEffectType.Projection(BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(
                    ResourceKey.create(Registries.MOB_EFFECT, effect.payload())), effect.layers() - 1),
            effect -> Component.translatable("guzhenren.path_effect.watch_rate", effect.layers())));
    //endregion

    //region Transformation Path [变化道]
    public static final PathEffectType<Unit> ZOMBIE = register(new PathEffectType<>(Guzhenren.id("zombie"),
            MapCodec.unit(Unit.INSTANCE), StreamCodec.unit(Unit.INSTANCE), PathEffectCategory.HARMFUL, true, null,
            effect -> Component.translatable(effect.isPermanent() ? "guzhenren.path_effect.zombie"
                    : "guzhenren.path_effect.temp_zombie")));
    //endregion

    //region Food Path [食道]
    public static final PathEffectType<Integer> LIQUOR_DISTILLING = register(PathEffectType.projecting(
            Guzhenren.id("liquor_distilling"), Codec.INT.fieldOf("tier"), ByteBufCodecs.VAR_INT.mapStream(buf -> buf),
            PathEffectCategory.BENEFICIAL, true,
            effect -> new PathEffectType.Projection(ModEffects.LIQUOR_WORM, effect.payload()),
            effect -> Component.translatable("guzhenren.path_effect.liquor_distilling")));
    //endregion

    //region Aperture [空窍]
    public static final PathEffectType<Unit> STONE_APERTURE =
            registerUnit("stone_aperture", PathEffectCategory.BENEFICIAL, true, null);
    //endregion

    //region Mind [脑海]
    public static final PathEffectType<TimedBuff> CASUAL_THOUGHT =
            registerTimedBuff("casual_thought", PathEffectCategory.BENEFICIAL);
    public static final PathEffectType<TimedBuff> MALICIOUS_THOUGHT =
            registerTimedBuff("malicious_thought", PathEffectCategory.HARMFUL);
    //endregion

    public static final Codec<PathEffectType<?>> CODEC = ResourceLocation.CODEC.comapFlatMap(
            ModPathEffectTypes::lookup, PathEffectType::getId);
    public static final Codec<PathEffect<?>> EFFECT_CODEC = CODEC.dispatch(
            "type", PathEffect::type, PathEffectType::getEffectCodec);

    public static @Nullable PathEffectType<?> get(@NotNull ResourceLocation id) { return BY_ID.get(id); }

    public static @NotNull Collection<PathEffectType<?>> getAll() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    private static <E extends Enum<E> & NamedEnum> PathEffectType<E> registerNamed(
            String id, String field, Class<E> type, Codec<E> codec, boolean exclusive) {
        return register(new PathEffectType<>(Guzhenren.id(id), codec.fieldOf(field), ModByteBufs.enumCodec(type),
                PathEffectCategory.BENEFICIAL, exclusive, null,
                effect -> Component.translatable("guzhenren.path_effect." + id,
                        Component.translatable(effect.payload().getTranslationKey()))));
    }

    private static PathEffectType<Unit> registerUnit(String id, PathEffectCategory category, boolean exclusive,
                                                     @Nullable Supplier<Holder<MobEffect>> icon) {
        return register(new PathEffectType<>(Guzhenren.id(id), MapCodec.unit(Unit.INSTANCE),
                StreamCodec.unit(Unit.INSTANCE), category, exclusive, icon,
                effect -> Component.translatable("guzhenren.path_effect." + id)));
    }

    private static PathEffectType<TimedBuff> registerTimedBuff(String id, PathEffectCategory category) {
        return register(PathEffectType.projecting(Guzhenren.id(id), TimedBuff.CODEC, TimedBuff.STREAM_CODEC, category,
                false, effect -> new PathEffectType.Projection(effect.payload().getHolder(),
                        effect.payload().amplifier()),
                effect -> Component.translatable("guzhenren.path_effect.timed_buff",
                        effect.payload().getHolder().value().getDisplayName())));
    }

    private static <P> PathEffectType<P> register(PathEffectType<P> type) {
        if (BY_ID.putIfAbsent(type.getId(), type) != null) {
            throw new IllegalStateException("Path effect type " + type.getId() + " registered twice");
        }
        return type;
    }

    private static DataResult<PathEffectType<?>> lookup(ResourceLocation id) {
        PathEffectType<?> type = BY_ID.get(id);
        return type == null ? DataResult.error(() -> "Unknown path effect type " + id) : DataResult.success(type);
    }
}
