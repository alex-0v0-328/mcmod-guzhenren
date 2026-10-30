package net.alex.guzhenren.item.gu.mortal.time;

import net.alex.guzhenren.effect.timed.WatchGuEffect;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

/**
 * The Watch Gu's [更蛊] module: for a while its holder's own clock [自身时间] outruns the world's.
 *
 * <p>Two rungs register with it: 两更蛊 reaches two ×2 layers, and 三更蛊 reaches three ×3 layers. The effect owns
 * both its rate and layer cap; a form that is not a {@link WatchGuEffect} is refused at construction.
 *
 * <p>⚠ It refuses nothing. Reusing one kind adds a capped layer and refreshes its duration; wearing
 * both kinds is the design, and their rates add. An Amplify Effect Gu [功倍蛊] multiplies the refreshed
 * duration, not the layers.
 *
 * @author Alex
 * @version 1.0.0
 * @see TimedEffects#grantWatch
 * @since 1.0.0
 */

public record WatchModule(Holder<MobEffect> form, int effectTicks) implements GuModule {

    public WatchModule {
        if (!(form.value() instanceof WatchGuEffect)) {
            throw new IllegalArgumentException("watch gu needs a WatchGuEffect");
        }
    }

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        TimedEffects.grantWatch(player, gu, form, (long) effectTicks * amplification.factor(), amplification);
    }
}
