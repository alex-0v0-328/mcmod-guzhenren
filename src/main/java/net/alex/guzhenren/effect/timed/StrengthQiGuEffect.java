package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 力气蛊[Strength Qi Gu]效果：生效时空手打原版触及距离外的 Epic Fight 锁定目标，改发气波（兽力虚影流发兽力冲击），
 * 不限距离，至多每 {@code StrengthRound.RANGE_INTERVAL_TICKS} 一次（{@code QiWaveService}）；本类只管时长与图标，
 * 真值为限时流派效果（{@code TimedEffects.hasBuff}），服务端读它，客户端读本效果定是否发拳。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.projectile.QiWaveEntity
 * @see net.alex.guzhenren.entity.projectile.BeastChargeEntity
 * @since 1.0.0
 */

public final class StrengthQiGuEffect extends MobEffect {

    public static final int DURATION_TICKS = 30 * Ticks.SECOND;

    public StrengthQiGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
