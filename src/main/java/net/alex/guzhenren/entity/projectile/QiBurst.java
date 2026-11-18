package net.alex.guzhenren.entity.projectile;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.particle.RingConeEmitter;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 力气蛊[Strength Qi Gu]与蓄力蛊[Store Strength Gu]远程拳共用的范围爆开结算工具，供 {@link BeastChargeEntity} 与
 * {@link QiWaveEntity} 调用。
 * <ul>
 *   <li>对碰撞箱距爆点不超过 {@link Spec#radius()}、经 {@link AreaImpact#canHit} 放行（排除主人、盟友、宠物、野生蛊与盔
 *       甲架）且爆点至眼部无方块遮挡（{@link AreaImpact#isInSight}，Alex 定，隔墙不受伤）的生物造成伤害：锁定目标承受全
 *       额伤害，其余目标乘以 {@link #SECONDARY_RATIO}；爆开本身不开启新攻击轮次、也不触发身旁兽力虚影。</li>
 *   <li>当 {@link Spec#rollsAreaImpact()} 为真且主人仍在同一维度时，每次爆开以弹射物随机源按主人概率判定一次范围冲击（
 *       {@link AreaImpact#rolls}）：掷中时沿弹射物末段飞行朝向 {@code heading} 发射冲击环，并在爆点触发
 *       {@link AreaImpact#burst}（跳过本次已命中的目标且不破坏方块）。</li>
 *   <li>{@link Spec} 定义爆开半径、伤害类型与是否判定范围冲击：{@link Spec#QI_WAVE} 供力气蛊远程拳使用，蓄力蛊的蓄力一
 *       拳则传入专属规格（使用蓄力一拳伤害类型且不重复判定范围冲击）。</li>
 *   <li>⚠ 气波伤害类型 {@code guzhenren:qi_wave} 必须带有 {@code minecraft:bypasses_cooldown} 标签，否则同一拳发出的多
 *       道兽力冲击会被目标的受击无敌帧吞掉。</li>
 * </ul>
 */

public final class QiBurst {

    private QiBurst() {}

    public static final double RADIUS = 3.0D;
    public static final float SECONDARY_RATIO = 0.5F;

    public static float getDamage(float damage, boolean locked) { return locked ? damage : damage * SECONDARY_RATIO; }

    public record Spec(double radius, @NotNull ResourceKey<DamageType> type, boolean rollsAreaImpact) {

        public static final Spec QI_WAVE = new Spec(RADIUS, ModDamageTypes.QI_WAVE, true);
    }

    public static @NotNull List<LivingEntity> burst(@NotNull ServerPlayer owner, @NotNull Entity projectile,
                                                    @NotNull Vec3 at, @NotNull Vec3 heading,
                                                    @Nullable LivingEntity locked, float damage, @NotNull Spec spec) {
        DamageSource source = ModDamageTypes.source(projectile, owner, spec.type());
        double radius = spec.radius();
        List<LivingEntity> hit = new ArrayList<>();
        AABB area = new AABB(at, at).inflate(radius);
        for (LivingEntity victim : projectile.level().getEntitiesOfClass(LivingEntity.class, area,
                victim -> victim.getBoundingBox().distanceToSqr(at) <= radius * radius
                        && AreaImpact.canHit(owner, victim) && AreaImpact.isInSight(projectile, at, victim))) {
            if (victim.hurt(source, getDamage(damage, victim == locked))) hit.add(victim);
        }
        if (!spec.rollsAreaImpact()) return hit;
        if (owner.level() == projectile.level() && AreaImpact.rolls(owner, projectile.getRandom())) {
            RingConeEmitter.areaImpactRing(owner.serverLevel(), at, heading,
                    AreaImpact.getRange(AreaImpact.getPoints(owner)));
            AreaImpact.burst(owner, at, hit);
        }
        return hit;
    }
}
