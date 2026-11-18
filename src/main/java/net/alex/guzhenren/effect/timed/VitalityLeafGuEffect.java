package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.PeriodicEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * 生机叶蛊[Vitality Leaf Gu]的限时周期治疗效果：每 0.5 秒回复 1 点生命值，总计 64 次；真值由流派效果
 * {@code TimedEffects.grantLeaf} 持有（图标被清除后会在 1 秒内补回），生效期间禁止重复催动，结束后不保留任何永久属性改
 * 动。
 */

public class VitalityLeafGuEffect extends PeriodicEffect {

    public static final int HEAL_INTERVAL_TICKS = Ticks.HALF_SECOND;
    public static final int DURATION_TICKS = 64 * HEAL_INTERVAL_TICKS;

    public VitalityLeafGuEffect(MobEffectCategory category, int color) {
        super(category, color, HEAL_INTERVAL_TICKS);
    }

    @Override
    protected void onPulse(@NotNull LivingEntity entity, int amplifier) {
        entity.heal(1.0F);
    }
}
