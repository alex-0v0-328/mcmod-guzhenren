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
 * target, it bursts ({@link QiBurst#burst}) around the target and is gone that tick, handing on the arc's tangent at
 * that tick ({@link MoonBladeArc#tangentAt}) as the direction it last flew; a block in the way, or a flight
 * that runs out with no target left, takes it away with no burst. Nothing lingers: the strike is the burst itself.
 *
 * <p>It plays its family's charge loop while it flies (the horse, drawn on vanilla's model, gallops on the
 * renderer's limb swing instead) and faces its next step along the arc, yaw and pitch alike, so
 * it rears on the arc's lift, dives after it and points at the target over its last stretch; it fades in over
 * {@link #FADE_IN_TICKS}. The
 * server owns the burst and the damage, set once by {@link #shoot}; the client copy flies the same curve from the
 * synced target, bend and flight ticks. It is never saved. The Store Strength Gu's [蓄力蛊] charged punch [蓄力一拳]
 * throws it with its own {@link QiBurst.Spec}; the Strength Qi Gu's punch bursts as a Qi Wave.
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
    public static final int FADE_IN_TICKS = 3;
    private static final int NO_TARGET = -1;
    private static final float SPAWN_DROP = 0.3F;
    private static final double CULL_MARGIN = 1.5D;
    private static final Map<BeastStrengthFamily, RawAnimation> CHARGES =
            BeastPhantomVisual.byFamily(family -> BeastPhantomVisual.getLoop(family, "charge"));
    private static final EntityDataAccessor<Integer> TARGET =
            SynchedEntityData.defineId(BeastChargeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> BEND =
            SynchedEntityData.defineId(BeastChargeEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> FLIGHT_TICKS =
            SynchedEntityData.defineId(BeastChargeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FAMILY =
            SynchedEntityData.defineId(BeastChargeEntity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float damage;
    private QiBurst.Spec burst = QiBurst.Spec.QI_WAVE;
    private @Nullable Vec3 arcStart;

    public BeastChargeEntity(EntityType<? extends BeastChargeEntity> type, Level level) { super(type, level); }

    public static BeastChargeEntity shoot(ServerPlayer owner, LivingEntity target, float damage,
                                          BeastStrengthFamily family) {
        return shoot(owner, target, damage, family, QiBurst.Spec.QI_WAVE);
    }

    public static BeastChargeEntity shoot(ServerPlayer owner, LivingEntity target, float damage,
                                          BeastStrengthFamily family, QiBurst.Spec burst) {
        BeastChargeEntity charge = new BeastChargeEntity(ModEntityTypes.BEAST_CHARGE.get(), owner.level());
        charge.setOwner(owner);
        charge.damage = damage;
        charge.burst = burst;
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

    @Override
    public BeastStrengthFamily getFamily() {
        BeastStrengthFamily[] families = BeastStrengthFamily.values();
        return families[Mth.clamp(entityData.get(FAMILY), 0, families.length - 1)];
    }

    @Override
    public float getFade(float partialTick) {
        return Mth.clamp((tickCount + partialTick) / FADE_IN_TICKS, 0.0F, 1.0F);
    }

    @Override
    public float getPitch(float partialTick) { return Mth.lerp(partialTick, xRotO, getXRot()); }

    @Override
    public boolean isCharging() { return true; }

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
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > entityData.get(FLIGHT_TICKS) + GRACE_TICKS) {
            discard();
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
        if (isRemoved()) return;

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        face(getNextStep(target, motion));
    }

    private Vec3 getNextStep(@Nullable LivingEntity target, Vec3 motion) {
        MoonBladeArc arc = getArc();
        if (arc == null || target == null) return motion;
        double t = Math.min(1.0D, (tickCount + 1) / (double) arc.flightTicks());
        return arc.pointAt(target.getBoundingBox().getCenter(), t).subtract(position());
    }

    private void face(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-6D) return;
        if (motion.horizontalDistanceSqr() > 1.0E-6D) setYRot((float) (Mth.atan2(-motion.x, motion.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(-motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
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

        Vec3 at = target.getBoundingBox().getCenter();
        QiBurst.burst(owner, this, at, getHeading(at), target, damage, burst);
    }

    private Vec3 getHeading(Vec3 end) {
        MoonBladeArc arc = getArc();
        if (arc == null) return getDeltaMovement();
        return arc.tangentAt(end, Math.min(1.0D, tickCount / (double) arc.flightTicks()));
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) discard();
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDeltaMovement(packet.getXa(), packet.getYa(), packet.getZa());
        yRotO = getYRot();
        xRotO = getXRot();
        arcStart = position();
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(CULL_MARGIN); }

    @Override
    public boolean shouldBeSaved() { return false; }

    //region Animation [动画]
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "charge", 0,
                state -> state.setAndContinue(CHARGES.get(getFamily()))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    //endregion
}
