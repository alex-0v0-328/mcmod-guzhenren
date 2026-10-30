package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The shields [护盾] over the body: one layer per {@link ShieldType}, newest first, the order a hit spends them in.
 * Immutable record attachment keyed {@code shield_data}; {@code ShieldService} is the only writer.
 *
 * <p>{@link #raise} puts a layer on top and drops the older layer of its kind, so a kind never stacks. A layer whose
 * time ran out stays listed until the heartbeat clears it; the totals and {@link #isUp} skip it. The marker effects
 * are only projections.
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
