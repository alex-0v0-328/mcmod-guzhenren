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
 * {@link PathEffect} 的类型。
 *
 * <ul>
 *   <li>定存档 id、payload 及其编解码、有益或有害（{@link PathEffectCategory}）、是否独占、图标与显示名。</li>
 *   <li>每个类型只在 {@code registry.effect.ModPathEffectTypes} 登记一次，即存档与同步 codec 查 id 的闭合列表；
 *       效果族在那里加类型，payload record 放自己代码旁。</li>
 *   <li>payload 是该类型独需的状态（护盾值、临时僵尸化等级、精炼真元阶段），无则取 {@code Unit} 配
 *       {@code MapCodec.unit}；其 {@link MapCodec} 内联在效果存档对象里，字段名由类型自定。</li>
 *   <li>{@link #isExclusive 独占} 类型每人一格：挂新实例即连道痕[Path Marks]丢弃旧实例，不论来源（如一档换另一档）；
 *       其他类型每来源一个实例，同来源再挂则替换。</li>
 *   <li>{@link #getIcon}：实例在身时心跳投影的原版效果，{@code null} 为无；图标随实例变的类型（兽种增益、按层等级）改用
 *       投影器返回 {@link Projection}。投影即图标、计时与等级：{@code /effect clear} 清后下次心跳放回，规则不读。</li>
 *   <li>⚠ {@link #getEffectCodec} 首用才建：类型构造于 {@code ModPathEffectTypes} 初始化中，分派 codec 也在该类。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffect
 * @see PathEffectService
 * @since 1.0.0
 */

public final class PathEffectType<P> {

    private static final Codec<Map<GuPath, PathMarks>> PATHS = Codec.unboundedMap(GuPath.CODEC, PathMarks.CODEC);
    private final ResourceLocation id;
    private final MapCodec<P> payloadCodec;
    private final StreamCodec<FriendlyByteBuf, P> payloadStreamCodec;
    private final PathEffectCategory category;
    private final boolean exclusive;
    private final @Nullable Supplier<Holder<MobEffect>> icon;
    private final @Nullable Function<PathEffect<P>, Projection> projector;
    private final Function<PathEffect<P>, Component> namer;
    private @Nullable MapCodec<PathEffect<P>> effectCodec;

    public PathEffectType(@NotNull ResourceLocation id, @NotNull MapCodec<P> payloadCodec,
                          @NotNull StreamCodec<FriendlyByteBuf, P> payloadStreamCodec,
                          @NotNull PathEffectCategory category, boolean exclusive,
                          @Nullable Supplier<Holder<MobEffect>> icon,
                          @NotNull Function<PathEffect<P>, Component> namer) {
        this(id, payloadCodec, payloadStreamCodec, category, exclusive, icon,
                icon == null ? null : effect -> new Projection(icon.get(), 0), namer);
    }

    public static <P> @NotNull PathEffectType<P> projecting(
            @NotNull ResourceLocation id, @NotNull MapCodec<P> payloadCodec,
            @NotNull StreamCodec<FriendlyByteBuf, P> payloadStreamCodec, @NotNull PathEffectCategory category,
            boolean exclusive, @NotNull Function<PathEffect<P>, Projection> projector,
            @NotNull Function<PathEffect<P>, Component> namer) {
        return new PathEffectType<>(id, payloadCodec, payloadStreamCodec, category, exclusive, null, projector, namer);
    }

    private PathEffectType(ResourceLocation id, MapCodec<P> payloadCodec,
                           StreamCodec<FriendlyByteBuf, P> payloadStreamCodec, PathEffectCategory category,
                           boolean exclusive, @Nullable Supplier<Holder<MobEffect>> icon,
                           @Nullable Function<PathEffect<P>, Projection> projector,
                           Function<PathEffect<P>, Component> namer) {
        this.id = id;
        this.payloadCodec = payloadCodec;
        this.payloadStreamCodec = payloadStreamCodec;
        this.category = category;
        this.exclusive = exclusive;
        this.icon = icon;
        this.projector = projector;
        this.namer = namer;
    }

    public @NotNull ResourceLocation getId() { return id; }

    public @NotNull PathEffectCategory getCategory() { return category; }

    public boolean isExclusive() { return exclusive; }

    public @Nullable Holder<MobEffect> getIcon() { return icon == null ? null : icon.get(); }

    public boolean projects() { return projector != null; }

    public @Nullable Projection project(@NotNull PathEffect<P> effect) {
        return projector == null ? null : projector.apply(effect);
    }

    /** 实例显示的原版效果及其等级；计时取实例自己的。 */
    public record Projection(@NotNull Holder<MobEffect> effect, int amplifier) {}

    public @NotNull Component getName(@NotNull PathEffect<P> effect) { return namer.apply(effect); }

    public @NotNull MapCodec<PathEffect<P>> getEffectCodec() {
        if (effectCodec == null) effectCodec = buildEffectCodec();
        return effectCodec;
    }

    public void encode(@NotNull FriendlyByteBuf buf, @NotNull PathEffect<P> effect) {
        ModByteBufs.writeEnum(buf, effect.carrier());
        buf.writeResourceLocation(effect.source());
        ModByteBufs.writeEnumMap(buf, effect.paths(), PathMarks::encode);
        buf.writeVarLong(effect.endTick());
        buf.writeVarInt(effect.layers());
        payloadStreamCodec.encode(buf, effect.payload());
    }

    public @NotNull PathEffect<P> decode(@NotNull FriendlyByteBuf buf) {
        return new PathEffect<>(this, ModByteBufs.readEnum(buf, EffectCarrier.class), buf.readResourceLocation(),
                ModByteBufs.readEnumMap(buf, GuPath.class, PathMarks::decode), buf.readVarLong(),
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
