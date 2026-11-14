package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.PeriodicEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * 生机叶蛊[Vitality Leaf Gu]限时治疗：每 0.5 秒回 1 点生命，共 64 次；投影流派效果
 * {@code TimedEffects.grantLeaf}（真值）：显示时脉冲，{@code /effect clear} 后 1 秒内补回；运行中再催动被拒
 * （{@code effect_active}）；不留永久改动。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
