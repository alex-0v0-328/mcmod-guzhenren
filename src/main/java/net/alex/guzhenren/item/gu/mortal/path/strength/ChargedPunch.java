package net.alex.guzhenren.item.gu.mortal.path.strength;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.QiBurst;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.gameplay.attribute.StrengthService;
import net.alex.guzhenren.gameplay.attribute.StrengthSnapshot;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthRound;
import net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The charged punch [蓄力一拳] a full Store Strength Gu [蓄力蛊] charge lets go ({@link #release} once
 * {@link StoreStrength#release} counts). It brings {@link StoreStrength#getMaxPoints} to bear, past the current
 * capacity: the fist's attack with those points ({@link AttackDamageService#getFistDamage}). On the beast phantom path
 * [兽力虚影流] it releases one held family at random, or every one under Hardship Strength [苦力], and shows their
 * phantoms beside a running round's; the ox never shows.
 *
 * <p>The client names the target; {@link #getTarget} keeps it only when it is alive, {@link AreaImpact#canHit},
 * ahead of the look and within {@link #MAX_TARGET_DISTANCE} blocks of the eyes, so a forged id cannot turn the strike
 * behind the player or reach across the world. With the Strength Qi Gu's [力气蛊] effect on and the target past the
 * reach (the ranged punch's own rule), in sight and open to projectiles, it strikes from afar ({@link #shoot}): one
 * Beast Charge [兽力冲击] per released family that can manifest on its arc, the damage split evenly, or, as the
 * ranged punch picks them ({@code QiWaveService.getChargeFamilies}), one of a held family that can at random when
 * none released can; one tracking Qi Wave [气波] for the human jun path [人力钧力流] and for a body holding no such
 * family; each bursts {@link StoreStrength#getBurstRadius} blocks around where it lands, the target in full and
 * the rest {@link QiBurst#SECONDARY_RATIO}, as a charged punch that rolls no area impact.
 * Otherwise it swings the fan ({@link #swing}): every target {@link StoreStrength#isInFan} along the level way to the
 * kept target (the look without one), in sight of the eyes and {@link AreaImpact#canHit}, nearest first until
 * {@link #MAX_TARGETS} took it, the kept one first in full and the rest {@link #SECONDARY_RATIO}, each pushed
 * {@link #KNOCKBACK} away from the feet. A swing that hits nothing still lands.
 *
 * <p>Whatever it hits, the impact backlash [冲击反噬] follows: the points past the capacity, through
 * {@link BodyOverload#recoil} as {@code guzhenren:impact_backlash}. Its damage type opens no strength round and pays
 * no body overload ({@code BeastPhantomEvents}).
 *
 * <p>⚠ {@link #SECONDARY_RATIO}, {@link #MAX_TARGETS}, {@link #KNOCKBACK}, {@link #ARC_RADIUS} and
 * {@link #MAX_TARGET_DISTANCE} are provisional; the last sits above Epic Fight's default lock-on range.
 *
 * @author Alex
 * @version 1.0.0
 * @see StoreStrengthBehavior
 * @since 1.0.0
 */

public final class ChargedPunch {

    private ChargedPunch() {}

    public static final float SECONDARY_RATIO = 0.5F;
    public static final int MAX_TARGETS = 8;
    public static final double KNOCKBACK = 1.0D;
    public static final double ARC_RADIUS = 3.0D;
    public static final double MAX_TARGET_DISTANCE = 32.0D;

    public static boolean release(@NotNull ServerPlayer player, int targetId) {
        if (!StoreStrength.release(player)) return false;

        strike(player, getTarget(player, targetId));
        return true;
    }

    public static @Nullable LivingEntity getTarget(@NotNull ServerPlayer player, int targetId) {
        if (!(player.level().getEntity(targetId) instanceof LivingEntity target)) return null;
        if (!AreaImpact.canHit(player, target)) return null;

        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toTarget.dot(player.getLookAngle()) <= 0.0D) return null;
        return toTarget.lengthSqr() <= MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE ? target : null;
    }

    public static void strike(@NotNull ServerPlayer player, @Nullable LivingEntity target) {
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        PathStrengthData data = PathStrengthService.get(player);
        StrengthPathBranch branch = PathStrengthService.getHeldBranch(player);
        boolean hardship = HardshipStrength.isActive(player);
        List<BeastStrengthFamily> released = branch == StrengthPathBranch.BEAST_STRENGTH_PHANTOM
                ? StoreStrength.getReleasedFamilies(StrengthRound.getAvailableFamilies(data), hardship,
                player.getRandom())
                : List.of();
        int maxPoints = StoreStrength.getMaxPoints(snapshot.totalPoints(),
                StoreStrength.getReleasedPoints(data, released));
        float damage = (float) AttackDamageService.getFistDamage(player, maxPoints);
        boolean widened = hardship && branch == StrengthPathBranch.HUMAN_JUN_STRENGTH;

        if (!released.isEmpty()) BeastPhantomEntity.show(player, getShownFamilies(player, released));
        if (target != null && isRanged(player, target)) {
            List<BeastStrengthFamily> charging = QiWaveService.getChargeFamilies(
                    branch == StrengthPathBranch.BEAST_STRENGTH_PHANTOM, released,
                    StrengthRound.getAvailableFamilies(data), player.getRandom());
            shoot(player, target, damage, charging, widened);
        } else {
            swing(player, target, damage, widened);
        }
        BodyOverload.recoil(player, StoreStrength.getBacklashPoints(maxPoints, snapshot.capacity()),
                snapshot.capacity(), ModDamageTypes.IMPACT_BACKLASH);
    }

    public static boolean isRanged(@NotNull ServerPlayer player, @NotNull LivingEntity target) {
        return player.hasEffect(ModEffects.STRENGTH_QI_GU) && target.canBeHitByProjectile()
                && QiWaveService.isPastReach(QiWaveService.getBoxDistance(player, target),
                player.entityInteractionRange())
                && player.hasLineOfSight(target);
    }

    public static float getDamage(float damage, boolean locked) { return locked ? damage : damage * SECONDARY_RATIO; }

    private static List<BeastStrengthFamily> getShownFamilies(ServerPlayer player, List<BeastStrengthFamily> released) {
        List<BeastStrengthFamily> shown = new ArrayList<>(StrengthRound.getFamilies(player));
        for (BeastStrengthFamily family : released) {
            if (!shown.contains(family)) shown.add(family);
        }
        return shown;
    }

    //region Ranged [远程]
    private static void shoot(ServerPlayer player, LivingEntity target, float damage,
                              List<BeastStrengthFamily> charging, boolean widened) {
        QiBurst.Spec burst = new QiBurst.Spec(StoreStrength.getBurstRadius(widened), ModDamageTypes.CHARGED_PUNCH,
                false);
        if (charging.isEmpty()) {
            QiWaveEntity.track(player, target, damage, burst);
            return;
        }
        for (BeastStrengthFamily family : charging) {
            BeastChargeEntity.shoot(player, target, damage / charging.size(), family, burst);
        }
    }
    //endregion

    //region Melee [近战]
    private static void swing(ServerPlayer player, @Nullable LivingEntity locked, float damage, boolean widened) {
        Vec3 feet = player.position();
        Vec3 axis = StoreStrength.getLevelAxis(locked != null ? locked.position().subtract(feet)
                : player.getLookAngle());
        if (axis == Vec3.ZERO) axis = StoreStrength.getLevelAxis(Vec3.directionFromRotation(0.0F, player.getYRot()));
        double range = StoreStrength.getRange(widened);
        double halfAngle = StoreStrength.getHalfAngle(widened);

        Vec3 fanAxis = axis;
        List<LivingEntity> targets = new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range + 2.0D, StoreStrength.ABOVE + StoreStrength.BELOW,
                        range + 2.0D),
                target -> AreaImpact.canHit(player, target)
                        && StoreStrength.isInFan(feet, player.getBbHeight(), fanAxis, range, halfAngle,
                        target.getBoundingBox())
                        && player.hasLineOfSight(target)));
        targets.sort(Comparator.comparingDouble((LivingEntity target) -> target == locked ? -1.0D
                : target.distanceToSqr(feet)));

        DamageSource source = ModDamageTypes.source(player, player, ModDamageTypes.CHARGED_PUNCH);
        int landed = 0;
        for (LivingEntity target : targets) {
            if (landed >= MAX_TARGETS) break;
            if (!target.hurt(source, getDamage(damage, target == locked))) continue;

            Vec3 push = StoreStrength.getLevelAxis(target.position().subtract(feet));
            if (push == Vec3.ZERO) push = axis;
            target.knockback(KNOCKBACK, -push.x, -push.z);
            target.hurtMarked = true;
            landed++;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        showSwing(player, axis, halfAngle);
    }

    private static void showSwing(ServerPlayer player, Vec3 axis, double halfAngle) {
        ServerLevel level = player.serverLevel();
        int points = StoreStrength.getArcPoints(halfAngle);
        double y = player.getY() + player.getBbHeight() * 0.5D;
        for (int i = 0; i < points; i++) {
            double angle = Math.toRadians(-halfAngle + i * StoreStrength.ARC_STEP_DEGREES);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double x = axis.x * cos - axis.z * sin;
            double z = axis.x * sin + axis.z * cos;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + x * ARC_RADIUS, y,
                    player.getZ() + z * ARC_RADIUS, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }
    //endregion
}
