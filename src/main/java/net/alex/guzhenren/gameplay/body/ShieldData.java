package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 肉身护盾：每种 {@link ShieldType} 一层，新者在前即消耗顺序；不可变 record 附件 {@code shield_data}，仅
 * {@code ShieldService} 写。{@link #raise} 置顶并丢同种旧层，不叠加；超时层留到心跳清除，合计与 {@link #isUp} 跳过它；
 * 标记效果仅为投影。
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldService
 * @since 1.0.0
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
