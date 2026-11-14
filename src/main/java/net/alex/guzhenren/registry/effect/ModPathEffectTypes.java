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
 * 全部流派效果类型常量，及按 ID 查类型的编解码器。
 *
 * <ul>
 *   <li>封闭列表，非 NeoForge 注册表：存档、同步编解码器在此解析 {@code type} ID，兄弟模组不加类型；家族用
 *       {@link #register} 加常量，payload 记录留本包；首用即初始化，读 ID 必能找到全部类型。</li>
 *   <li>{@code registerNamed} 建单枚举常量（存于 {@code field}）的增益类型，键随常量名；{@code registerUnit}、
 *       {@code registerTimedBuff} 建无 payload、限时增益形状。</li>
 *   <li>⚠ ID 即存档键，改名丢该类型全部存值；{@code BY_ID} 须在常量前声明，因常量类初始化时注册进它。</li>
 *   <li>{@link #RACE_TALENT} 种族天赋：肉身增益，payload 种族；变异种族在其道上的 10 整道痕[Path Marks]。</li>
 *   <li>{@link #EXTREME_PHYSIQUE} 十绝体质：空窍[Aperture]增益，payload 体质；1000 整道痕分摊到各天赋道。</li>
 *   <li>{@link #BODY_SKIN}、{@link #BODY_TENDON}、{@link #BODY_BONE} 炼体皮筋骨：肉身永久增益，payload 等级；互斥，
 *       新级连丝顶旧。</li>
 *   <li>{@link #BEAST_STRENGTH} 兽力[Beast Strength]：每物种一个肉身永久增益，payload 物种。</li>
 *   <li>{@link #HUMAN_STRENGTH} 人力：每种类一个肉身永久增益，每存一层算一层。</li>
 *   <li>{@link #BUFF} 限时增益：每蛊一个肉身限时增益，payload 原版效果与 amplifier；兽类、冲撞、
 *       全力以赴蛊[All-Out Effort Gu]用。</li>
 *   <li>{@link #SELF_RELIANCE} 自力更生：肉身自动治疗，payload 等级；时长为自动催动窗口；互斥。</li>
 *   <li>{@link #HARDSHIP_STRENGTH} 苦力：肉身自动状态，payload 力量 amplifier 提供者；时长为自动催动窗口；互斥。</li>
 *   <li>{@link #TIMED_SKIN} 限时皮：肉身玉皮或白玉皮，payload 皮；互斥；皮本身在 {@code BodyData}，自投图标。</li>
 *   <li>{@link #MUGWORT_SHIELD} 天蓬护盾：肉身护盾，点数在 {@code ShieldData}。</li>
 *   <li>{@link #VITALITY_LEAF} 生机叶：肉身限时治疗。</li>
 *   <li>{@link #WATCH_RATE} 更蛊：每蛊一个肉身限时时间倍率，payload 原版效果形态；每催动一次一层。</li>
 *   <li>{@link #ZOMBIE} 僵尸：临时僵尸肉身的有害限时状态，永久尸化即散；形态在 {@code BodyData}；互斥。</li>
 *   <li>{@link #LIQUOR_DISTILLING} 酒虫精炼：精炼真元[Essence]时的肉身限时增益，payload 蛊档次；历时一天；互斥。</li>
 *   <li>{@link #STONE_APERTURE} 石窍：空窍石化时的空窍永久增益；锁存在 {@code Aperture}；互斥。</li>
 *   <li>{@link #CASUAL_THOUGHT}、{@link #MALICIOUS_THOUGHT} 随意念、恶念：每蛊一个脑海限时状态，payload 同
 *       {@link #BUFF}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectType
 * @since 1.0.0
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
