package net.alex.guzhenren.registry.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModByteBufs;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Race;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectCategory;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * </ul>
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
    public static final PathEffectType<Race> RACE_TALENT = register(new PathEffectType<>(Guzhenren.id("race_talent"),
            Race.CODEC.fieldOf("race"), ModByteBufs.enumCodec(Race.class), PathEffectCategory.BENEFICIAL, true, null,
            effect -> Component.translatable("guzhenren.path_effect.race_talent",
                    Component.translatable(effect.payload().getTranslationKey()))));
    public static final PathEffectType<ExtremePhysique> EXTREME_PHYSIQUE = register(new PathEffectType<>(
            Guzhenren.id("extreme_physique"), ExtremePhysique.CODEC.fieldOf("physique"),
            ModByteBufs.enumCodec(ExtremePhysique.class), PathEffectCategory.BENEFICIAL, true, null,
            effect -> Component.translatable("guzhenren.path_effect.extreme_physique",
                    Component.translatable(effect.payload().getTranslationKey()))));
    public static final Codec<PathEffectType<?>> CODEC = ResourceLocation.CODEC.comapFlatMap(
            ModPathEffectTypes::lookup, PathEffectType::getId);
    public static final Codec<PathEffect<?>> EFFECT_CODEC = CODEC.dispatch(
            "type", effect -> effect.type(), type -> type.getEffectCodec());

    public static @Nullable PathEffectType<?> get(@NotNull ResourceLocation id) { return BY_ID.get(id); }

    public static @NotNull Collection<PathEffectType<?>> getAll() {
        return Collections.unmodifiableCollection(BY_ID.values());
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
