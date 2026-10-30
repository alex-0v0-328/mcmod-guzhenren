package net.alex.guzhenren.item.gu.mortal;

import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The shared class behind every instant-buff Gu; the effect holder and its length come from registration.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.TendedGuItem}. Eight Gu register against this one class -- the
 * three beast buffs (花豕蛊 / 蛮力天牛蛊 / 龙丸蛐蛐蛊), the four Crash Gu and the Hardship Strength Gu; the
 * payout simply grants the timed buff ({@link TimedEffects#grantBuff}) and returns. The gate refuses only while a
 * stronger copy of the same effect runs (a weaker Gu would pay its cost for nothing); an equal or weaker running
 * copy is refreshed, a stronger Gu taking the weaker one's effect and strands. An Amplify Effect Gu [功倍蛊]
 * multiplies the length it adds and lays its Rule strands on the buff.
 *
 * <p>⚠ Each of them still declares its own feed tag. Sharing a class does not mean sharing a larder.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.TendedGuItem
 * @since 1.0.0
 */

public class BuffGuItem extends TendedGuItem {

    private static final int COOLDOWN_HUNGER_MULTIPLIER = 2;
    private static final String FAILED_STRONGER = "guzhenren.item.failed.buff_stronger";
    private final Holder<MobEffect> buff;
    private final int durationTicks;
    private final int amplifier;

    public BuffGuItem(Properties properties, Holder<MobEffect> buff, int durationTicks, GuSpec spec) {
        this(properties, buff, durationTicks, 0, spec);
    }

    public BuffGuItem(Properties properties, Holder<MobEffect> buff, int durationTicks, int amplifier,
                      GuSpec spec) {
        super(properties, spec);
        this.buff = buff;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    @Override
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) {
        PathEffect<TimedBuff> current = TimedEffects.findBuff(player, buff);
        return current != null && current.payload().amplifier() > amplifier
                ? new Refusal(FAILED_STRONGER, buff.value().getDisplayName()) : null;
    }

    @Override
    protected void applyEffect(ServerPlayer player, ItemStack stack) {
        PathEffect<TimedBuff> current = TimedEffects.findBuff(player, buff);
        if (current != null && amplifier < current.payload().amplifier()) return;

        Amplification amplification = AmplifyEffectGuItem.bill(player, this);
        int added = durationTicks * amplification.factor();
        int duration = current == null ? added : getNextDuration(TimedEffects.getBuffTicksLeft(player, buff),
                current.payload().amplifier(), added, amplifier, getEffectCooldownLeft(player, stack) > 0);
        TimedEffects.grantBuff(player, this, buff, amplifier, duration, amplification);
    }

    static int getNextDuration(int currentDuration, int currentAmplifier, int addedDuration, int addedAmplifier,
                               boolean accumulates) {
        if (addedAmplifier < currentAmplifier) return currentDuration;
        if (addedAmplifier > currentAmplifier) return addedDuration;
        if (!accumulates) return Math.max(currentDuration, addedDuration);
        return (int) Math.min(Integer.MAX_VALUE, (long) currentDuration + addedDuration);
    }

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    protected boolean canUseDuringEffectCooldown() { return true; }

    @Override
    protected int getHungerCostMultiplier(Player player, ItemStack stack) {
        return getEffectCooldownLeft(player, stack) > 0 ? COOLDOWN_HUNGER_MULTIPLIER : 1;
    }
}
