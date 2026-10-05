package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The timed buff of the Dragonpill Cricket Gu [龙丸蛐蛐蛊]: a jump-strength lift for thirty seconds,
 * followed by a twenty-second weakness aftermath.
 *
 * <p>This one carries a real {@link net.minecraft.world.entity.ai.attributes.AttributeModifier} on
 * {@link net.minecraft.world.entity.ai.attributes.Attributes#JUMP_STRENGTH} — the one effect NOT
 * implementing {@link net.alex.guzhenren.effect.AttackContributor}: jump, not attack.
 *
 * <p>⚠ The aftermath lands on the last tick ({@code duration == 1}) — MobEffect has no expiry hook,
 * so milk or {@code /effect clear} skips the penalty, which is the intended design.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.effect.AttackContributor
 * @since 1.0.0
 */

public class DragonpillCricketGuEffect extends MobEffect {

    public static final int DURATION_TICKS = 30 * Ticks.SECOND;
    public static final int AFTERMATH_TICKS = 20 * Ticks.SECOND;
    public static final double JUMP_BONUS = 0.20D;
    private static final int LAST_TICK = 1;
    private static final ResourceLocation MODIFIER_ID =
            Guzhenren.id("dragonpill_cricket_jump_strength");

    public DragonpillCricketGuEffect(MobEffectCategory category, int color) {
        super(category, color);
        addAttributeModifier(Attributes.JUMP_STRENGTH, MODIFIER_ID, JUMP_BONUS,
                AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration == LAST_TICK; }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.addEffect(ModEffects.instance(MobEffects.WEAKNESS, AFTERMATH_TICKS));
        return true;
    }
}
