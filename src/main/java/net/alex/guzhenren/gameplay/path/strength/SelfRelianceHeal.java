package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The payload of a running Self Reliance heal [自力更生]: the Gu's grade and the Amplify Effect Gu [功倍蛊] factor
 * billed when it started.
 *
 * <p>The grade is the icon's amplifier and the gate's strength; the factor multiplies each pulse.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.path.strength.SelfRelianceModule
 * @since 1.0.0
 */

public record SelfRelianceHeal(int grade, int factor) {

    public static final MapCodec<SelfRelianceHeal> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("grade").forGetter(SelfRelianceHeal::grade),
            Codec.INT.optionalFieldOf("factor", 1).forGetter(SelfRelianceHeal::factor)
    ).apply(instance, SelfRelianceHeal::new));
    public static final StreamCodec<FriendlyByteBuf, SelfRelianceHeal> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelfRelianceHeal::grade, ByteBufCodecs.VAR_INT, SelfRelianceHeal::factor,
            SelfRelianceHeal::new);
}
