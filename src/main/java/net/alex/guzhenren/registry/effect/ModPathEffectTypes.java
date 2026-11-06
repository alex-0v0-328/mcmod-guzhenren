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
 * Every path effect type [流派效果类型] this mod knows, one constant per kind, and the codec that finds a type by id.
 *
 * <p>A closed list, not a NeoForge registry: the save and sync codecs resolve a {@code type} id here, and no sibling
 * mod adds a type. A family adds its constant with {@link #register} and keeps its payload record in its own
 * package; the list is initialized on first use, so a codec reading an id always finds every type.
 *
 * <ul>
 * <li>{@link #RACE_TALENT} [种族天赋] -- a buff on the body, payload the race; the variant race's ten whole marks
 * on its path.</li>
 * <li>{@link #EXTREME_PHYSIQUE} [十绝体质] -- a buff on the aperture, payload the physique; its thousand whole marks
 * split across its talent paths.</li>
 * <li>{@link #BODY_SKIN}, {@link #BODY_TENDON}, {@link #BODY_BONE} [炼体·皮 / 筋 / 骨] -- a permanent buff on the
 * body, payload the grade; exclusive, so a new grade takes the old one and its strands.</li>
 * <li>{@link #BEAST_STRENGTH} [兽力] -- a permanent buff on the body per species, payload the species.</li>
 * <li>{@link #HUMAN_STRENGTH} [人力] -- a permanent buff on the body per kind, one layer per stored layer.</li>
 * <li>{@link #BUFF} [限时增益] -- a timed buff on the body per Gu, payload the vanilla effect and its amplifier;
 * the beast buffs, the crashes and all-out effort.</li>
 * <li>{@link #SELF_RELIANCE} [自力更生] -- an auto heal on the body, payload the grade. Permanent until its auto
 * use stops it; exclusive.</li>
 * <li>{@link #HARDSHIP_STRENGTH} [苦力] -- an auto state on the body, its payload a strength amplifier provider.
 * Permanent until its auto use stops it; exclusive.</li>
 * <li>{@link #TIMED_SKIN} [限时皮] -- the jade skin or white jade skin on the body, payload the skin; exclusive. The
 * skin itself stays on {@code BodyData}, which projects its own icon.</li>
 * <li>{@link #MUGWORT_SHIELD} [天蓬护盾] -- the shield on the body; its points stay on {@code ShieldData}.</li>
 * <li>{@link #VITALITY_LEAF} [生机叶] -- a timed heal on the body.</li>
 * <li>{@link #WATCH_RATE} [更蛊] -- a timed time rate on the body per Gu, payload the vanilla form; one layer per
 * use.</li>
 * <li>{@link #ZOMBIE} [僵尸] -- a harmful timed state on a temp zombie's body; turning zombie for good dissipates
 * it, and the form itself stays on {@code BodyData}. Exclusive.</li>
 * <li>{@link #LIQUOR_DISTILLING} [酒虫精炼] -- a timed buff on the body while true essence is distilled, payload the
 * Gu's tier; the day it lasts is the distilling's. Exclusive.</li>
 * <li>{@link #STONE_APERTURE} [石窍] -- a permanent buff on the aperture while one is petrified; the latch itself
 * stays on {@code Aperture}. Exclusive.</li>
 * <li>{@link #CASUAL_THOUGHT}, {@link #MALICIOUS_THOUGHT} [随意念 / 恶念] -- a timed state on the mind per Gu, payload
 * the vanilla effect and its amplifier.</li>
 * </ul>
 *
 * <p>{@code registerNamed} builds a beneficial type carrying one enum constant in {@code field}, named by its key
 * around that constant's name; {@code registerUnit} and {@code registerTimedBuff} build the payload-free and the
 * timed-buff shapes.
 *
 * <p>⚠ The ids are save keys: renaming one drops every saved effect of that type. ⚠ {@code BY_ID} is declared
 * before the constants, since each constant registers into it during class initialization.
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
