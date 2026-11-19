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
 * 气波弹射物（方位攻击球）：力气蛊[Strength Qi Gu]人力与钧力流派的远程拳，兽力虚影流派在未持有永久兽力时也回退发射本弹
 * 射物。
 * <ul>
 *   <li>{@link #shoot} 发射直线气波：不受重力与阻力影响，以 {@link #SPEED} 格/tick 飞向发射瞬间目标中心所在的固定点（不
 *       追踪移动），到达该点或途中触碰可伤害生物时触发 {@link QiBurst#burst} 爆开，撞上方块则直接熄灭、不触发爆开；锁定
 *       目标只要处于爆开半径内即承受全额伤害。</li>
 *   <li>伤害、瞄准点、直线朝向（爆开时作为范围冲击环的轴向）与锁定目标仅在服务端维护；实体无几何模型，客户端沿生成包初
 *       速度直飞并每 tick 生成一团云粒子，服务端爆开时发送 {@link #BURST_PUFFS} 团烟雾、撞方块熄灭时发送
 *       {@link #SNUFF_PUFFS} 团烟雾；不写入存档。</li>
 *   <li>蓄力蛊[Store Strength Gu]的蓄力一拳调用 {@link #track} 发射追踪气波：两端每 tick 根据同步目标 ID 转向目标当前中
 *       心，最大飞行时长放宽至直线飞行 tick 的 {@link #TRACK_FLIGHT_FACTOR} 倍，并使用蓄力一拳专属的
 *       {@link QiBurst.Spec}；若目标中途死亡则继续飞向最后记录的位置，飞行超时仍未到达则直接在当前位置爆开（因出拳反噬
 *       已结算）。</li>
 * </ul>
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

    public static QiWaveEntity shoot(ServerPlayer owner, LivingEntity target, float damage, QiBurst.Spec burst) {
        QiWaveEntity orb = aim(owner, target, damage);
        orb.burst = burst;
        return launch(orb, owner);
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
        boolean arriving = !level().isClientSide() && aimPoint != null
                && center.distanceTo(aimPoint) <= motion.length();
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
