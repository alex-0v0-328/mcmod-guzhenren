package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.gameplay.clock.TimeRateContributor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 自身时间加速类蛊虫（两更蛊[Second Watch Gu]、三更蛊[Third Watch Gu]）的 {@link TimeRateContributor} 效果基类。各更蛊
 * 分别注册独立效果以支持同时佩戴与速率叠加，效果倍率对应层数（封顶后按 {@code ratePerLayer} 线性累加）；持续时间仍按世
 * 界时间流逝，五分钟倒计时不会自我加速。
 */

public class WatchGuEffect extends MobEffect implements TimeRateContributor {

    private final int ratePerLayer;
    private final int maxLayers;

    public WatchGuEffect(MobEffectCategory category, int color, int ratePerLayer, int maxLayers) {
        super(category, color);
        this.ratePerLayer = ratePerLayer;
        this.maxLayers = maxLayers;
    }

    public int getNextAmplifier(int amplifier) { return Mth.clamp(amplifier + 1, 0, maxLayers - 1); }

    @Override
    public int timeRate(int amplifier) { return ratePerLayer * Mth.clamp(amplifier + 1, 1, maxLayers); }
}
