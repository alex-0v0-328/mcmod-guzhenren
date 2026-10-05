package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.PeriodicEffect;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * The Malicious Thought Gu [恶念蛊] effect: each second for twelve seconds it adds evil-tagged
 * thoughts [恶念] to the mind ocean [脑海].
 *
 * <p>Timed effects own their truth on vanilla's timer. One effect, four grades — the amplifier is
 * the rank's tier, and the {@code evilPerSecond} table is given at registration. The immediate
 * portion lands in the Gu's payout, not here; this effect owns only the per-second drip. Thoughts
 * land through {@link MindService#addThoughts} tagged
 * {@code EVIL}.
 *
 * @author Alex
 * @version 1.0.0
 * @see MindService
 * @since 1.0.0
 */

public class MaliciousThoughtEffect extends PeriodicEffect {

    public static final int DURATION_TICKS = 12 * Ticks.SECOND;
    private final long[] evilPerSecond;

    public MaliciousThoughtEffect(MobEffectCategory category, int color, long[] evilPerSecond) {
        super(category, color, Ticks.SECOND);
        this.evilPerSecond = evilPerSecond;
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) {
            int index = Math.clamp(amplifier, 0, evilPerSecond.length - 1);
            MindService.addThoughts(player, evilPerSecond[index], ThoughtTag.EVIL);
        }
        return true;
    }
}
