package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The Crash Gu family [横冲蛊 / 直撞蛊 / 横冲直撞蛊]: one class carrying all three dashes --
 * horizontal, vertical, and the charging one that moves on both axes.
 *
 * <p>The class holds the shared duration helper and the dash distance spec. Which axes a dash may use is
 * read on the client from WHICH of the three registrations the player wears; the movement itself is
 * reported through {@link net.alex.guzhenren.network.payload.DashPayload}.
 * The charging shape grades its icon by rank because it spans ranks four and five.
 *
 * <p>{@link #DASH_COORD_SCALE} is the multiplier on Epic Fight's dodge coordinate vector: one number
 * shared by the whole Crash Gu family, 4.5. It lives here rather than in the EF
 * bridge so the spec stays readable (and pinnable in pure tests) without Epic Fight on the classpath.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.payload.DashPayload
 * @since 1.0.0
 */

public final class CrashGuEffect extends MobEffect {

    public static final double DASH_COORD_SCALE = 4.5D;

    public CrashGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    public static int duration(int seconds) { return seconds * Ticks.SECOND; }
}
