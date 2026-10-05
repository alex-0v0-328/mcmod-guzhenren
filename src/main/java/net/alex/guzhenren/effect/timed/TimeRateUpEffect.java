package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.gameplay.path.time.TimeRateContributor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Time Rate Up [自身时间加速]: each layer makes the wearer's own clock outrun the world by its base rate.
 *
 * <p>One effect per Watch Gu [更蛊], so both kinds can run together and their capped rates add. The
 * effect duration remains world time: five minutes never hastens itself.
 *
 * @author Alex
 * @version 1.0.0
 * @see TimeRateContributor
 * @since 1.0.0
 */

public class TimeRateUpEffect extends MobEffect implements TimeRateContributor {

    private final int ratePerLayer;
    private final int maxLayers;

    public TimeRateUpEffect(MobEffectCategory category, int color, int ratePerLayer, int maxLayers) {
        super(category, color);
        this.ratePerLayer = ratePerLayer;
        this.maxLayers = maxLayers;
    }

    public int nextAmplifier(int amplifier) { return Math.clamp(amplifier + 1, 0, maxLayers - 1); }

    @Override
    public int timeRate(int amplifier) { return ratePerLayer * Math.clamp(amplifier + 1, 1, maxLayers); }
}
