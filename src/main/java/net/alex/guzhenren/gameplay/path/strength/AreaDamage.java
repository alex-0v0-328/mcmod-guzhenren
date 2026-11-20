package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.minecraft.server.level.ServerPlayer;
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
 * 范围伤害[Area Damage]服务：人力钧力流[Human Jun Strength]暴击[Crit]时空手收尾招追加的身前锥形伤害，以及各类力道出手
 * 共用的溢出结算。
 * <ul>
 *   <li>是否放由连招轮[Combo Round]决定（{@link ComboRound#rollsArea}：人力流且本轮暴击，挥空同样判定）；兽力虚影流
 *       [Beast Strength Phantom]的收尾招不放锥形。</li>
 *   <li>作用半径 {@link #getRange(int)} 由本击结算力量点[Strength Points]（{@link #getPoints}，手持物品时为 {@code 0}）
 *       按平方根增长；人力流按苦力档位放大（{@link #getWidenTier}，取 {@link HardshipStrength#getTier(Player)}，兽力流恒
 *       为 {@code 0}）：半径乘 {@link #getRangeFactor}（{@code 1 + 0.4 × 档}）后封顶 {@link #MAX_RANGE}，锥体半角
 *       {@link #getHalfAngle}（{@code 45° + 5.5° × 档}，5 档扇面 145°）；崩裂范围不放大。</li>
 *   <li>溢出结算（{@link #spill}）：目标按给定顺序依次吃剩余伤害，每命中一个就从剩余里扣掉该目标挨打前的生命
 *       （{@link #getSpill}，不看护甲，下一个目标照常吃护甲），扣完或满 {@code maxTargets} 个为止，免疫伤害的目标不占
 *       名额也不扣剩余；蓄力一拳[Charged Punch]扇形、远程攻击爆开与终点破坏打击[Crash Strike]共用。</li>
 *   <li>{@code EpicFightIntegration} 在近战收尾招最后一段判定结束时调 {@link #release}：从眼部沿视线发出锥形伤害，排
 *       除本拳已判定目标，剩余伤害取 {@link ComboRound#getSpill}（主目标已吃整拳后的溢出量，举盾时先扣格挡；没有主目标
 *       时为整拳伤害，由最近的目标吃起），由近及远至多 {@link #MAX_TARGETS} 个，命中后经
 *       {@link ComboRound#settleOverload} 结算过载反噬（这一招近战已结算过则不重复），随后调
 *       {@link AreaDamageShatter} 崩裂前方地形。收尾招化作远程攻击时不放锥形，由 {@code QiWaveService} 把爆开半径扩到
 *       本半径。</li>
 *   <li>目标碰撞箱中心须落在锥体内（{@link #isInCone}），且从起点到目标眼部无方块碰撞遮挡（{@link #isInSight}）；伤害
 *       为 {@code guzhenren:area_damage} 普通战斗伤害（受护甲减免，带 {@code NO_KNOCKBACK} 标签以避免叠加原版击退），
 *       实际造成伤害后沿锥轴施加 {@link #KNOCKBACK} 击退。</li>
 *   <li>排除目标规则与二段飞行[Second-Stage Flight]冲撞一致（自身、同队盟友、己方宠物、野生蛊、盔甲架），并额外排除同乘
 *       载具的实体。</li>
 * </ul>
 */

public final class AreaDamage {

    private AreaDamage() {}

    public static final double BASE_RANGE = 3.0D;
    public static final double RANGE_PER_ROOT_POINT = 0.4D;
    public static final double MAX_RANGE = 12.0D;
    public static final double HALF_ANGLE_DEGREES = 45.0D;
    public static final double KNOCKBACK = 0.5D;
    public static final int MAX_TARGETS = 8;
    public static final double RANGE_PER_TIER = 0.4D;
    public static final double HALF_ANGLE_PER_TIER = 5.5D;
    private static final double MIN_LENGTH_SQR = 1.0E-6D;

    public static int getWidenTier(@NotNull Player player) {
        return PathStrengthService.getHeldBranch(player) == StrengthPathBranch.HUMAN_JUN_STRENGTH
                ? HardshipStrength.getTier(player) : 0;
    }

    public static int getPoints(@NotNull Player player) {
        return AttackDamageService.isFistHand(player) ? StrengthService.getSettledPoints(player) : 0;
    }

    public static double getRange(int points) {
        return Math.min(MAX_RANGE, BASE_RANGE + RANGE_PER_ROOT_POINT * Math.sqrt(Math.max(0, points)));
    }

    public static double getRangeFactor(int widenTier) { return 1.0D + RANGE_PER_TIER * Math.max(0, widenTier); }

    public static double getRange(int points, int widenTier) {
        return Math.min(MAX_RANGE, getRange(points) * getRangeFactor(widenTier));
    }

    public static double getHalfAngle(int widenTier) {
        return HALF_ANGLE_DEGREES + HALF_ANGLE_PER_TIER * Math.max(0, widenTier);
    }

    public static boolean isInCone(Vec3 origin, Vec3 axis, double range, double halfAngleDegrees, Vec3 point) {
        Vec3 offset = point.subtract(origin);
        double distanceSqr = offset.lengthSqr();
        if (distanceSqr > range * range) return false;
        if (distanceSqr < MIN_LENGTH_SQR) return true;

        return offset.dot(axis) >= Math.cos(Math.toRadians(halfAngleDegrees)) * Math.sqrt(distanceSqr) - 1.0E-9D;
    }

    public static float getSpill(float damage, float health) { return damage - Math.max(0.0F, health); }

    public static @NotNull List<LivingEntity> spill(@NotNull List<? extends LivingEntity> ordered, float damage,
                                                    int maxTargets, @NotNull DamageSource source,
                                                    @NotNull Consumer<LivingEntity> onLanded) {
        List<LivingEntity> landed = new ArrayList<>();
        float left = damage;
        for (LivingEntity target : ordered) {
            if (landed.size() >= maxTargets || left <= 0.0F) break;

            float health = target.getHealth();
            if (!target.hurt(source, left)) continue;
            onLanded.accept(target);
            landed.add(target);
            left = getSpill(left, health);
        }
        return landed;
    }

    public static List<LivingEntity> release(@NotNull ServerPlayer player, @NotNull Vec3 look,
                                             @NotNull Collection<? extends Entity> spared) {
        if (look.lengthSqr() < MIN_LENGTH_SQR) return List.of();

        Vec3 origin = player.getEyePosition();
        Vec3 axis = look.normalize();
        int points = getPoints(player);
        int widenTier = getWidenTier(player);
        double range = getRange(points, widenTier);
        double halfAngle = getHalfAngle(widenTier);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(origin, origin).inflate(range),
                target -> !spared.contains(target) && canHit(player, target)
                        && isInCone(origin, axis, range, halfAngle, target.getBoundingBox().getCenter())
                        && isInSight(player, origin, target));
        targets.sort(Comparator.comparingDouble(target -> target.getBoundingBox().getCenter().distanceToSqr(origin)));

        float damage = ComboRound.getSpill(player, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        List<LivingEntity> landed = spill(targets, damage, MAX_TARGETS,
                ModDamageTypes.source(player, player, ModDamageTypes.AREA_DAMAGE), target -> {
                    target.knockback(KNOCKBACK, -axis.x, -axis.z);
                    target.hurtMarked = true;
                });
        if (!landed.isEmpty()) ComboRound.settleOverload(player);
        if (AreaDamageShatter.canShatter(points)) {
            AreaDamageShatter.shatter(player, origin,
                    AreaDamageShatter.getConeCells(origin, axis, getRange(points) / 2.0D), points,
                    AreaDamageShatter.CONE_MAX_BLOCKS);
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

    public static boolean isInSight(@NotNull Entity viewer, @NotNull Vec3 origin, @NotNull LivingEntity target) {
        Vec3 eyes = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        return viewer.level().clip(new ClipContext(origin, eyes, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                viewer)).getType() == HitResult.Type.MISS;
    }
}
