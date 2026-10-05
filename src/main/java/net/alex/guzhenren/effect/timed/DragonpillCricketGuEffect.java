package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.AftermathEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The timed buff of the Dragonpill Cricket Gu [龙丸蛐蛐蛊]: a jump-strength lift for thirty seconds,
 * followed by a twenty-second weakness aftermath.
 *
 * <p>This one carries a real {@link net.minecraft.world.entity.ai.attributes.AttributeModifier} on
 * {@link net.minecraft.world.entity.ai.attributes.Attributes#JUMP_STRENGTH} — the one effect NOT
 * implementing {@link net.alex.guzhenren.gameplay.attribute.AttackContributor}: jump, not attack.
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
    private static final ResourceLocation MODIFIER_ID =
            Guzhenren.id("dragonpill_cricket_jump_strength");

    public DragonpillCricketGuEffect(MobEffectCategory category, int color) {
        super(category, color);
        addAttributeModifier(Attributes.JUMP_STRENGTH, MODIFIER_ID, JUMP_BONUS,
                AttributeModifier.Operation.ADD_VALUE);
    }
}
