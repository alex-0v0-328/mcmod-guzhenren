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
 * <li>{@code JADE_SKIN_GU} [玉皮蛊] and {@code WHITE_JADE_GU} [白玉蛊] -- timed; project the skin [皮肤] stored
 * on {@code BodyData} with its end tick, re-applied or removed by {@code SkinService.tickSkin} every
 * heartbeat; the flat damage reduction reads the skin, never the effect.</li>
 * <li>{@code HEAVENLY_MUGWORT_GU} [天蓬蛊] -- timed; projects the shield stored on {@code MugwortShieldData},
 * re-applied or removed by {@code MugwortShieldService.tickShield} every heartbeat; the hit that empties the
 * shield removes it at once.</li>
 * <li>{@code LIQUOR_WORM} [酒虫] -- the icon of the {@code LIQUOR_DISTILLING} path effect, which is the truth of
 * the distilling; {@code closeDistilling} pays the 1:2 back once that effect is gone.</li>
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
