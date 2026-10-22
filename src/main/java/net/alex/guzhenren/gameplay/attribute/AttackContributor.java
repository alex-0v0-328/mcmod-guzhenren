package net.alex.guzhenren.gameplay.attribute;


/**
 * The one seam anything adds attack damage through, so the body panel and a real hit stay one number.
 *
 * <p>Implemented by the 力道 qi effect and the timed attack-buff Gu effects. {@link
 * AttackDamageService#getBonus} walks {@code getActiveEffects()}
 * and asks each contributor, instead of listing the effects it knows about — a new effect is one
 * interface, no edit there.
 *
 * <p>☠ An {@link net.minecraft.world.entity.ai.attributes.AttributeModifier} here would be a second
 * source for a single number, so never give an effect its own modifier.
 *
 * @author Alex
 * @version 1.0.0
 * @see AttackDamageService
 * @since 1.0.0
 */

public interface AttackContributor {

    double attackBonus(int amplifier);
}
