package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 自力更生蛊[Self Reliance Gu]持续恢复流派效果的 {@code payload} 记录：保存触发该效果的蛊虫阶数（{@code grade}，兼作状
 * 态栏投影图标的放大器等级；旧存档残留的功倍蛊[Amplify Effect Gu]倍率字段在反序列化时自动忽略）。
 */

public record SelfRelianceHeal(int grade) {

    public static final MapCodec<SelfRelianceHeal> CODEC =
            Codec.INT.fieldOf("grade").xmap(SelfRelianceHeal::new, SelfRelianceHeal::grade);
    public static final StreamCodec<FriendlyByteBuf, SelfRelianceHeal> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(SelfRelianceHeal::new, SelfRelianceHeal::grade).mapStream(buf -> buf);
}
