package net.alex.guzhenren.gameplay.path.timed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.NotNull;

/**
 * The payload of a timed buff [限时增益]: the vanilla effect it projects and the amplifier it projects at.
 *
 * <p>Stored by the effect's registry id, so a save names it; the {@link Holder} is resolved on use. The amplifier is
 * the Gu's own tier (a crash Gu's rank, a self-reliance Gu's grade), never a count of marks.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.TimedEffects
 * @since 1.0.0
 */

public record TimedBuff(@NotNull ResourceLocation effect, int amplifier) {

    public static final MapCodec<TimedBuff> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(TimedBuff::effect),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(TimedBuff::amplifier)
    ).apply(instance, TimedBuff::new));
    public static final StreamCodec<FriendlyByteBuf, TimedBuff> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TimedBuff::effect, ByteBufCodecs.VAR_INT, TimedBuff::amplifier,
            TimedBuff::new);

    public static @NotNull TimedBuff of(@NotNull Holder<MobEffect> effect, int amplifier) {
        return new TimedBuff(effect.unwrapKey().orElseThrow().location(), amplifier);
    }

    public @NotNull Holder<MobEffect> getHolder() {
        return BuiltInRegistries.MOB_EFFECT.getHolder(effect).orElseThrow(
                () -> new IllegalStateException("Unknown mob effect " + effect));
    }

    public boolean is(@NotNull Holder<MobEffect> other) { return other.is(effect); }
}
