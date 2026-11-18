package net.alex.guzhenren.gameplay.path;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 流派效果与道痕[Path Marks]在玩家身上的四大载体枚举：空窍（{@link #APERTURE}）、肉体（{@link #BODY}）、魂魄
 * （{@link #SOUL}）与脑海（{@link #MIND}）；每个 {@link PathEffect} 挂载于单一载体，而不带效果的裸道痕
 * （{@link PathEntry}）仅存在于肉体上。
 */

public enum EffectCarrier implements NamedEnum {

    APERTURE,
    BODY,
    SOUL,
    MIND;

    public static final Codec<EffectCarrier> CODEC = StringRepresentable.fromEnum(EffectCarrier::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.path.carrier."; }
}
