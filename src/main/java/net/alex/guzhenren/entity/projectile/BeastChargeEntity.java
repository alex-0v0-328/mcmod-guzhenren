package net.alex.guzhenren.entity.projectile;

import java.util.Map;
import net.alex.guzhenren.entity.beast.BeastPhantomVisual;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Beast Charge [兽力冲击]: the Strength Qi Gu's [力气蛊] ranged punch on the beast strength phantom path, one
 * phantom beast of the round's family running at the lock-on target. It flies a {@link MoonBladeArc} from the muzzle to
 * the target's center, read afresh every tick, at {@link #SPEED} blocks a tick for at most {@link #MAX_FLIGHT_TICKS},
 * without gravity or drag, and passes through every other entity. When the flight runs out, or its box touches the
 * target, it bursts ({@link QiBurst#burst}) around the target; a block in the way stops it with no burst. A
 * target gone mid-flight drops the arc and the charge runs on straight until its time runs out, bursting nowhere.
 *
 * <p>Stopped, it stays where it stopped, plays its family's attack once and fades over {@link #DISSOLVE_TICKS}.
 * It runs while it flies and faces its motion, turned by yaw alone. The server owns the burst and the damage, set
 * once by {@link #shoot}; the client copy flies the same curve from the synced target, bend and flight ticks. It is
 * never saved. ⚠ {@link #DISSOLVE_TICKS} is provisional.
 *
 * @author Alex
 * @version 1.0.0
 * @see QiBurst
 * @see net.alex.guzhenren.client.renderer.BeastPhantomRenderer
 * @since 1.0.0
 */

public final class BeastChargeEntity extends Projectile implements GeoEntity, BeastPhantomVisual {

    public static final float SPEED = 1.5F;
    public static final int MAX_FLIGHT_TICKS = 10;
    public static final int GRACE_TICKS = 2;
    public static final int DISSOLVE_TICKS = 10;
    private static final int FADE_IN_TICKS = 3;
    private static final int NO_TARGET = -1;
    private static final int FLYING = -1;
    private static final float SPAWN_DROP = 0.3F;
    private static final double CULL_MARGIN = 1.5D;
    private static final Map<BeastStrengthFamily, RawAnimation> RUNS =
            BeastPhantomVisual.byFamily(family -> BeastPhantomVisual.getLoop(family, "run"));
    private static final Map<BeastStrengthFamily, RawAnimation> ATTACKS =
            BeastPhantomVisual.byFamily(family -> BeastPhantomVisual.getAttack(family, "idle"));
    private static final EntityDataAccessor<Integer> TARGET =
            SynchedEntityData.defineId(BeastChargeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> BEND =
            SynchedEntityData.defineId(BeastChargeEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> FLIGHT_TICKS =
            SynchedEntityData.defineId(BeastChargeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FAMILY =
            SynchedEntityData.defineId(BeastChargeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STOPPED_AT =
            SynchedEntityData.defineId(BeastChargeEntity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float damage;
    private @Nullable Vec3 arcStart;

    public BeastChargeEntity(EntityType<? extends BeastChargeEntity> type, Level level) { super(type, level); }

    public static BeastChargeEntity shoot(ServerPlayer owner, LivingEntity target, float damage,
                                          BeastStrengthFamily family) {
        BeastChargeEntity charge = new BeastChargeEntity(ModEntityTypes.BEAST_CHARGE.get(), owner.level());
        charge.setOwner(owner);
        charge.damage = damage;
        charge.setPos(owner.getX(), owner.getEyeY() - SPAWN_DROP - charge.getBbHeight() / 2.0F, owner.getZ());
        Vec3 start = charge.position();
        Vec3 end = target.getBoundingBox().getCenter();
        MoonBladeArc arc = MoonBladeArc.aim(start, end, SPEED, owner.getRandom());
        int ticks = Math.min(arc.flightTicks(), MAX_FLIGHT_TICKS);
        charge.entityData.set(TARGET, target.getId());
        charge.entityData.set(BEND, arc.bend().toVector3f());
        charge.entityData.set(FLIGHT_TICKS, ticks);
        charge.entityData.set(FAMILY, family.ordinal());
        charge.arcStart = start;
        charge.setDeltaMovement(arc.pointAt(end, 1.0D / ticks).subtract(start));
        charge.face(charge.getDeltaMovement());
        owner.level().addFreshEntity(charge);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.RAVAGER_ROAR,
                SoundSource.PLAYERS, 0.5F, 1.6F);
        return charge;
    }

    public float getDamage() { return damage; }

    public boolean isStopped() { return entityData.get(STOPPED_AT) != FLYING; }

    @Override
    public BeastStrengthFamily getFamily() {
        BeastStrengthFamily[] families = BeastStrengthFamily.values();
        return families[Mth.clamp(entityData.get(FAMILY), 0, families.length - 1)];
    }

    @Override
    public float getFade(float partialTick) {
        float age = tickCount + partialTick;
        int stoppedAt = entityData.get(STOPPED_AT);
        float out = stoppedAt == FLYING ? 1.0F : 1.0F - (age - stoppedAt) / DISSOLVE_TICKS;
        return Mth.clamp(Math.min(age / FADE_IN_TICKS, out), 0.0F, 1.0F);
    }

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
        builder.define(FAMILY, BeastStrengthFamily.BOAR.ordinal());
        builder.define(STOPPED_AT, FLYING);
    }

    @Override
    public void tick() {
        super.tick();
        if (isStopped()) {
            if (!level().isClientSide() && tickCount - entityData.get(STOPPED_AT) >= DISSOLVE_TICKS) discard();
            return;
        }
        if (!level().isClientSide() && tickCount > entityData.get(FLIGHT_TICKS) + GRACE_TICKS) {
            stop();
            return;
        }
        LivingEntity target = followArc();
        Vec3 motion = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() == HitResult.Type.MISS && target != null && arrives(target, motion)) {
            hit = new EntityHitResult(target, target.getBoundingBox().getCenter());
        }
        if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit)) {
            hitTargetOrDeflectSelf(hit);
        }
        if (isRemoved() || isStopped()) return;

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        face(motion);
    }

    private void face(Vec3 motion) {
        if (motion.horizontalDistanceSqr() > 1.0E-6D) setYRot((float) (Mth.atan2(-motion.x, motion.z) * Mth.RAD_TO_DEG));
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
    protected boolean canHitEntity(@NotNull Entity entity) {
        return entity.getId() == entityData.get(TARGET) && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide() || !(getOwner() instanceof ServerPlayer owner)) return;
        if (!(result.getEntity() instanceof LivingEntity target)) return;

        QiBurst.burst(owner, this, target.getBoundingBox().getCenter(), target, damage);
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) stop();
    }

    private void stop() {
        setDeltaMovement(Vec3.ZERO);
        entityData.set(STOPPED_AT, tickCount);
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDeltaMovement(packet.getXa(), packet.getYa(), packet.getZa());
        arcStart = position();
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(CULL_MARGIN); }

    @Override
    public boolean shouldBeSaved() { return false; }

    //region Animation [动画]
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "charge", 0, state -> state.setAndContinue(
                (isStopped() ? ATTACKS : RUNS).get(getFamily()))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    //endregion
}
