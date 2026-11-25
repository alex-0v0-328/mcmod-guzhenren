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
 * 流派效果（{@link PathEffect}）的类型定义：声明注册 ID、{@code payload} 编解码器、增益/减益分类
 * （{@link PathEffectCategory}）、槽位独占规则、状态栏投影图标与显示名称。
 * <ul>
 *   <li>所有类型统一在 {@code registry.effect.ModPathEffectTypes} 中注册，构成存档与网络同步按 ID 分派的封闭类型表。
 *       </li>
 *   <li>{@code payload} 承载该类型特有的附加状态（无附加状态时用 {@code Unit} 搭配 {@code MapCodec.unit}），其
 *       {@link MapCodec} 内联在流派效果的序列化对象中。</li>
 *   <li>独占类型（{@link #isExclusive} 为 {@code true}）在每名玩家身上最多存在一个实例，挂载新实例时不论来源直接替换旧
 *       实例；非独占类型按 {@code (type, source)} 共用槽位，仅替换同来源的旧实例。</li>
 *   <li>{@link #getIcon} 或动态投影器（{@link #project} 返回 {@link Projection}）指定效果在身时向原版状态栏投影的药水图
 *       标与等级（{@code null} 表示不投影）；投影仅供 HUD 展示，业务规则只读 {@link PathEffect}。</li>
 *   <li>⚠ {@link #getEffectCodec} 采用延迟初始化：因为类型常量在 {@code ModPathEffectTypes} 类加载期构造，分派 Codec
 *       也在同一类中初始化。</li>
 * </ul>
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

    /** 状态栏投影目标的原版药水效果及其放大器等级（持续时间取自流派效果实例自身）。 */
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
