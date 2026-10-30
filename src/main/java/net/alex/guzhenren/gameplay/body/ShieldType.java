package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * A kind of shield [护盾] a Gu raises over the body: its bar fill, whether a hit it takes whole leaves the body
 * unmoved [霸体], and the marker effect that projects it.
 *
 * <p>Closed vocabulary enum, one layer per kind on {@code ShieldData}; points and length are the raising Gu's
 * registration's, never this enum's. A new shield Gu adds a constant here and its name in the language provider;
 * {@code ShieldService}, the bar ({@code ShieldHud}) and the screen glow ({@code ShieldGlowHud}) need no change.
 * A kind that is not super armor still takes the 0.4 knockback vanilla leaves after a shield block.
 *
 * <p>⚠ {@link #CODEC} writes the saved name into player saves: renaming a constant drops a layer saved under it.
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldService
 * @since 1.0.0
 */

public enum ShieldType implements NamedEnum {

    HEAVENLY_MUGWORT(0xFFE6E0D2, true, () -> ModEffects.HEAVENLY_MUGWORT_GU);

    public static final Codec<ShieldType> CODEC = StringRepresentable.fromEnum(ShieldType::values);

    private final int barFill;
    private final boolean superArmor;
    private final Supplier<Holder<MobEffect>> effect;

    ShieldType(int barFill, boolean superArmor, Supplier<Holder<MobEffect>> effect) {
        this.barFill = barFill;
        this.superArmor = superArmor;
        this.effect = effect;
    }

    public int getBarFill() { return barFill; }

    public boolean isSuperArmor() { return superArmor; }

    public @NotNull Holder<MobEffect> getEffect() { return effect.get(); }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.shield."; }
}
