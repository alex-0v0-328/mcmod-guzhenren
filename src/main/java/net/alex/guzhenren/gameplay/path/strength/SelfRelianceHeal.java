package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 自力更生蛊[Self Reliance Gu]回复的 payload：发起蛊的级数，兼作图标效果等级。旧存档另带的功倍蛊[Amplify Effect Gu]
 * 倍数由编解码器忽略。
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
