package net.alex.guzhenren.gameplay.path.timed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The payload of a running Self Reliance heal [自力更生]: the Gu's grade, the Amplify Effect Gu [功倍蛊] factor
 * billed when it started, and the share of max health healed since its last hunger point.
 *
 * <p>The grade is the icon's amplifier and the gate's strength; the factor multiplies each pulse. {@code healed}
 * restarts at 0 with every new heal, so a share short of a hunger point is dropped when the heal ends.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.strength.SelfRelianceModule
 * @since 1.0.0
 */

public record SelfRelianceHeal(int grade, int factor, float healed) {

    public static final MapCodec<SelfRelianceHeal> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("grade").forGetter(SelfRelianceHeal::grade),
            Codec.INT.optionalFieldOf("factor", 1).forGetter(SelfRelianceHeal::factor),
            Codec.FLOAT.optionalFieldOf("healed", 0.0F).forGetter(SelfRelianceHeal::healed)
    ).apply(instance, SelfRelianceHeal::new));
    public static final StreamCodec<FriendlyByteBuf, SelfRelianceHeal> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelfRelianceHeal::grade, ByteBufCodecs.VAR_INT, SelfRelianceHeal::factor,
            ByteBufCodecs.FLOAT, SelfRelianceHeal::healed, SelfRelianceHeal::new);

    public SelfRelianceHeal withHealed(float value) { return new SelfRelianceHeal(grade, factor, value); }
}
