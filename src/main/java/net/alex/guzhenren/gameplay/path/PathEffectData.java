package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Every path effect [流派效果] a person carries, in the order they were put on.
 *
 * <p>Immutable record attachment keyed {@code path_effect_data}; {@link PathEffectService} is the only runtime
 * writer, and it syncs to the owner alone, like {@link PathData}. The compact constructor drops an effect with no
 * marks and keeps the later of two that share a slot ({@link PathEffect#sharesSlot}), so a store can never hold
 * an effect without marks or the same effect twice.
 *
 * <p>The save codec dispatches on each effect's {@code type} id through {@code ModPathEffectTypes} and reads each
 * saved effect on its own: one that fails, an id no type answers among them, is dropped and the rest load, since
 * NeoForge would otherwise skip the whole attachment. ⚠ The sync codec reads the same ids strictly, so a client and
 * server must register the same types, which one jar guarantees.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffect
 * @see PathEffectService
 * @since 1.0.0
 */

public record PathEffectData(List<PathEffect<?>> effects) {

    public static final PathEffectData DEFAULT = new PathEffectData(List.of());
    public static final Codec<PathEffectData> CODEC = Codec.of(
            ModPathEffectTypes.EFFECT_CODEC.listOf().comap(PathEffectData::effects),
            Codec.PASSTHROUGH.listOf().map(saved -> new PathEffectData(saved.stream()
                    .flatMap(entry -> ModPathEffectTypes.EFFECT_CODEC.parse(entry).result().stream()).toList())));

    public PathEffectData {
        List<PathEffect<?>> kept = new ArrayList<>(effects.size());
        for (PathEffect<?> effect : effects) {
            if (effect.isEmpty()) continue;
            kept.removeIf(effect::sharesSlot);
            kept.add(effect);
        }
        effects = List.copyOf(kept);
    }

    public static @NotNull PathEffectData decode(@NotNull FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<PathEffect<?>> effects = new ArrayList<>(Math.min(size, 64));
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            PathEffectType<?> type = ModPathEffectTypes.get(id);
            if (type == null) throw new DecoderException("Unknown path effect type " + id);
            effects.add(type.decode(buf));
        }
        return new PathEffectData(effects);
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull PathEffectData value) {
        buf.writeVarInt(value.effects().size());
        for (PathEffect<?> effect : value.effects()) encodeEffect(buf, effect);
    }

    private static <P> void encodeEffect(FriendlyByteBuf buf, PathEffect<P> effect) {
        buf.writeResourceLocation(effect.type().getId());
        effect.type().encode(buf, effect);
    }

    public DaoMarks getMarks(GuPath path) {
        DaoMarks total = DaoMarks.NONE;
        for (PathEffect<?> effect : effects) total = total.add(effect.getMarks(path));
        return total;
    }

    @SuppressWarnings("unchecked")
    public <P> @Nullable PathEffect<P> find(PathEffectType<P> type, @Nullable ResourceLocation source) {
        for (PathEffect<?> effect : effects) {
            if (effect.type() == type && (source == null || effect.source().equals(source))) {
                return (PathEffect<P>) effect;
            }
        }
        return null;
    }

    public PathEffectData with(PathEffect<?> effect) {
        List<PathEffect<?>> next = new ArrayList<>(effects);
        next.add(effect);
        return new PathEffectData(next);
    }

    public PathEffectData without(Predicate<PathEffect<?>> drop) {
        List<PathEffect<?>> next = new ArrayList<>(effects);
        next.removeIf(drop);
        return new PathEffectData(next);
    }
}
