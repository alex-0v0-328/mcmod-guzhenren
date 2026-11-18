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
 * 玩家身上的全部流派效果列表附件（键 {@code path_effect_data}，按挂载先后顺序排列，仅同步给玩家本人）。
 * <ul>
 *   <li>运行时仅由 {@link PathEffectService} 写入；紧凑构造器自动丢弃无道痕[Path Marks]的空效果，并在发生同槽冲突
 *       （{@link PathEffect#sharesSlot}）时保留后加入的效果。</li>
 *   <li>存档反序列化按各条目的类型 ID 经 {@code ModPathEffectTypes} 逐条解析：单条解析失败（含已移除的未知类型 ID）时仅
 *       丢弃该条，确保其余流派效果正常加载而不会让 NeoForge 丢弃整个附件。</li>
 *   <li>⚠ 网络同步编解码严格按注册表 ID 读写，要求客户端与服务端运行相同版本的类型注册表。</li>
 * </ul>
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
