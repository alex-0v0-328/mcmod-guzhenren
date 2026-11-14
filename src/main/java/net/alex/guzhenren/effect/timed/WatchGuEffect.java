package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.gameplay.clock.TimeRateContributor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 自身时间加速效果：每层使持有者时钟按基础速率快于世界；两更蛊[Second Watch Gu]、三更蛊[Third Watch Gu]各一效果，
 * 可并行，封顶后速率相加；时长仍按世界时间，五分钟不自我加速。
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeRateContributor
 * @since 1.0.0
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
