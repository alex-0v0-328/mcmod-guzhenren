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
 * 蛊虫升起的肉身护盾种类枚举：定义 HUD 护盾条填充色、是否附带霸体（全额格挡时自身免击退）以及对应的投影药水效果。
 * <ul>
 *   <li>封闭词汇枚举，{@link ShieldData} 每种最多存一层，护盾点数与持续时间由对应蛊虫注册定义；新增护盾蛊只需在此添加枚
 *       举常量与译名，{@link ShieldService} 与 HUD 渲染自动适配。</li>
 *   <li>非霸体护盾在全额格挡有源攻击时仍承受 {@link ShieldService#DEFENDER_KNOCKBACK} 击退。</li>
 *   <li>⚠ {@link #CODEC} 将枚举序列化名写入玩家存档，修改常量名会导致旧存档中保存的对应护盾层丢失。</li>
 * </ul>
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
