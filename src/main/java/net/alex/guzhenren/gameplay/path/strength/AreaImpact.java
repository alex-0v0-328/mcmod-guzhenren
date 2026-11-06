package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.alex.guzhenren.entity.gu.WildGuEntity;
import net.alex.guzhenren.gameplay.attribute.StrengthService;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * The area impact [范围冲击]: once a body brings {@link #THRESHOLD} strength points to bear
 * ({@link StrengthService#getUsablePoints}, {@code min(points, current capacity)}), every fist strike also hits what
 * stands in a cone ahead of it, even when the fist itself whiffs. {@code EpicFightIntegration} calls
 * {@link #release} when the hit window of an Epic Fight combo, dash or air attack closes, handing in every target
 * the fist already tried, which the impact spares; this class knows nothing of Epic Fight.
 *
 * <p>The cone opens from the eyes along the look: a target's hitbox center within {@link #RANGE} blocks and
 * {@link #HALF_ANGLE_DEGREES} off the axis, in line of sight, nearest first until {@link #MAX_TARGETS} have taken
 * the hit, so a nearer body that refuses it does not use up the cap. Each takes {@link #DAMAGE_RATIO} of the
 * attack panel as {@code AREA_IMPACT}, ordinary combat damage that armor stops, and a hit that lands pushes it
 * {@link #KNOCKBACK} along the axis through vanilla {@code knockback}, which scales by the knockback resistance;
 * the type carries {@code NO_KNOCKBACK}, so vanilla's own shove away from the attacker stays off. A refused hit (no PvP, invulnerable, still flinching) moves nothing. Spared like the second stage ram, plus
 * whatever rides with the attacker: itself, allies, its own pets, wild Gu and armor stands. No block is touched.
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthService
 * @since 1.0.0
 */

public final class AreaImpact {

    private AreaImpact() {}

    public static final int THRESHOLD = 72;
    public static final double RANGE = 5.0D;
    public static final double HALF_ANGLE_DEGREES = 45.0D;
    public static final float DAMAGE_RATIO = 0.5F;
    public static final double KNOCKBACK = 0.5D;
    public static final int MAX_TARGETS = 8;
    private static final double MIN_COS = Math.cos(Math.toRadians(HALF_ANGLE_DEGREES));
    private static final double MIN_LENGTH_SQR = 1.0E-6D;

    public static boolean isUnlocked(@NotNull Player player) {
        return isUnlocked(StrengthService.getUsablePoints(player));
    }

    public static boolean isUnlocked(int usablePoints) { return usablePoints >= THRESHOLD; }

    /** Whether {@code point} lies in the cone from {@code origin} along {@code axis}, a unit vector. */
    public static boolean isInCone(Vec3 origin, Vec3 axis, Vec3 point) {
        Vec3 offset = point.subtract(origin);
        double distanceSqr = offset.lengthSqr();
        if (distanceSqr > RANGE * RANGE) return false;
        if (distanceSqr < MIN_LENGTH_SQR) return true;

        return offset.dot(axis) >= MIN_COS * Math.sqrt(distanceSqr);
    }

    public static float getDamage(double attackDamage) { return (float) attackDamage * DAMAGE_RATIO; }

    /** Hits the cone ahead of {@code look} and returns the targets the impact landed on, nearest first. */
    public static List<LivingEntity> release(@NotNull ServerPlayer player, @NotNull Vec3 look,
                                             @NotNull Collection<? extends Entity> spared) {
        if (look.lengthSqr() < MIN_LENGTH_SQR || !isUnlocked(player)) return List.of();

        Vec3 axis = look.normalize();
        Vec3 eye = player.getEyePosition();
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, eye).inflate(RANGE),
                target -> !spared.contains(target) && canHit(player, target)
                        && isInCone(eye, axis, target.getBoundingBox().getCenter())
                        && player.hasLineOfSight(target));
        targets.sort(Comparator.comparingDouble(target -> target.getBoundingBox().getCenter().distanceToSqr(eye)));

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
                && !(target instanceof WildGuEntity) && !(target instanceof ArmorStand);
    }
}
