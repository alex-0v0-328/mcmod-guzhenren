package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.gameplay.body.BodyStrengthService;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * All-Out Effort [全力以赴] effect: while it runs, the body's carrying limit [承受上限] does not
 * apply, unlocking a stockpiled 斤 total.
 *
 * <p>Timed effects own their truth on vanilla's timer. This is a marker with no {@link
 * net.minecraft.world.entity.ai.attributes.AttributeModifier} — the lift is read by {@link
 * BodyStrengthService#getUsableJin}, so attack still comes
 * out of one formula rather than gaining a second source.
 *
 * <p>A timed buff alters nothing permanently. Re-using it while it runs is a refusal ({@code all_out_active}).
 *
 * @author Alex
 * @version 1.0.0
 * @see PathStrengthService
 * @since 1.0.0
 */

public class AllOutEffortEffect extends MobEffect {

    public AllOutEffortEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
