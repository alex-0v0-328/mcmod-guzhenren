package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
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
 * 范围冲击[Area Impact]服务：空手拳击追加的身前锥形冲击波，或力气蛊[Strength Qi Gu]远程气波爆开时追加的球形冲击波。
 * <ul>
 *   <li>每个连招轮[Combo Round]只在收尾招掷一次（{@link #rolls}，由 {@link ComboRound#rollsImpact} 调用，挥空同样判
 *       定）；触发概率仅取决于状态（全力以赴为 {@link #ALL_OUT_CHANCE}、力气蛊为 {@link #STRENGTH_QI_CHANCE}、两者并存为
 *       {@link #BOTH_CHANCE}），与力量点无关。</li>
 *   <li>作用半径 {@link #getRange} 由本击可发挥力量点[Strength Points]（{@link #getPoints}，手持物品时为 {@code 0}）按
 *       平方根增长，封顶于 {@link #MAX_RANGE}。</li>
 *   <li>{@code EpicFightIntegration} 在近战收尾招最后一段判定结束时调 {@link #release}（从眼部沿视线发出锥形冲击，排
 *       除本拳已判定目标，随后调 {@link AreaImpactShatter} 崩裂前方地形）；收尾招化作远程拳时改由 {@code QiBurst} 在爆开
 *       时调 {@link #burst}（以爆点为中心发出球形冲击，排除气波直击目标，不破方块）。</li>
 *   <li>目标碰撞箱中心须落在锥体（{@link #isInCone}）或球体内，且从起点到目标眼部无方块碰撞遮挡（{@link #isInSight}）；
 *       按由近及远顺序最多命中 {@link #MAX_TARGETS} 个目标（免疫伤害的目标不占名额）。</li>
 *   <li>伤害为当前攻击力面板 × {@link #DAMAGE_RATIO}（{@code guzhenren:area_impact} 普通战斗伤害，受护甲减免，带
 *       {@code NO_KNOCKBACK} 标签以避免叠加原版击退），实际造成伤害后沿锥轴或球心向外施加 {@link #KNOCKBACK} 击
 *       退。</li>
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

    public static int getPoints(@NotNull Player player) {
        return AttackDamageService.isFistHand(player) ? StrengthService.getUsablePoints(player) : 0;
    }

    public static double getRange(int points) {
        return Math.min(MAX_RANGE, BASE_RANGE + RANGE_PER_ROOT_POINT * Math.sqrt(Math.max(0, points)));
    }

    public static boolean isInCone(Vec3 origin, Vec3 axis, double range, Vec3 point) {
        Vec3 offset = point.subtract(origin);
        double distanceSqr = offset.lengthSqr();
        if (distanceSqr > range * range) return false;
        if (distanceSqr < MIN_LENGTH_SQR) return true;

        return offset.dot(axis) >= MIN_COS * Math.sqrt(distanceSqr);
    }

    public static float getDamage(double attackDamage) { return (float) attackDamage * DAMAGE_RATIO; }

    public static List<LivingEntity> release(@NotNull ServerPlayer player, @NotNull Vec3 look,
                                             @NotNull Collection<? extends Entity> spared) {
        if (look.lengthSqr() < MIN_LENGTH_SQR) return List.of();

        Vec3 origin = player.getEyePosition();
        Vec3 axis = look.normalize();
        int points = getPoints(player);
        double range = getRange(points);
        List<LivingEntity> landed = strike(player, origin, range, spared,
                center -> isInCone(origin, axis, range, center), center -> axis);
        if (AreaImpactShatter.canShatter(points)) {
            AreaImpactShatter.shatter(player, origin, AreaImpactShatter.getConeCells(origin, axis, range / 2.0D),
                    points, AreaImpactShatter.CONE_MAX_BLOCKS);
        }
        return landed;
    }

    public static List<LivingEntity> burst(@NotNull ServerPlayer player, @NotNull Vec3 center,
                                           @NotNull Collection<? extends Entity> spared) {
        double radius = getRange(getPoints(player));
        return strike(player, center, radius, spared,
                at -> at.distanceToSqr(center) <= radius * radius, at -> at.subtract(center));
    }

    private static List<LivingEntity> strike(ServerPlayer player, Vec3 origin, double range,
                                             Collection<? extends Entity> spared, Predicate<Vec3> inside,
                                             Function<Vec3, Vec3> push) {
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(origin, origin).inflate(range),
                target -> !spared.contains(target) && canHit(player, target)
                        && inside.test(target.getBoundingBox().getCenter())
                        && isInSight(player, origin, target));
        targets.sort(Comparator.comparingDouble(target -> target.getBoundingBox().getCenter().distanceToSqr(origin)));

        DamageSource source = ModDamageTypes.source(player, player, ModDamageTypes.AREA_IMPACT);
        float damage = getDamage(player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        List<LivingEntity> landed = new ArrayList<>();
        for (LivingEntity target : targets) {
            if (landed.size() >= MAX_TARGETS) break;
            if (!target.hurt(source, damage)) continue;
            Vec3 away = push.apply(target.getBoundingBox().getCenter());
            target.knockback(KNOCKBACK, -away.x, -away.z);
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

    public static boolean isInSight(@NotNull Entity viewer, @NotNull Vec3 origin, @NotNull LivingEntity target) {
        Vec3 eyes = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        return viewer.level().clip(new ClipContext(origin, eyes, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                viewer)).getType() == HitResult.Type.MISS;
    }
}
