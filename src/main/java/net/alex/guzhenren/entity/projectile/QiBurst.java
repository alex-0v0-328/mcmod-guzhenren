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
 * The burst [爆开] a Strength Qi Gu's [力气蛊] ranged punch ends in, a {@link BeastChargeEntity} or a
 * {@link QiWaveEntity} alike: every living entity whose box lies within {@link #RADIUS} blocks of the burst and that
 * {@link AreaImpact#canHit} lets the owner hurt -- never the owner, an ally, a pet, a wild Gu or an armor stand --
 * takes the punch's damage as a Qi Wave, the locked target in full and everyone else {@link #SECONDARY_RATIO} of it.
 * A Qi Wave opens no strength round and manifests no phantom.
 *
 * <p>Each burst then rolls the area impact [范围冲击] once on the projectile's own random ({@link AreaImpact#rolls}),
 * at the owner's chance; a landed roll sends the area impact ring and releases the cone from the burst point along
 * the {@code heading} the projectile last flew, sparing every target the burst hit, while the owner stands in the
 * burst's level. The burst itself never changes with the roll.
 *
 * <p>A {@link Spec} names the radius, the damage type and whether the area impact rolls: {@link Spec#QI_WAVE} for the
 * Strength Qi Gu's punch, and the Store Strength Gu's [蓄力蛊] charged punch [蓄力一拳] hands its own, which bursts
 * as a charged punch and rolls nothing.
 *
 * <p>⚠ {@link #RADIUS} and {@link #SECONDARY_RATIO} are provisional.
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
                        && AreaImpact.canHit(owner, victim))) {
            if (victim.hurt(source, getDamage(damage, victim == locked))) hit.add(victim);
        }
        if (!spec.rollsAreaImpact()) return hit;
        if (owner.level() == projectile.level() && AreaImpact.rolls(owner, projectile.getRandom())) {
            RingConeEmitter.areaImpactRing(owner.serverLevel(), at, heading, AreaImpact.RANGE);
            AreaImpact.release(owner, at, heading, hit);
        }
        return hit;
    }
}
