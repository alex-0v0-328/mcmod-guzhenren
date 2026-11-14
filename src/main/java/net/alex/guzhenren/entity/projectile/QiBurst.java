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
 * 力气蛊[Strength Qi Gu]远程拳的爆开，{@link BeastChargeEntity} 与 {@link QiWaveEntity} 共用。
 * <ul>
 *   <li>碰撞箱距爆点 {@link #RADIUS} 格内、{@link AreaImpact#canHit} 放行（不含主人、盟友、宠物、野生蛊、盔甲架）且
 *       爆点到其眼无方块遮挡（{@link AreaImpact#isInSight}，Alex 定，墙后不挨打）的生物以气波受伤：锁定目标全额，
 *       其余 {@link #SECONDARY_RATIO}；不开轮、不出虚影。</li>
 *   <li>每次爆开以弹射物随机源按主人概率掷一次范围冲击（{@link AreaImpact#rolls}）；掷中且主人同维度时沿最后飞行的
 *       {@code heading} 发冲击环，在爆点放范围冲击球（{@link AreaImpact#burst}），避开已命中者、不破方块；爆开本身
 *       不变。</li>
 *   <li>{@link Spec} 定半径、伤害类型、是否掷范围冲击：{@link Spec#QI_WAVE} 给力气蛊；蓄力蛊[Store Strength Gu]的蓄力一
 *       拳自带一份，按蓄力一拳爆开、不掷。</li>
 *   <li>⚠ 气波伤害类型 {@code guzhenren:qi_wave} 带 {@code minecraft:bypasses_cooldown}：同一拳的几只兽力冲击才不会被
 *       受击无敌帧吞掉。</li>
 *   <li>⚠ {@link #RADIUS}、{@link #SECONDARY_RATIO} 暂定。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService
 * @since 1.0.0
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
                                                    @Nullable LivingEntity locked, float damage) {
        return burst(owner, projectile, at, heading, locked, damage, Spec.QI_WAVE);
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
