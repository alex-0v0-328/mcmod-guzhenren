package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The Heavenly Mugwort shield [天蓬护盾]: raised by the Gu, spent by the damage that passes the skin and the
 * invulnerability frames, before armor, and projected as the {@code HEAVENLY_MUGWORT_GU} effect on the heartbeat.
 *
 * <p>{@link #raise} fills the shield to the Gu's points and restarts its timer; a shield still up is replaced, never
 * stacked. {@link #absorb} takes what it can of a hit and passes the rest on; the hit that empties it ends it and
 * its effect at once. {@link #tickShield} clears a shield whose time ran out and keeps the effect in step with it.
 *
 * <p>The truth is {@code MugwortShieldData}, never the effect: a {@code MobEffect} has no expiry hook, and milk or
 * {@code /effect clear} removes it silently, so the heartbeat re-applies it as {@link SkinService} does a jade
 * skin. ⚠ Damage that bypasses invulnerability or effects ({@code /kill}, the void, starvation) passes untouched.
 * ⚠ The caller hands {@link #absorb} only what lands after the invulnerability frames, and a hit the shield takes
 * whole must still start them, or standing in fire drains it every tick ({@code BodyStateEvents}).
 *
 * @author Alex
 * @version 1.0.0
 * @see MugwortShieldData
 * @since 1.0.0
 */

public final class MugwortShieldService {

    private MugwortShieldService() {}

    public static @NotNull MugwortShieldData get(@NotNull Player player) {
        return PlayerData.get(player).getMugwortShield();
    }

    public static boolean isUp(@NotNull Player player) { return get(player).isUp(BodyService.getGameTime(player)); }

    public static long getTicksLeft(@NotNull Player player) {
        return get(player).getTicksLeft(BodyService.getGameTime(player));
    }

    public static void raise(@NotNull ServerPlayer player, float points, int durationTicks) {
        PlayerData.get(player).setMugwortShield(
                new MugwortShieldData(points, points, BodyService.getGameTime(player) + durationTicks));
        tickShield(player);
    }

    public static float absorb(@NotNull ServerPlayer player, @NotNull DamageSource source, float amount) {
        if (amount <= 0.0F || !isUp(player) || !SkinService.canTurn(source)) return amount;

        MugwortShieldData shield = get(player);
        float taken = Math.min(shield.points(), amount);
        float left = shield.points() - taken;
        if (left > 0.0F) {
            PlayerData.get(player).setMugwortShield(shield.withPoints(left));
        } else {
            PlayerData.get(player).setMugwortShield(MugwortShieldData.DEFAULT);
            player.removeEffect(ModEffects.HEAVENLY_MUGWORT_GU);
        }
        return amount - taken;
    }

    public static void tickShield(@NotNull ServerPlayer player) {
        Holder<MobEffect> effect = ModEffects.HEAVENLY_MUGWORT_GU;
        if (isUp(player)) {
            player.addEffect(ModEffects.instance(effect, Math.max(1, (int) getTicksLeft(player))));
            return;
        }
        PlayerData.get(player).setMugwortShield(MugwortShieldData.DEFAULT);
        if (player.hasEffect(effect)) player.removeEffect(effect);
    }
}
