package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 肉身分层护盾数据附件（键 {@code shield_data}）：每种 {@link ShieldType} 最多占一层，按升起时间由新到旧排列即受击扣减
 * 顺序。
 * <ul>
 *   <li>运行时仅由 {@link ShieldService} 写入；{@link #raise} 将新护盾层置于列表首位并替换同种旧层（同种不叠加）。</li>
 *   <li>已到期的护盾层留待心跳统一清理，{@link #isUp}、{@link #getPoints} 与 {@link #getMax} 均实时跳过过期层；药水标记
 *       效果仅作状态栏投影。</li>
 * </ul>
 */

public record ShieldData(@NotNull List<ShieldLayer> layers) {

    public static final ShieldData DEFAULT = new ShieldData(List.of());

    public static final Codec<ShieldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ShieldLayer.CODEC.listOf().optionalFieldOf("layers", List.of()).forGetter(ShieldData::layers)
    ).apply(instance, ShieldData::new));

    public ShieldData { layers = List.copyOf(layers); }

    public static @NotNull ShieldData decode(@NotNull FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ShieldLayer> layers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) layers.add(ShieldLayer.decode(buf));
        return new ShieldData(layers);
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull ShieldData value) {
        buf.writeVarInt(value.layers().size());
        for (ShieldLayer layer : value.layers()) ShieldLayer.encode(buf, layer);
    }

    public @Nullable ShieldLayer find(@NotNull ShieldType type) {
        for (ShieldLayer layer : layers) {
            if (layer.type() == type) return layer;
        }
        return null;
    }

    public boolean isUp(long now) {
        for (ShieldLayer layer : layers) {
            if (layer.isUp(now)) return true;
        }
        return false;
    }

    public float getPoints(long now) {
        float points = 0.0F;
        for (ShieldLayer layer : layers) {
            if (layer.isUp(now)) points += layer.points();
        }
        return points;
    }

    public float getMax(long now) {
        float max = 0.0F;
        for (ShieldLayer layer : layers) {
            if (layer.isUp(now)) max += layer.max();
        }
        return max;
    }

    public ShieldData raise(@NotNull ShieldLayer layer) {
        List<ShieldLayer> next = new ArrayList<>(layers.size() + 1);
        next.add(layer);
        for (ShieldLayer old : layers) {
            if (old.type() != layer.type()) next.add(old);
        }
        return new ShieldData(next);
    }

    public ShieldData without(@NotNull ShieldType type) {
        return new ShieldData(layers.stream().filter(layer -> layer.type() != type).toList());
    }

    public ShieldData onlyUp(long now) {
        return new ShieldData(layers.stream().filter(layer -> layer.isUp(now)).toList());
    }
}
