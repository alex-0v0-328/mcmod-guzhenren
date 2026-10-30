package net.alex.guzhenren.item.gu.mortal;

import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The instant-buff module [限时增益]: grants a timed buff whose effect holder, length and strength come from
 * registration. Eleven Gu carry it, each with a {@link RecastModule}: the three beast buffs (花豕蛊 / 蛮力天牛蛊 /
 * 龙丸蛐蛐蛊), the four Crash Gu, the Hardship Strength Gu and the three Self Reliance Gu, which add an
 * {@link net.alex.guzhenren.item.gu.mortal.strength.AutoUseModule}.
 *
 * <p>The effect simply grants the timed buff ({@link TimedEffects#grantBuff}) and returns. The gate refuses only
 * while a stronger copy of the same effect runs (a weaker Gu would pay its cost for nothing); an equal or weaker
 * running copy is refreshed, a stronger Gu taking the weaker one's effect and strands. Inside the Gu's effect
 * cooldown the same strength adds its length; outside it the longer length wins ({@link #getNextDuration}). An
 * Amplify Effect Gu [功倍蛊] multiplies the length it adds and lays its Rule strands on the buff.
 *
 * <p>⚠ Each of them still declares its own feed tag. Sharing a module does not mean sharing a larder.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.TendedGuItem
 * @since 1.0.0
 */

public record TimedBuffModule(Holder<MobEffect> buff, int durationTicks, int amplifier) implements GuModule {

    private static final String FAILED_STRONGER = "guzhenren.item.failed.buff_stronger";

    public TimedBuffModule(Holder<MobEffect> buff, int durationTicks) { this(buff, durationTicks, 0); }

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        PathEffect<TimedBuff> current = TimedEffects.findBuff(player, buff);
        return current != null && current.payload().amplifier() > amplifier
                ? new Refusal(FAILED_STRONGER, buff.value().getDisplayName()) : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathEffect<TimedBuff> current = TimedEffects.findBuff(player, buff);
        if (current != null && amplifier < current.payload().amplifier()) return;

        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        int added = durationTicks * amplification.factor();
        int duration = current == null ? added : getNextDuration(TimedEffects.getBuffTicksLeft(player, buff),
                current.payload().amplifier(), added, amplifier, gu.getEffectCooldownLeft(player, stack) > 0);
        TimedEffects.grantBuff(player, gu, buff, amplifier, duration, amplification);
    }

    static int getNextDuration(int currentDuration, int currentAmplifier, int addedDuration, int addedAmplifier,
                               boolean accumulates) {
        if (addedAmplifier < currentAmplifier) return currentDuration;
        if (addedAmplifier > currentAmplifier) return addedDuration;
        if (!accumulates) return Math.max(currentDuration, addedDuration);
        return (int) Math.min(Integer.MAX_VALUE, (long) currentDuration + addedDuration);
    }
}
