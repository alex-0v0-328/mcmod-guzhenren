package net.alex.guzhenren.effect.timed;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The Crash Gu family [横冲蛊 / 直撞蛊 / 横冲直撞蛊]: one class carrying all three dashes --
 * horizontal, vertical, and the charging one that moves on both axes.
 *
 * <p>Which axes a dash may use is
 * read on the client from WHICH of the three registrations the player wears; the movement itself is
 * reported through {@link net.alex.guzhenren.network.payload.DashPayload}.
 * The charging shape grades its icon by rank because it spans ranks four and five.
 *
 * <p>The dash distance spec, the multiplier on Epic Fight's dodge coordinate vector and its All-Out Effort [全力以赴]
 * doubling, is {@link CrashDash}, kept apart because a {@code MobEffect} cannot load in plain JUnit.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.payload.DashPayload
 * @since 1.0.0
 */

public final class CrashGuEffect extends MobEffect {

    public CrashGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
