package net.alex.guzhenren.effect.pool;

import net.alex.guzhenren.gameplay.path.qi.PathQiData;
import net.alex.guzhenren.gameplay.path.qi.PathQiService;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Death Qi [死气] effect — a pool projection of the 死气 in {@link
 * PathQiData}: burns lifespan [寿元] and floors health while above zero.
 *
 * <p>Pool effects are rebuilt every heartbeat by {@code PathQiService.syncEffects}, so milk cannot cure
 * them. A {@link net.minecraft.world.effect.MobEffect} has no expiry hook, so the burning runs on the
 * heartbeat in {@code UndeadService.tickDeathQi}, which also holds its numbers, settling the debt by
 * reading the level.
 *
 * <p>Life Qi [生气] pays 死气 down 1:1; clearing to 0 refunds {@code REFUND_NUMERATOR / REFUND_DENOMINATOR}.
 *
 * @author Alex
 * @version 1.0.0
 * @see PathQiService
 * @since 1.0.0
 */

public class DeathQiEffect extends MobEffect {

    public static final int REFUND_NUMERATOR = 3;
    public static final int REFUND_DENOMINATOR = 4;

    public DeathQiEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
