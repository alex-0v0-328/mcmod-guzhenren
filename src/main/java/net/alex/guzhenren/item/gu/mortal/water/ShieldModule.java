package net.alex.guzhenren.item.gu.mortal.water;

import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The shield module [护盾]: raises one shield layer whose type, points and length come from registration. The
 * Heavenly Mugwort Gu [天蓬蛊] carries it, with a {@link net.alex.guzhenren.item.gu.mortal.RecastModule}.
 *
 * <p>Every use fills the shield to its full points and restarts its timer through {@link ShieldService#raise}; a
 * shield still up is replaced, never stacked. It refuses nothing. An Amplify Effect Gu [功倍蛊] multiplies the
 * length, never the points. {@code TimedEffects} finds the Gu of a shield type by this module.
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldService
 * @since 1.0.0
 */

public record ShieldModule(ShieldType type, float points, int durationTicks) implements GuModule {

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        ShieldService.raise(player, type, points, durationTicks * amplification.factor());
        TimedEffects.markShieldAmplified(player, type, amplification);
    }
}
