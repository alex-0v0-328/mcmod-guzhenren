package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * The Casual Gu [随意蛊] effect: each second for ten seconds it adds a random amount of natural
 * thoughts [念] to the mind ocean [脑海].
 *
 * <p>Timed effects own their truth on vanilla's timer. One effect, two grades — the amplifier is
 * the rank's tier, and the {@code RANGES} table widens with it. Thoughts land through {@link
 * MindService#addThoughts} tagged {@code NATURAL}.
 *
 * <p>⚠ {@code applyEffectTick} must {@code return true} — returning false lets vanilla remove the
 * effect on the spot.
 *
 * @author Alex
 * @version 1.0.0
 * @see MindService
 * @since 1.0.0
 */

public class CasualThoughtEffect extends MobEffect {

    public static final int DURATION_TICKS = 10 * Ticks.SECOND;
    private static final int[][] RANGES = { { 1, 100 }, { 100, 200 } };

    public CasualThoughtEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % Ticks.SECOND == 0;
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) {
            int index = Math.clamp(amplifier, 0, RANGES.length - 1);
            int amount = player.getRandom().nextIntBetweenInclusive(RANGES[index][0], RANGES[index][1]);
            MindService.addThoughts(player, amount, ThoughtTag.NATURAL);
        }
        return true;
    }
}
