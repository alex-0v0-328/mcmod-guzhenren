package net.alex.guzhenren.entity.projectile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.alex.guzhenren.gameplay.path.strength.AreaDamage;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
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
 * 力气蛊[Strength Qi Gu]与蓄力蛊[Store Strength Gu]远程攻击共用的范围爆开结算工具，供 {@link BeastChargeEntity} 与
 * {@link QiWaveEntity} 调用。
 * <ul>
 *   <li>对碰撞箱距爆点不超过 {@link Spec#radius()}、经 {@link ProjectileHits#canHit} 放行（只排除旁观者，射手自己、盟
 *       友、宠物、野生蛊与盔甲架都算）且爆点至眼部无方块遮挡（{@link AreaDamage#isInSight}，Alex 定，隔墙不受伤）的生物
 *       按溢出结算
 *       （{@link AreaDamage#spill}）：锁定目标排首位吃整发伤害，剩余由近及远至多 {@link #MAX_TARGETS} 个；锁定目标不在
 *       范围内时由最近的目标吃起。射手自己无论远近都排在最后（Alex 定）：只吃打完别人后剩下的溢出，不替敌人挡伤害，
 *       名额已满时不挨打。爆开本身不结算连招轮、也不触发身旁兽力虚影。</li>
 *   <li>当 {@link Spec#areaDamage()} 为真（人力钧力流收尾招暴击[Crit]，发射方在连招轮收尾时一次掷定）时，发射方已把
 *       {@link Spec#radius()} 扩到范围伤害[Area Damage]半径，爆开另沿弹射物末段飞行朝向 {@code heading} 发射冲击环，并
 *       把锁定目标以外的命中目标从爆点向外击退；不破坏方块。</li>
 *   <li>{@link Spec#shot()} 是同一拳几道弹射物共用的过载反噬[Body Overload]记账：首次造成伤害的爆开结算一次。</li>
 *   <li>{@link Spec} 定义爆开半径、伤害类型、是否放范围伤害与过载记账，由出手方按每一拳新建（蓄力蛊的蓄力一拳使用蓄
 *       力一拳伤害类型）；{@link Spec#QI_WAVE} 只是弹射物字段的无记账默认值。</li>
 *   <li>⚠ 气波伤害类型 {@code guzhenren:qi_wave} 必须带有 {@code minecraft:bypasses_cooldown} 标签，否则同一拳发出的多
 *       道兽力冲击会被目标的受击无敌帧吞掉。</li>
 * </ul>
 */

public final class QiBurst {

    private QiBurst() {}

    public static final double RADIUS = 3.0D;
    public static final int MAX_TARGETS = 8;

    public record Spec(double radius, @NotNull ResourceKey<DamageType> type, boolean areaDamage,
                       @Nullable BodyOverload.Shot shot) {

        public static final Spec QI_WAVE = new Spec(RADIUS, ModDamageTypes.QI_WAVE, false, null);
    }

    public static @NotNull List<LivingEntity> burst(@NotNull ServerPlayer owner, @NotNull Entity projectile,
                                                    @NotNull Vec3 at, @NotNull Vec3 heading,
                                                    @Nullable LivingEntity locked, float damage, @NotNull Spec spec) {
        DamageSource source = ModDamageTypes.source(projectile, owner, spec.type());
        double radius = spec.radius();
        List<LivingEntity> targets = new ArrayList<>(projectile.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(at, at).inflate(radius),
                victim -> victim.getBoundingBox().distanceToSqr(at) <= radius * radius
                        && ProjectileHits.canHit(victim) && AreaDamage.isInSight(projectile, at, victim)));
        targets.sort(Comparator.comparingDouble((LivingEntity victim) -> victim == locked ? -1.0D
                : victim == owner ? Double.MAX_VALUE : victim.getBoundingBox().distanceToSqr(at)));

        boolean area = spec.areaDamage() && owner.level() == projectile.level();
        List<LivingEntity> hit = AreaDamage.spill(targets, damage, MAX_TARGETS, source, victim -> {
            if (!area || victim == locked) return;
            Vec3 away = victim.getBoundingBox().getCenter().subtract(at);
            victim.knockback(AreaDamage.KNOCKBACK, -away.x, -away.z);
            victim.hurtMarked = true;
        });
        if (!hit.isEmpty() && spec.shot() != null) spec.shot().settle(owner);
        if (area) RingConeEmitter.areaDamageRing(owner.serverLevel(), at, heading, radius);
        return hit;
    }
}
