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
 * 限时增益 payload：投射的原版效果及等级。按效果注册 id 存储，存档写名字，{@link Holder} 用时解析；等级为蛊自身
 * tier（横冲系蛊的转数），不是道痕[Path Marks]数。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects
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
