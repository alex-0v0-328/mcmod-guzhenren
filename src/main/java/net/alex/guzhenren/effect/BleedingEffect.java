package net.alex.guzhenren.effect;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * 流血效果：每秒 1 点伤害、无视护甲，由血月刃留下；无状态普通效果，故牛奶能解（{@link MilkCureEvents} 唯一例外）；脉冲
 * 不指明攻击者，击杀归最后伤害者，同中毒。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.projectile.MoonBladeEntity
 * @since 1.0.0
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
