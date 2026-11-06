package net.alex.guzhenren.item.gu.mortal.path.qi;

import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.entity.projectile.MoonBladeArc;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Strength Qi Gu's [力气蛊] ranged punch: while its buff runs, an empty-hand punch at a living target past the
 * vanilla reach and within {@link StrengthQiGuEffect#RANGE} blocks throws one {@link QiWaveEntity} at it. The client
 * picks the target (Epic Fight's lock-on, else the crosshair) and sends its id; {@link #fire} checks it all again.
 *
 * <p>{@link #isInWaveBand} is the one range rule, read by both ends: a target inside the reach is left to the plain
 * melee hit, so one click never lands twice. The wave hits as hard as the punch would ({@link #getDamage}): the
 * player's attack damage, which is where the strength path's bonus lives, scaled by the attack charge the vanilla way,
 * and the charge restarts with every wave. A charge under {@link #MIN_CHARGE} throws nothing, which bounds a forged
 * click stream to about one wave a tick and a half.
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthQiGuEffect
 * @since 1.0.0
 */

public final class QiWaveService {

    private QiWaveService() {}

    /** How far past {@link StrengthQiGuEffect#RANGE} the server still accepts a target, for a tick or two of motion. */
    public static final double RANGE_SLACK = 2.0D;
    public static final float MIN_CHARGE = 0.25F;

    /** Whether a target {@code distance} blocks from the eye (to its box) is a wave's: past the reach, within range. */
    public static boolean isInWaveBand(double distance, double reach, double range) {
        return distance > reach && distance <= range;
    }

    /** The vanilla charge curve of a punch: a fifth of the damage at no charge, all of it at full charge. */
    public static float getDamage(double attackDamage, float charge) {
        float clamped = Math.max(0.0F, Math.min(1.0F, charge));
        return (float) attackDamage * (0.2F + clamped * clamped * 0.8F);
    }

    /** What both ends ask of the puncher before a target: the main hand empty and the body able to punch. */
    public static boolean canPunch(@NotNull Player player) {
        return player.getMainHandItem().isEmpty() && player.isAlive() && !player.isSpectator();
    }

    public static double getBoxDistance(@NotNull Player player, @NotNull LivingEntity target) {
        return Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition()));
    }

    /** Throws the wave at the client's target, or does nothing when any check fails; the wave thrown, or null. */
    public static @Nullable QiWaveEntity fire(@NotNull ServerPlayer player, int targetId) {
        if (!canPunch(player) || !TimedEffects.hasBuff(player, ModEffects.STRENGTH_QI_GU)) return null;
        if (!(player.level().getEntity(targetId) instanceof LivingEntity target) || target == player) return null;
        if (!target.isAlive() || !target.canBeHitByProjectile()) return null;

        double range = StrengthQiGuEffect.RANGE + RANGE_SLACK;
        if (!isInWaveBand(getBoxDistance(player, target), player.entityInteractionRange(), range)) return null;
        Vec3 center = target.getBoundingBox().getCenter();
        if (!MoonBladeArc.isAimable(player.getEyePosition(), player.getLookAngle(), center, range)) return null;
        if (!player.hasLineOfSight(target)) return null;

        float charge = player.getAttackStrengthScale(0.5F);
        if (charge < MIN_CHARGE) return null;
        float damage = getDamage(player.getAttributeValue(Attributes.ATTACK_DAMAGE), charge);
        player.resetAttackStrengthTicker();
        return QiWaveEntity.shoot(player, target, damage);
    }
}
