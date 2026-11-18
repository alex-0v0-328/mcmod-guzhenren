package net.alex.guzhenren.effect;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * 流血效果：每秒造成 {@value #DAMAGE_PER_PULSE} 点无视护甲伤害，由血月刃
 * （{@link net.alex.guzhenren.entity.projectile.MoonBladeEntity}）附加；由于背后不挂接持久状态，因此是
 * {@link MilkCureEvents} 中唯一允许牛奶解除的效果，且脉冲不指定攻击者而沿用最后伤害者归属。
 */

public final class BleedingEffect extends PeriodicEffect {

    public static final float DAMAGE_PER_PULSE = 1.0F;

    public BleedingEffect(MobEffectCategory category, int color) {
        super(category, color, Ticks.SECOND);
    }

    @Override
    protected void onPulse(@NotNull LivingEntity entity, int amplifier) {
        entity.hurt(ModDamageTypes.source(entity, ModDamageTypes.BLEEDING), DAMAGE_PER_PULSE);
    }
}
