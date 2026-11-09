package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * The area impact [范围冲击]: a fist strike, or the burst of a Strength Qi Gu [力气蛊] ranged punch, may also hit what
 * stands in a cone ahead of it, even when the strike itself whiffs. Each strike phase and each burst rolls once
 * ({@link #rolls}) against {@link #getChance}, which the running effects alone set, never the strength points:
 * {@link #ALL_OUT_CHANCE} under All-Out Effort [全力以赴] ({@link StrengthService#isAllOut}), {@link #BOTH_CHANCE}
 * with the Strength Qi Gu's effect beside it, {@link #STRENGTH_QI_CHANCE} under the Strength Qi Gu alone, none
 * without either. The roll reads the vanilla effects and draws from the random it is handed; a missed roll leaves
 * no trace.
 *
 * <p>{@code EpicFightIntegration} rolls when the hit window of an Epic Fight combo, dash or air attack closes and
 * releases the cone from the eyes along the look, sparing every target the fist already tried; {@code QiBurst} rolls
 * on the projectile's random and releases it from the burst point along the projectile's last flight direction,
 * sparing every target the burst hit. This class knows nothing of Epic Fight.
 *
 * <p>{@link #release} opens the cone from its origin along its axis: a target's hitbox center within {@link #RANGE}
 * blocks and {@link #HALF_ANGLE_DEGREES} off the axis, in line of sight from the origin (a block clip to its eyes,
 * as vanilla's {@code hasLineOfSight} clips from the eyes), nearest first until {@link #MAX_TARGETS} have taken the
 * hit, so a nearer body that refuses it does not use up the cap. Each takes {@link #DAMAGE_RATIO} of the attacker's
 * attack panel as {@code AREA_IMPACT}, ordinary combat damage that armor stops, and a hit that lands pushes it
 * {@link #KNOCKBACK} along the axis through vanilla {@code knockback}, which scales by the knockback resistance;
 * the type carries {@code NO_KNOCKBACK}, so vanilla's own shove away from the attacker stays off. A refused hit (no
 * PvP, invulnerable, still flinching) moves nothing. Spared like the second stage ram, plus whatever rides with the
 * attacker: itself, allies, its own pets, wild Gu and armor stands. No block is touched.
 *
 * <p>⚠ {@link #ALL_OUT_CHANCE}, {@link #BOTH_CHANCE} and {@link #STRENGTH_QI_CHANCE} are provisional.
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthService
 * @since 1.0.0
 */

public final class AreaImpact {

    private AreaImpact() {}

    public static final double RANGE = 5.0D;
    public static final double HALF_ANGLE_DEGREES = 45.0D;
    public static final float DAMAGE_RATIO = 0.5F;
    public static final double KNOCKBACK = 0.5D;
    public static final int MAX_TARGETS = 8;
    public static final float ALL_OUT_CHANCE = 0.4F;
    public static final float BOTH_CHANCE = 0.5F;
    public static final float STRENGTH_QI_CHANCE = 0.1F;
    private static final double MIN_COS = Math.cos(Math.toRadians(HALF_ANGLE_DEGREES));
    private static final double MIN_LENGTH_SQR = 1.0E-6D;

    public static float getChance(boolean allOut, boolean strengthQi) {
        if (allOut) return strengthQi ? BOTH_CHANCE : ALL_OUT_CHANCE;
        return strengthQi ? STRENGTH_QI_CHANCE : 0.0F;
    }

    public static float getChance(@NotNull Player player) {
        return getChance(StrengthService.isAllOut(player), player.hasEffect(ModEffects.STRENGTH_QI_GU));
    }

    public static boolean rolls(@NotNull Player player, @NotNull RandomSource random) {
        float chance = getChance(player);
        return chance > 0.0F && random.nextFloat() < chance;
    }

    /** Whether {@code point} lies in the cone from {@code origin} along {@code axis}, a unit vector. */
    public static boolean isInCone(Vec3 origin, Vec3 axis, Vec3 point) {
        Vec3 offset = point.subtract(origin);
        double distanceSqr = offset.lengthSqr();
        if (distanceSqr > RANGE * RANGE) return false;
        if (distanceSqr < MIN_LENGTH_SQR) return true;

        return offset.dot(axis) >= MIN_COS * Math.sqrt(distanceSqr);
    }

    public static float getDamage(double attackDamage) { return (float) attackDamage * DAMAGE_RATIO; }

    public static List<LivingEntity> release(@NotNull ServerPlayer player, @NotNull Vec3 look,
                                             @NotNull Collection<? extends Entity> spared) {
        return release(player, player.getEyePosition(), look, spared);
    }

    public static List<LivingEntity> release(@NotNull ServerPlayer player, @NotNull Vec3 origin, @NotNull Vec3 look,
                                             @NotNull Collection<? extends Entity> spared) {
        if (look.lengthSqr() < MIN_LENGTH_SQR) return List.of();

        Vec3 axis = look.normalize();
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(origin, origin).inflate(RANGE),
                target -> !spared.contains(target) && canHit(player, target)
                        && isInCone(origin, axis, target.getBoundingBox().getCenter())
                        && isInSight(player, origin, target));
        targets.sort(Comparator.comparingDouble(target -> target.getBoundingBox().getCenter().distanceToSqr(origin)));

        DamageSource source = ModDamageTypes.source(player, player, ModDamageTypes.AREA_IMPACT);
        float damage = getDamage(player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        List<LivingEntity> landed = new ArrayList<>();
        for (LivingEntity target : targets) {
            if (landed.size() >= MAX_TARGETS) break;
            if (!target.hurt(source, damage)) continue;
            target.knockback(KNOCKBACK, -axis.x, -axis.z);
            target.hurtMarked = true;
            landed.add(target);
        }
        return landed;
    }

    public static boolean canHit(@NotNull ServerPlayer player, @NotNull LivingEntity target) {
        return target != player && target.isAlive() && !target.isSpectator() && target.isPickable()
                && !player.isAlliedTo(target) && !target.isAlliedTo(player)
                && !player.isPassengerOfSameVehicle(target)
                && !(target instanceof OwnableEntity owned && player.equals(owned.getOwner()))
                && !target.getType().is(ModEntityTypeTags.WILD_GU) && !(target instanceof ArmorStand);
    }

    private static boolean isInSight(ServerPlayer player, Vec3 origin, LivingEntity target) {
        Vec3 eyes = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        return player.level().clip(new ClipContext(origin, eyes, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                player)).getType() == HitResult.Type.MISS;
    }
}
