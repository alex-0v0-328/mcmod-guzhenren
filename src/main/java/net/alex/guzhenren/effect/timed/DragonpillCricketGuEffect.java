package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.effect.AftermathEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The timed buff of the Dragonpill Cricket Gu [龙丸蛐蛐蛊]: a jump-strength lift for thirty seconds,
 * followed by a twenty-second weakness aftermath.
 *
 * <p>The lift is {@link #JUMP_BONUS} per level added to every jump's upward speed; {@link DragonpillCricketJump}
 * applies it as the jump starts. The one effect NOT implementing
 * {@link net.alex.guzhenren.gameplay.attribute.AttackContributor}: jump, not attack.
 *
 * <p>⚠ 1.20.1 has no player jump-strength attribute ({@code Attributes.JUMP_STRENGTH} is the horse's), so the
 * mainline's attribute modifier becomes a jump-event lift of the same size on normal ground.
 *
 * <p>The duration and the aftermath come from {@link AftermathEffect}.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.attribute.AttackContributor
 * @since 1.0.0
 */

public class DragonpillCricketGuEffect extends AftermathEffect {

    public static final double JUMP_BONUS = 0.20D;

    public DragonpillCricketGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
