package net.alex.guzhenren.item.gu.mortal.path.strength;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.QiBurst;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.gameplay.path.strength.AreaImpactShatter;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastChargeDamage;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthRound;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthSnapshot;
import net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService;
import net.alex.guzhenren.particle.RingConeEmitter;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
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
 * 蓄力蛊[Store Strength Gu]蓄满后放出的蓄力一拳。
 * <ul>
 *   <li>{@link StoreStrength#release} 计数后触发；按 {@link StoreStrength#getMaxPoints} 出拳，可超当前承受力
 *       （{@link AttackDamageService#getFistDamage}）。</li>
 *   <li>兽力虚影流随机解放一个持有兽种，苦力期间全解放，虚影显在运行中轮次的虚影旁。</li>
 *   <li>{@link #getTarget} 重验客户端给的目标（存活、可打、在视线前、距眼 {@link #MAX_TARGET_DISTANCE} 格内），伪造 id
 *       不能把一击转到身后或伸到远处。</li>
 *   <li>力气蛊[Strength Qi Gu]效果在身、目标在触及外（同远程拳）、可见且可被投射物命中时远程出手（{@link #shoot}）：
 *       每个能出形的解放兽种沿弧线发一道兽力冲击，都不能则按远程拳选法随机发一道能出形的持有兽种；人力钧力流或无此类
 *       兽种者发一道带拳全伤的追踪气波；选法见 {@code QiWaveService.getChargeFamilies}。</li>
 *   <li>冲击按本兽种力量命中（{@link BeastChargeDamage}）：解放的为永久力量点[Strength Points] ×
 *       {@link StoreStrength#CHARGED_RELEASE_FACTOR}，随机的 ×1，不截承受力；各在落点按最大点数
 *       {@link StoreStrength#getBurstRadius} 爆开，目标全额、余者 {@link QiBurst#SECONDARY_RATIO}，无范围冲击、
 *       不破方块。</li>
 *   <li>否则挥扇 {@link #swing}：水平朝保留目标（无则视线），射程按最大点数 {@link StoreStrength#getRange}，扇内
 *       可见可打者由近到远至多 {@link #MAX_TARGETS} 个，保留目标居首全额，余者 {@link #SECONDARY_RATIO}，各从脚下
 *       击退 {@link #KNOCKBACK}；打完在半个射程内崩裂地形（{@link AreaImpactShatter}）；挥空照落，总沿扇轴发一道
 *       范围冲击环到射程（{@link RingConeEmitter#areaImpactRing}）。</li>
 *   <li>无论打中什么都有冲击反噬：超承受力的点数经 {@link BodyOverload#recoil} 结算；该伤害类型不开轮次、不付肉体过载
 *       （{@code BeastPhantomEvents}）。</li>
 *   <li>⚠ {@link #SECONDARY_RATIO}、{@link #MAX_TARGETS}、{@link #KNOCKBACK}、{@link #MAX_TARGET_DISTANCE} 暂定；
 *       末项高于 Epic Fight 默认锁定距离。</li>
 * </ul>
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
            shoot(player, target, damage, BeastChargeDamage.getDamages(data, charging, released,
                    StoreStrength.CHARGED_RELEASE_FACTOR, BeastChargeDamage.UNCAPPED), charging,
                    StoreStrength.getBurstRadius(maxPoints, widened));
        } else {
            swing(player, target, damage, maxPoints, widened);
        }
        BodyOverload.recoil(player, StoreStrength.getBacklashPoints(maxPoints, snapshot.capacity()),
                snapshot.capacity(), ModDamageTypes.IMPACT_BACKLASH);
    }

    private static boolean isRanged(@NotNull ServerPlayer player, @NotNull LivingEntity target) {
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
    private static void shoot(ServerPlayer player, LivingEntity target, float damage, float[] chargeDamages,
                              List<BeastStrengthFamily> charging, double radius) {
        QiBurst.Spec burst = new QiBurst.Spec(radius, ModDamageTypes.CHARGED_PUNCH, false);
        if (charging.isEmpty()) {
            QiWaveEntity.track(player, target, damage, burst);
            return;
        }
        for (int i = 0; i < charging.size(); i++) {
            BeastChargeEntity.shoot(player, target, chargeDamages[i], charging.get(i), burst, i, charging.size());
        }
    }
    //endregion

    //region Melee [近战]
    private static void swing(ServerPlayer player, @Nullable LivingEntity locked, float damage, int maxPoints,
                              boolean widened) {
        Vec3 feet = player.position();
        Vec3 axis = StoreStrength.getLevelAxis(locked != null ? locked.position().subtract(feet)
                : player.getLookAngle());
        if (axis == Vec3.ZERO) axis = StoreStrength.getLevelAxis(Vec3.directionFromRotation(0.0F, player.getYRot()));
        double range = StoreStrength.getRange(maxPoints, widened);
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
        if (AreaImpactShatter.canShatter(maxPoints)) {
            Vec3 eye = player.getEyePosition();
            AreaImpactShatter.shatter(player, eye, AreaImpactShatter.getFanCells(feet, player.getBbHeight(), axis,
                    range / 2.0D, halfAngle, eye), maxPoints, AreaImpactShatter.FAN_MAX_BLOCKS);
        }
        showSwing(player, axis, range);
    }

    private static void showSwing(ServerPlayer player, Vec3 axis, double range) {
        ServerLevel level = player.serverLevel();
        RingConeEmitter.areaImpactRing(level, player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D), axis,
                range);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }
    //endregion
}
