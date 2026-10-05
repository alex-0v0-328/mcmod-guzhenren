package net.alex.guzhenren.item.gu.mortal;

import net.alex.guzhenren.item.gu.GuSpec;
import net.alex.guzhenren.item.gu.TendedGuItem;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The shared class behind every instant-buff Gu; the effect holder and its length come from registration.
 *
 * <p>Extends {@link net.alex.guzhenren.item.gu.TendedGuItem}. Eight Gu register against this one class -- the
 * three beast buffs (花豕蛊 / 蛮力天牛蛊 / 龙丸蛐蛐蛊), the four Crash Gu and the Hardship Strength Gu; the
 * payout simply adds the effect and returns. The gate is
 * always open here because the refusal "effect already running" lives in the base's own check.
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
    protected @Nullable Refusal payoutGate(Player player, ItemStack stack) { return null; }

    @Override
    protected void payout(ServerPlayer player, ItemStack stack) {
        MobEffectInstance current = player.getEffect(buff);
        if (current != null && amplifier < current.getAmplifier()) return;

        int duration = current == null ? durationTicks : nextDuration(current.getDuration(),
                current.getAmplifier(), durationTicks, amplifier, effectCooldownLeft(player, stack) > 0);
        if (current != null) player.removeEffect(buff);
        player.addEffect(ModEffects.instance(buff, duration, amplifier));
    }

    static int nextDuration(int currentDuration, int currentAmplifier, int addedDuration, int addedAmplifier,
                            boolean accumulates) {
        if (addedAmplifier < currentAmplifier) return currentDuration;
        if (addedAmplifier > currentAmplifier) return addedDuration;
        if (!accumulates) return Math.max(currentDuration, addedDuration);
        return (int) Math.min(Integer.MAX_VALUE, (long) currentDuration + addedDuration);
    }

    @Override
    protected boolean allowsUseDuringEffectCooldown() { return true; }

    @Override
    protected int hungerCostMultiplier(Player player, ItemStack stack) {
        return effectCooldownLeft(player, stack) > 0 ? COOLDOWN_HUNGER_MULTIPLIER : 1;
    }
}
