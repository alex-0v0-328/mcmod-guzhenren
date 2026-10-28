package net.alex.guzhenren.gameplay.body;

import java.util.function.Supplier;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.mixin.LivingEntityAccessor;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The Heavenly Mugwort shield [天蓬护盾]: raised by the Gu, spent by incoming damage before armor and skin, and
 * projected as the {@code HEAVENLY_MUGWORT_GU} effect on the heartbeat.
 *
 * <p>{@link #raise} fills the shield to the Gu's points and restarts its timer; a shield still up is replaced, never
 * stacked. {@link #absorb} takes what it can of a hit and passes the rest on; the hit that empties it ends it and
 * its effect at once. {@link #tickShield} clears a shield whose time ran out and keeps the effect in step with it.
 *
 * <p>The truth is {@code MugwortShieldData}, never the effect: a {@code MobEffect} has no expiry hook, and milk or
 * {@code /effect clear} removes it silently, so the heartbeat re-applies it as {@link SkinService} does a jade
 * skin. ⚠ Damage that bypasses invulnerability or effects ({@code /kill}, the void, starvation) passes untouched.
 * ⚠ The hit arrives before vanilla's invulnerability frames are checked: inside them the shield spends only on what
 * exceeds the hit already taken, so standing in fire does not drain it every tick.
 *
 * @author Alex
 * @version 1.0.0
 * @see MugwortShieldData
 * @since 1.0.0
 */

public final class MugwortShieldService {

    private MugwortShieldService() {}

    private static final int INVULNERABLE_THRESHOLD = 10;

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
        float taken = Math.min(shield.points(), getLanding(player, source, amount));
        if (taken <= 0.0F) return amount;
        float left = shield.points() - taken;
        if (left > 0.0F) {
            PlayerData.get(player).setMugwortShield(shield.withPoints(left));
        } else {
            PlayerData.get(player).setMugwortShield(MugwortShieldData.DEFAULT);
            ModEffects.remove(player, ModEffects.HEAVENLY_MUGWORT_GU);
        }
        return amount - taken;
    }

    /** The part of {@code amount} that lands: inside invulnerability frames, only what exceeds the hit taken. */
    private static float getLanding(LivingEntity entity, DamageSource source, float amount) {
        if (entity.invulnerableTime <= INVULNERABLE_THRESHOLD || source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            return amount;
        }
        return Math.max(0.0F, amount - ((LivingEntityAccessor) entity).guzhenren$getLastHurt());
    }

    public static void tickShield(@NotNull ServerPlayer player) {
        Supplier<? extends MobEffect> effect = ModEffects.HEAVENLY_MUGWORT_GU;
        if (isUp(player)) {
            player.addEffect(ModEffects.instance(effect, Math.max(1, (int) getTicksLeft(player))));
            return;
        }
        PlayerData.get(player).setMugwortShield(MugwortShieldData.DEFAULT);
        if (ModEffects.has(player, effect)) ModEffects.remove(player, effect);
    }
}
