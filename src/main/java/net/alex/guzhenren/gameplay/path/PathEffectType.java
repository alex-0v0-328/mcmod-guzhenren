package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What kind of {@link PathEffect} an instance is: its save id, its payload and how that payload is written, whether
 * it helps or harms ({@link PathEffectCategory}), whether one person may carry two, the icon it projects and the
 * name it shows.
 *
 * <p>Every type is registered once in {@code registry.effect.ModPathEffectTypes}, the closed list the save and sync
 * codecs look ids up in; a family adds its type there and its payload record beside its own code. The payload is
 * the state only that kind needs (a shield's value, a half-zombie level, a distilling phase); a kind that needs none
 * takes {@code Unit} with {@code MapCodec.unit}. Its {@link MapCodec} sits inline in the effect's saved object, so the
 * type picks its own field names.
 *
 * <p>An {@link #isExclusive exclusive} type is one slot per person: applying a new instance drops the old one with
 * its marks, whatever its source (a grade replaced by another). Any other type keeps one instance per source, and
 * applying the same source again replaces that one.
 *
 * <p>{@link #getIcon} names the vanilla effect the heartbeat projects while an instance is on, or {@code null} for
 * none. The projection is an icon and a timer only: milk and {@code /effect clear} take the icon, the next
 * heartbeat puts it back, and nothing reads it.
 *
 * <p>⚠ {@link #getEffectCodec} is built on first use, not in the constructor: the types are constructed while
 * {@code ModPathEffectTypes} initializes, and the dispatch codec over them belongs to the same class.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffect
 * @see PathEffectService
 * @since 1.0.0
 */

public final class PathEffectType<P> {

    private static final Codec<Map<GuPath, DaoMarks>> PATHS = Codec.unboundedMap(GuPath.CODEC, DaoMarks.CODEC);
    private final ResourceLocation id;
    private final MapCodec<P> payloadCodec;
    private final StreamCodec<FriendlyByteBuf, P> payloadStreamCodec;
    private final PathEffectCategory category;
    private final boolean exclusive;
    private final @Nullable Supplier<Holder<MobEffect>> icon;
    private final Function<PathEffect<P>, Component> namer;
    private @Nullable MapCodec<PathEffect<P>> effectCodec;

    public PathEffectType(@NotNull ResourceLocation id, @NotNull MapCodec<P> payloadCodec,
                          @NotNull StreamCodec<FriendlyByteBuf, P> payloadStreamCodec,
                          @NotNull PathEffectCategory category, boolean exclusive,
                          @Nullable Supplier<Holder<MobEffect>> icon,
                          @NotNull Function<PathEffect<P>, Component> namer) {
        this.id = id;
        this.payloadCodec = payloadCodec;
        this.payloadStreamCodec = payloadStreamCodec;
        this.category = category;
        this.exclusive = exclusive;
        this.icon = icon;
        this.namer = namer;
    }

    public @NotNull ResourceLocation getId() { return id; }

    public @NotNull PathEffectCategory getCategory() { return category; }

    public boolean isExclusive() { return exclusive; }

    public @Nullable Holder<MobEffect> getIcon() { return icon == null ? null : icon.get(); }

    public @NotNull Component getName(@NotNull PathEffect<P> effect) { return namer.apply(effect); }

    public @NotNull MapCodec<PathEffect<P>> getEffectCodec() {
        if (effectCodec == null) effectCodec = buildEffectCodec();
        return effectCodec;
    }

    public void encode(@NotNull FriendlyByteBuf buf, @NotNull PathEffect<P> effect) {
        ModByteBufs.writeEnum(buf, effect.carrier());
        buf.writeResourceLocation(effect.source());
        ModByteBufs.writeEnumMap(buf, effect.paths(), DaoMarks::encode);
        buf.writeVarLong(effect.endTick());
        buf.writeVarInt(effect.layers());
        payloadStreamCodec.encode(buf, effect.payload());
    }

    public @NotNull PathEffect<P> decode(@NotNull FriendlyByteBuf buf) {
        return new PathEffect<>(this, ModByteBufs.readEnum(buf, EffectCarrier.class), buf.readResourceLocation(),
                ModByteBufs.readEnumMap(buf, GuPath.class, DaoMarks::decode), buf.readVarLong(),
                buf.readVarInt(), payloadStreamCodec.decode(buf));
    }

    private MapCodec<PathEffect<P>> buildEffectCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                EffectCarrier.CODEC.fieldOf("carrier").forGetter(PathEffect::carrier),
                ResourceLocation.CODEC.fieldOf("source").forGetter(PathEffect::source),
                PATHS.optionalFieldOf("paths", Map.of()).forGetter(PathEffect::paths),
                Codec.LONG.optionalFieldOf("end_tick", PathEffect.PERMANENT).forGetter(PathEffect::endTick),
                Codec.INT.optionalFieldOf("layers", 1).forGetter(PathEffect::layers),
                payloadCodec.forGetter(PathEffect::payload)
        ).apply(instance, (carrier, source, paths, endTick, layers, payload) ->
                new PathEffect<>(this, carrier, source, paths, endTick, layers, payload)));
    }

    @Override
    public String toString() { return id.toString(); }
}
