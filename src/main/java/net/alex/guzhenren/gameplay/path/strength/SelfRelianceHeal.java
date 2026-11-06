package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The payload of a running Self Reliance heal [自力更生]: the grade of the Gu that started it, the icon's amplifier.
 *
 * <p>A save from before the auto rebuild also carries an Amplify Effect Gu factor; the codec ignores it.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.path.strength.SelfRelianceBehavior
 * @since 1.0.0
 */

public record SelfRelianceHeal(int grade) {

    public static final MapCodec<SelfRelianceHeal> CODEC =
            Codec.INT.fieldOf("grade").xmap(SelfRelianceHeal::new, SelfRelianceHeal::grade);
    public static final StreamCodec<FriendlyByteBuf, SelfRelianceHeal> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(SelfRelianceHeal::new, SelfRelianceHeal::grade).mapStream(buf -> buf);
}
