package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
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
 * 范围冲击[Area Impact]服务：空手收尾招追加的身前锥形冲击波，以及各类力道出手共用的溢出结算。
 * <ul>
 *   <li>每个连招轮[Combo Round]只在收尾招掷一次（{@link #rolls}，由 {@link ComboRound#rollsImpact} 调用，挥空同样判
 *       定）；触发概率仅取决于状态（全力以赴为 {@link #ALL_OUT_CHANCE}、力气蛊为 {@link #STRENGTH_QI_CHANCE}、两者并存为
 *       {@link #BOTH_CHANCE}），与力量点无关；人力钧力流[Human Jun Strength]在苦力蛊[Hardship Strength Gu]运行中
 *       （{@link #isWidened}）必定触发。</li>
 *   <li>作用半径 {@link #getRange} 由本击结算力量点[Strength Points]（{@link #getPoints}，手持物品时为 {@code 0}）按平
 *       方根增长，封顶于 {@link #MAX_RANGE}；{@link #isWidened} 时半径乘 {@link #WIDENED_RANGE_FACTOR}、锥体半角由
 *       {@link #HALF_ANGLE_DEGREES} 放宽到 {@link #WIDENED_HALF_ANGLE_DEGREES}（崩裂范围不放大）。</li>
 *   <li>溢出结算（{@link #spill}）：目标按给定顺序依次吃剩余伤害，每命中一个就从剩余里扣掉该目标挨打前的生命
 *       （{@link #getSpill}，不看护甲，下一个目标照常吃护甲），扣完或满 {@code maxTargets} 个为止，免疫伤害的目标不占
 *       名额也不扣剩余；蓄力一拳[Charged Punch]扇形、远程拳爆开与终点破坏打击[Crash Strike]共用。</li>
 *   <li>{@code EpicFightIntegration} 在近战收尾招最后一段判定结束时调 {@link #release}：从眼部沿视线发出锥形冲击，排
 *       除本拳已判定目标，剩余伤害取 {@link ComboRound#getSpill}（主目标已吃整拳后的溢出量；没有主目标时为整拳伤害，
 *       由最近的目标吃起），由近及远至多 {@link #MAX_TARGETS} 个，命中后经 {@link ComboRound#settleOverload} 结算过载反
 *       噬（这一招近战已结算过则不重复），随后调 {@link AreaImpactShatter} 崩裂前方地形。收尾招化作远程拳时不放锥
 *       形，由 {@code QiWaveService} 把爆开半径扩到本半径。</li>
 *   <li>目标碰撞箱中心须落在锥体内（{@link #isInCone}），且从起点到目标眼部无方块碰撞遮挡（{@link #isInSight}）；伤害
 *       为 {@code guzhenren:area_impact} 普通战斗伤害（受护甲减免，带 {@code NO_KNOCKBACK} 标签以避免叠加原版击退），
 *       实际造成伤害后沿锥轴施加 {@link #KNOCKBACK} 击退。</li>
 *   <li>排除目标规则与二段飞行[Second-Stage Flight]冲撞一致（自身、同队盟友、己方宠物、野生蛊、盔甲架），并额外排除同乘
 *       载具的实体。</li>
 * </ul>
 */

public final class AreaImpact {

    private AreaImpact() {}

    public static final double BASE_RANGE = 3.0D;
    public static final double RANGE_PER_ROOT_POINT = 0.4D;
    public static final double MAX_RANGE = 12.0D;
    public static final double HALF_ANGLE_DEGREES = 45.0D;
    public static final double WIDENED_RANGE_FACTOR = 1.5D;
    public static final double WIDENED_HALF_ANGLE_DEGREES = 135.0D;
    public static final double KNOCKBACK = 0.5D;
    public static final int MAX_TARGETS = 8;
    public static final float ALL_OUT_CHANCE = 0.4F;
    public static final float BOTH_CHANCE = 0.5F;
    public static final float STRENGTH_QI_CHANCE = 0.1F;
    private static final double MIN_LENGTH_SQR = 1.0E-6D;

    public static float getChance(boolean allOut, boolean strengthQi) {
        if (allOut) return strengthQi ? BOTH_CHANCE : ALL_OUT_CHANCE;
        return strengthQi ? STRENGTH_QI_CHANCE : 0.0F;
    }

    public static float getChance(@NotNull Player player) {
        return getChance(StrengthService.isAllOut(player), player.hasEffect(ModEffects.STRENGTH_QI_GU));
    }

    public static boolean isWidened(@NotNull Player player) {
        return HardshipStrength.isActive(player)
                && PathStrengthService.getHeldBranch(player) == StrengthPathBranch.HUMAN_JUN_STRENGTH;
    }

    public static boolean rolls(@NotNull Player player, @NotNull RandomSource random) {
        if (isWidened(player)) return true;
        float chance = getChance(player);
        return chance > 0.0F && random.nextFloat() < chance;
    }

    public static int getPoints(@NotNull Player player) {
        return AttackDamageService.isFistHand(player) ? StrengthService.getSettledPoints(player) : 0;
    }

    public static double getRange(int points) {
        return Math.min(MAX_RANGE, BASE_RANGE + RANGE_PER_ROOT_POINT * Math.sqrt(Math.max(0, points)));
    }

    public static double getRange(int points, boolean widened) {
        return widened ? getRange(points) * WIDENED_RANGE_FACTOR : getRange(points);
    }

    public static double getHalfAngle(boolean widened) {
        return widened ? WIDENED_HALF_ANGLE_DEGREES : HALF_ANGLE_DEGREES;
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
        boolean widened = isWidened(player);
        double range = getRange(points, widened);
        double halfAngle = getHalfAngle(widened);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(origin, origin).inflate(range),
                target -> !spared.contains(target) && canHit(player, target)
                        && isInCone(origin, axis, range, halfAngle, target.getBoundingBox().getCenter())
                        && isInSight(player, origin, target));
        targets.sort(Comparator.comparingDouble(target -> target.getBoundingBox().getCenter().distanceToSqr(origin)));

        float damage = ComboRound.getSpill(player, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        List<LivingEntity> landed = spill(targets, damage, MAX_TARGETS,
                ModDamageTypes.source(player, player, ModDamageTypes.AREA_IMPACT), target -> {
                    target.knockback(KNOCKBACK, -axis.x, -axis.z);
                    target.hurtMarked = true;
                });
        if (!landed.isEmpty()) ComboRound.settleOverload(player);
        if (AreaImpactShatter.canShatter(points)) {
            AreaImpactShatter.shatter(player, origin,
                    AreaImpactShatter.getConeCells(origin, axis, getRange(points) / 2.0D), points,
                    AreaImpactShatter.CONE_MAX_BLOCKS);
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
