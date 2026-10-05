package net.alex.guzhenren.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * An effect that holds no state and runs no tick: the vanilla icon (and, for a timed one, the timer) of a
 * state whose truth lives elsewhere. One class, registered once per marker in {@code ModEffects}:
 *
 * <ul>
 * <li>{@code HALF_ZOMBIE} [半生半僵] -- projects the form stored on {@code BodyData};
 * {@code UndeadService.tickHalfZombie} re-applies or removes it every heartbeat, so milk,
 * {@code /effect clear} and death cannot strand a player wearing a form they are no longer in.</li>
 * <li>{@code DEATH_QI} [死气] and {@code ESSENCE_QI} [元气] -- project the qi pools in {@code PathQiData}
 * and are rebuilt by {@code PathQiService.syncEffects}; {@code UndeadService.tickDeathQi} burns lifespan
 * [寿元], and {@code ApertureEssenceService.getEssenceQiBonus} lifts essence regeneration. ⚠ Death Qi
 * outranks Essence Qi: the regen step checks {@code isChoked} first and returns.</li>
 * <li>{@code ALL_OUT_EFFORT} [全力以赴] -- timed; while it runs the carrying limit [承受上限] does not
 * apply, which {@code BodyStrengthService.getUsableJin} reads. Re-using it while it runs is a refusal
 * ({@code all_out_active}).</li>
 * <li>{@code LIQUOR_WORM} [酒虫] -- worn while distilling; {@code ApertureEssenceService} owns the three
 * phases, so removing the effect does not end the distillation, and {@code closeDistilling} pays the 1:2
 * back by reading the level.</li>
 * </ul>
 *
 * <p>☠ Never hang a rule on a marker's expiry: {@link MobEffect} has no expiry hook, and milk or
 * {@code /effect clear} removes it silently. The truth reads its own level instead. ☠ Never give a marker
 * an {@code AttributeModifier}: attack goes through {@code AttackContributor}, one formula.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class MarkerEffect extends MobEffect {

    public MarkerEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
