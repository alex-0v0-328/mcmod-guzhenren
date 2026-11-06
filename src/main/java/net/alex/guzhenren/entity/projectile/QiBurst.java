package net.alex.guzhenren.entity.projectile;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
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

    public static @NotNull List<LivingEntity> burst(@NotNull ServerPlayer owner, @NotNull Entity projectile,
                                                    @NotNull Vec3 at, @Nullable LivingEntity locked, float damage) {
        DamageSource source = ModDamageTypes.source(projectile, owner, ModDamageTypes.QI_WAVE);
        List<LivingEntity> hit = new ArrayList<>();
        AABB area = new AABB(at, at).inflate(RADIUS);
        for (LivingEntity victim : projectile.level().getEntitiesOfClass(LivingEntity.class, area,
                victim -> victim.getBoundingBox().distanceToSqr(at) <= RADIUS * RADIUS
                        && AreaImpact.canHit(owner, victim))) {
            if (victim.hurt(source, getDamage(damage, victim == locked))) hit.add(victim);
        }
        return hit;
    }
}
