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
 * 蛊升起的护盾种类：血条填充色、是否霸体（全额吃下时肉身不动）、投影它的标记效果。
 *
 * <ul>
 *   <li>封闭词表，{@code ShieldData} 每种一层；点数与时长归升起之蛊的注册。新护盾蛊只加常量和 LanguageProvider 名称，
 *       {@code ShieldService}、{@code ShieldHud}、{@code ShieldGlowHud} 不用改。</li>
 *   <li>非霸体仍吃原版盾牌格挡后的 0.4 击退。</li>
 *   <li>⚠ {@link #CODEC} 把常量名写入玩家存档：改名会丢失旧名保存的层。</li>
 * </ul>
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
