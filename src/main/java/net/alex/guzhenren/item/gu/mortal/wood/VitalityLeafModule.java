package net.alex.guzhenren.item.gu.mortal.wood;

import net.alex.guzhenren.effect.timed.VitalityLeafGuEffect;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Vitality Leaf Gu's [生机叶蛊] module: grants the vitality effect [生机叶], refusing while one is still running.
 *
 * <p>The gate checks {@link TimedEffects#hasLeaf} so a re-use while the leaf runs is a refusal, not a refresh; the
 * effect hands its duration to {@link TimedEffects#grantLeaf}. An Amplify Effect Gu [功倍蛊] multiplies that
 * duration, and with it the health the leaf returns.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
 */

public record VitalityLeafModule() implements GuModule {

    private static final String FAILED_VITALITY_ACTIVE = "guzhenren.item.failed.vitality_active";

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        return TimedEffects.hasLeaf(player) ? new Refusal(FAILED_VITALITY_ACTIVE) : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        TimedEffects.grantLeaf(player, gu, (long) VitalityLeafGuEffect.DURATION_TICKS * amplification.factor(),
                amplification);
    }
}
