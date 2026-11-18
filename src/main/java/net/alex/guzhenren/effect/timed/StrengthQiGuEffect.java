package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 力气蛊[Strength Qi Gu]的限时图标效果：生效期间空手攻击原版触及距离外的 Epic Fight 锁定目标时，改由
 * {@code QiWaveService} 以不高于 {@code StrengthRound.RANGE_INTERVAL_TICKS} 的频率发射
 * {@link net.alex.guzhenren.entity.projectile.QiWaveEntity}（兽力虚影流改为发射
 * {@link net.alex.guzhenren.entity.projectile.BeastChargeEntity}）；本类仅承载客户端图标与时长（客户端据此判断是否拦截
 * 发拳），服务端真值以限时流派效果 {@code TimedEffects.hasBuff} 为准。
 */

public final class StrengthQiGuEffect extends MobEffect {

    public static final int DURATION_TICKS = 30 * Ticks.SECOND;

    public StrengthQiGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
