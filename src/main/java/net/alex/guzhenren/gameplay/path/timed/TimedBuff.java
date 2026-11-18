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
 * 限时药水增益类流派效果的 {@code payload} 记录：保存投影的原版药水效果注册 ID 与放大器等级（按注册 ID 序列化，运行时经
 * {@link #getHolder} 解析 {@link Holder}；{@code amplifier} 表示横冲直撞等蛊虫自身的转数档位，而非道痕[Path Marks]
 * 数）。
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
