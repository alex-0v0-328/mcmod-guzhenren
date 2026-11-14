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
 * 一人身上的全部流派效果，按挂上先后排列。
 *
 * <ul>
 *   <li>附件 {@code path_effect_data}，运行时只由 {@link PathEffectService} 写入，同 {@link PathData} 只同步给本人；
 *       紧凑构造器丢弃无道痕[Path Marks]的效果，共用一格（{@link PathEffect#sharesSlot}）时留后者。</li>
 *   <li>存档 codec 按各效果 {@code type} id 经 {@code ModPathEffectTypes} 分派、逐个读：读失败的（含无类型认领的 id）
 *       丢弃，其余照常加载，否则 NeoForge 会跳过整个附件。</li>
 *   <li>⚠ 同步 codec 严格读同样的 id，两端须注册同样的类型；同一个 jar 保证这点。</li>
 * </ul>
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

    public PathMarks getMarks(GuPath path) {
        PathMarks total = PathMarks.NONE;
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
