package net.alex.guzhenren.entity.projectile;

import java.util.function.Predicate;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A Qi Wave [气波]: the punch a Strength Qi Gu [力气蛊] throws at a target out of reach. It always flies at one: a
 * {@link MoonBladeArc} from the muzzle to the target's center, read afresh every tick, at {@link #SPEED} blocks a
 * tick, without gravity or drag. When the flight ticks run out, or its box touches the target on the way, it hits that
 * target outright; a block or another entity in the way stops it first. A target gone mid-flight drops the arc and
 * the wave flies on straight until its {@link #getLifetimeTicks} run out.
 *
 * <p>The server owns the hit: the damage, set once by {@link #shoot}, lives only there, and the source names the
 * shooter so PvP follows the server's own rules. The wave has no model; the client copy flies the same curve from the
 * synced target, bend and flight ticks only to leave one cloud puff a tick, and the hit leaves {@link #PUFF_COUNT}
 * more. It is never saved.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService
 * @since 1.0.0
 */

public final class QiWaveEntity extends Projectile {

    public static final float SPEED = 1.5F;
    /** Ticks a wave outlives its flight, so a hit at the last tick still lands before it is gone. */
    public static final int GRACE_TICKS = 2;
    public static final int MAX_FLIGHT_TICKS = 10;
    private static final int NO_TARGET = -1;
    private static final float SPAWN_DROP = 0.3F;
    private static final int PUFF_COUNT = 3;
    private static final EntityDataAccessor<Integer> TARGET =
            SynchedEntityData.defineId(QiWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> BEND =
            SynchedEntityData.defineId(QiWaveEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> FLIGHT_TICKS =
            SynchedEntityData.defineId(QiWaveEntity.class, EntityDataSerializers.INT);
    private final Predicate<Entity> hittable = this::canHitEntity;
    private float damage;
    private @Nullable Vec3 arcStart;

    public QiWaveEntity(EntityType<? extends QiWaveEntity> type, Level level) { super(type, level); }

    /** Throws one wave at {@code target}; the caller has already checked it can be hit. */
    public static QiWaveEntity shoot(ServerPlayer owner, LivingEntity target, float damage) {
        QiWaveEntity wave = new QiWaveEntity(ModEntityTypes.QI_WAVE.get(), owner.level());
        wave.setOwner(owner);
        wave.damage = damage;
        wave.setPos(owner.getX(), owner.getEyeY() - SPAWN_DROP - wave.getBbHeight() / 2.0F, owner.getZ());
        Vec3 start = wave.position();
        Vec3 end = target.getBoundingBox().getCenter();
        MoonBladeArc arc = MoonBladeArc.aim(start, end, SPEED, owner.getRandom());
        int ticks = Math.min(arc.flightTicks(), MAX_FLIGHT_TICKS);
        wave.entityData.set(TARGET, target.getId());
        wave.entityData.set(BEND, arc.bend().toVector3f());
        wave.entityData.set(FLIGHT_TICKS, ticks);
        wave.arcStart = start;
        wave.setDeltaMovement(arc.pointAt(end, 1.0D / ticks).subtract(start));
        owner.level().addFreshEntity(wave);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.BREEZE_SHOOT,
                SoundSource.PLAYERS, 0.6F, 1.4F);
        return wave;
    }

    public float getDamage() { return damage; }

    public int getLifetimeTicks() { return entityData.get(FLIGHT_TICKS) + GRACE_TICKS; }

    /** The target this wave flies at, while it is still there to be hit. */
    public @Nullable LivingEntity getTarget() {
        int id = entityData.get(TARGET);
        if (id == NO_TARGET) return null;
        return level().getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private @Nullable MoonBladeArc getArc() {
        int ticks = entityData.get(FLIGHT_TICKS);
        if (ticks <= 0 || entityData.get(TARGET) == NO_TARGET) return null;
        if (arcStart == null) arcStart = position();
        return new MoonBladeArc(arcStart, new Vec3(entityData.get(BEND)), ticks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(TARGET, NO_TARGET);
        builder.define(BEND, new Vector3f());
        builder.define(FLIGHT_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > getLifetimeTicks()) {
            discard();
            return;
        }
        LivingEntity target = followArc();
        Vec3 motion = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, hittable);
        if (hit.getType() == HitResult.Type.MISS && target != null && arrives(target, motion)) {
            hit = new EntityHitResult(target, position().add(motion));
        }
        if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit)) {
            hitTargetOrDeflectSelf(hit);
        }
        if (isRemoved()) return;

        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.CLOUD, getX(), getY() + getBbHeight() / 2.0F, getZ(), 0.0D, 0.0D, 0.0D);
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
    }

    private @Nullable LivingEntity followArc() {
        MoonBladeArc arc = getArc();
        if (arc == null) return null;
        LivingEntity target = getTarget();
        if (target == null) {
            if (!level().isClientSide()) entityData.set(TARGET, NO_TARGET);
            return null;
        }
        double t = Math.min(1.0D, tickCount / (double) arc.flightTicks());
        setDeltaMovement(arc.pointAt(target.getBoundingBox().getCenter(), t).subtract(position()));
        return target;
    }

    private boolean arrives(LivingEntity target, Vec3 motion) {
        return tickCount >= entityData.get(FLIGHT_TICKS)
                || getBoundingBox().move(motion).intersects(target.getBoundingBox());
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide()) return;

        DamageSource source = ModDamageTypes.source(this, getOwner(), ModDamageTypes.QI_WAVE);
        result.getEntity().hurt(source, damage);
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel server)) return;

        Vec3 at = result.getLocation();
        server.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, PUFF_COUNT, 0.1D, 0.1D, 0.1D, 0.02D);
        discard();
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDeltaMovement(packet.getXa(), packet.getYa(), packet.getZa());
        arcStart = position();
    }

    @Override
    public boolean shouldBeSaved() { return false; }
}
