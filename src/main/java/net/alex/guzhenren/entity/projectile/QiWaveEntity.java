package net.alex.guzhenren.entity.projectile;

import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 气波（方位攻击球）：力气蛊[Strength Qi Gu]人力钧力流的远程拳；兽力虚影流无永久兽力[Beast Strength]时也用。
 * <ul>
 *   <li>直飞，无重力阻力，每 tick {@link #SPEED} 格，飞向投出时目标中心所在点，不跟随；在该点或途中碰到可伤生物处爆开
 *       （{@link QiBurst#burst}），遇方块熄灭不爆；锁定目标在范围内即吃全额。</li>
 *   <li>伤害、瞄准点、朝向（直线方向，爆开时作范围冲击轴）、锁定目标由 {@link #shoot} 设定，只在服务端；无模型，客户端
 *       沿生成包直线飞，每 tick 一团云；爆开 {@link #BURST_PUFFS} 团烟，熄灭 {@link #SNUFF_PUFFS} 团；不存档。</li>
 *   <li>蓄力蛊[Store Strength Gu]的蓄力一拳投追踪球（{@link #track}）：两端每 tick 按同步 id 追目标中心，至多直飞的
 *       {@link #TRACK_FLIGHT_FACTOR} 倍，用拳自己的 {@link QiBurst.Spec}；目标死则飞向最后所见点，飞完未到即原地爆开
 *       （反噬已付）。</li>
 *   <li>⚠ {@link #BURST_PUFFS}、{@link #SNUFF_PUFFS} 暂定。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see QiBurst
 * @since 1.0.0
 */

public final class QiWaveEntity extends Projectile {

    public static final float SPEED = 1.5F;
    public static final int GRACE_TICKS = 2;
    public static final int BURST_PUFFS = 8;
    public static final int SNUFF_PUFFS = 3;
    private static final float SPAWN_DROP = 0.3F;
    public static final int TRACK_FLIGHT_FACTOR = 2;
    private static final int NO_TARGET = -1;
    private static final EntityDataAccessor<Integer> TRACKED =
            SynchedEntityData.defineId(QiWaveEntity.class, EntityDataSerializers.INT);
    private float damage;
    private QiBurst.Spec burst = QiBurst.Spec.QI_WAVE;
    private @Nullable Vec3 aimPoint;
    private Vec3 heading = Vec3.ZERO;
    private int lockedId = NO_TARGET;
    private int flightTicks;

    public QiWaveEntity(EntityType<? extends QiWaveEntity> type, Level level) { super(type, level); }

    public static QiWaveEntity shoot(ServerPlayer owner, LivingEntity target, float damage) {
        return launch(aim(owner, target, damage), owner);
    }

    public static QiWaveEntity track(ServerPlayer owner, LivingEntity target, float damage, QiBurst.Spec burst) {
        QiWaveEntity orb = aim(owner, target, damage);
        orb.burst = burst;
        orb.flightTicks *= TRACK_FLIGHT_FACTOR;
        orb.entityData.set(TRACKED, target.getId());
        return launch(orb, owner);
    }

    private static QiWaveEntity aim(ServerPlayer owner, LivingEntity target, float damage) {
        QiWaveEntity orb = new QiWaveEntity(ModEntityTypes.QI_WAVE.get(), owner.level());
        orb.setOwner(owner);
        orb.damage = damage;
        orb.setPos(owner.getX(), owner.getEyeY() - SPAWN_DROP - orb.getBbHeight() / 2.0F, owner.getZ());
        Vec3 aim = target.getBoundingBox().getCenter();
        Vec3 path = aim.subtract(orb.position().add(0.0D, orb.getBbHeight() / 2.0F, 0.0D));
        orb.aimPoint = aim;
        orb.lockedId = target.getId();
        orb.flightTicks = Mth.ceil(path.length() / SPEED);
        orb.heading = path.normalize();
        orb.setDeltaMovement(orb.heading.scale(SPEED));
        return orb;
    }

    private static QiWaveEntity launch(QiWaveEntity orb, ServerPlayer owner) {
        owner.level().addFreshEntity(orb);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.BREEZE_SHOOT,
                SoundSource.PLAYERS, 0.6F, 1.4F);
        return orb;
    }

    public float getDamage() { return damage; }

    private @Nullable LivingEntity getTracked() {
        int id = entityData.get(TRACKED);
        if (id == NO_TARGET) return null;
        return level().getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(TRACKED, NO_TARGET);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 center = position().add(0.0D, getBbHeight() / 2.0F, 0.0D);
        if (!level().isClientSide() && (aimPoint == null || tickCount > flightTicks + GRACE_TICKS)) {
            if (aimPoint != null && entityData.get(TRACKED) != NO_TARGET) {
                burst(center);
            } else {
                discard();
            }
            return;
        }
        steer(center);
        Vec3 motion = getDeltaMovement();
        boolean arriving = !level().isClientSide() && center.distanceTo(aimPoint) <= motion.length();
        if (arriving) setDeltaMovement(aimPoint.subtract(center));
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit)) {
            hitTargetOrDeflectSelf(hit);
        } else if (arriving) {
            burst(aimPoint);
        }
        if (isRemoved()) return;

        if (level().isClientSide()) level().addParticle(ParticleTypes.CLOUD, center.x, center.y, center.z, 0, 0, 0);
        motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
    }

    private void steer(Vec3 center) {
        LivingEntity tracked = getTracked();
        if (tracked == null) return;

        Vec3 aim = tracked.getBoundingBox().getCenter();
        Vec3 toward = aim.subtract(center);
        if (toward.lengthSqr() <= 1.0E-6D) return;
        if (!level().isClientSide()) aimPoint = aim;
        heading = toward.normalize();
        setDeltaMovement(heading.scale(SPEED));
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        if (!super.canHitEntity(entity) || !(entity instanceof LivingEntity living)) return false;
        return !(getOwner() instanceof ServerPlayer owner) || AreaImpact.canHit(owner, living);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide()) burst(result.getEntity().getBoundingBox().getCenter());
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel server)) return;

        Vec3 at = result.getLocation();
        server.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, SNUFF_PUFFS, 0.1D, 0.1D, 0.1D, 0.02D);
        discard();
    }

    private void burst(Vec3 at) {
        if (getOwner() instanceof ServerPlayer owner) {
            LivingEntity locked = level().getEntity(lockedId) instanceof LivingEntity living ? living : null;
            QiBurst.burst(owner, this, at, heading, locked, damage, burst);
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, BURST_PUFFS, 0.4D, 0.4D, 0.4D, 0.05D);
        }
        discard();
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDeltaMovement(packet.getXa(), packet.getYa(), packet.getZa());
    }

    @Override
    public boolean shouldBeSaved() { return false; }
}
