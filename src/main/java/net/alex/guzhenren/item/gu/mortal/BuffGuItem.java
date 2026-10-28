package net.alex.guzhenren.item.gu.mortal;

import java.util.function.Supplier;
import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.item.gu.mortal.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The shared class behind every instant-buff Gu; the effect holder and its length come from registration.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.TendedGuItem}. Nine Gu register against this one class -- the
 * three beast buffs (花豕蛊 / 蛮力天牛蛊 / 龙丸蛐蛐蛊), the four Crash Gu, the Hardship Strength Gu and the
 * Heavenly Mugwort Gu [天蓬蛊], whose buff is vanilla Absorption; the
 * payout simply adds the effect and returns. The gate refuses only while a stronger copy of the same
 * effect runs (a weaker Gu would pay its cost for nothing); an equal or weaker running copy is refreshed. An
 * Amplify Effect Gu [功倍蛊] multiplies the length it adds.
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
    private final Supplier<? extends MobEffect> buff;
    private final int durationTicks;
    private final int amplifier;

    public BuffGuItem(Properties properties, Supplier<? extends MobEffect> buff, int durationTicks, GuSpec spec) {
        this(properties, buff, durationTicks, 0, spec);
    }

    public BuffGuItem(Properties properties, Supplier<? extends MobEffect> buff, int durationTicks, int amplifier,
                      GuSpec spec) {
        super(properties, spec);
        this.buff = buff;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    @Override
    protected @Nullable Refusal checkPayoutGate(Player player, ItemStack stack) {
        MobEffectInstance current = ModEffects.get(player, buff);
        return current != null && current.getAmplifier() > amplifier
                ? new Refusal(FAILED_STRONGER, buff.get().getDisplayName()) : null;
    }

    @Override
    protected void payOut(ServerPlayer player, ItemStack stack) {
        MobEffectInstance current = ModEffects.get(player, buff);
        if (current != null && amplifier < current.getAmplifier()) return;

        int added = durationTicks * AmplifyEffectGuItem.amplify(player, this);
        int duration = current == null ? added : getNextDuration(current.getDuration(),
                current.getAmplifier(), added, amplifier, getEffectCooldownLeft(player, stack) > 0);
        if (current != null) ModEffects.remove(player, buff);
        player.addEffect(ModEffects.instance(buff, duration, amplifier));
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
