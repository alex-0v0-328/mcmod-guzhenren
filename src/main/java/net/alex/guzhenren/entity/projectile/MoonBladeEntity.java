package net.alex.guzhenren.entity.projectile;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A Moon Blade [月刃]: the crescent the Moonlight [月光蛊], Moonglow [月芒蛊] and Blood Moon Gu [血月蛊] fire. Without
 * a target it flies straight, without gravity or drag, at {@link #SPEED} blocks a tick for {@link #LIFETIME_TICKS},
 * and ends on the first entity or block it meets; a Blood Moon Blade [血月刃] also leaves Bleeding [流血] on what it
 * hurts.
 *
 * <p>Fired at a locked-on target [锁定目标] ({@link #shoot} with one the blade can hit) it flies a {@link MoonBladeArc}
 * instead: a curve from the muzzle to the target's center, read afresh every tick so the target is met where it is,
 * bent left or right by a roll made once per shot. It covers the chord in the arc's flight ticks and lives
 * {@link #ARC_GRACE_TICKS} longer; when the ticks run out, or its box touches the target on the way, it hits that
 * target outright, so the landing point is the target. A block or another entity in the way still stops it as on a
 * straight flight. A target gone (dead, removed, unloaded) mid-flight drops the arc, and the blade flies on straight.
 *
 * <p>The server owns the hit: the damage lives only there, set once by {@link #shoot}, and the client copy flies
 * the same line, or the same curve from the synced target, bend and flight ticks, just to be drawn. The blade is
 * never saved: it lives about a second, so a chunk unloading under it simply drops it.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.moon.MoonBladeModule
 * @since 1.0.0
 */

public final class MoonBladeEntity extends Projectile {

    /** The two blades: their texture, their hit puff, and whether they make the target bleed. */
    public enum Variant {
        MOON(false), BLOOD(true);

        private final boolean bleeds;

        Variant(boolean bleeds) { this.bleeds = bleeds; }

        public boolean bleeds() { return bleeds; }
    }

    public static final float SPEED = 1.5F;
    public static final int LIFETIME_TICKS = 16;
    /** Ticks a locked blade outlives its flight, so a hit at the last tick still lands before it is gone. */
    public static final int ARC_GRACE_TICKS = 2;
    /** The longest locked flight; {@code KillingMoveService} locks nothing past 26 blocks, 18 ticks, inside it. */
    public static final int MAX_ARC_FLIGHT_TICKS = 20;
    public static final int NO_TARGET = -1;
    /** Blocks below the eye the blade appears, so the shooter sees its face instead of its edge. */
    private static final float SPAWN_DROP = 0.3F;
    public static final int BLEEDING_TICKS = 5 * Ticks.SECOND;
    private static final int PUFF_COUNT = 6;
    private static final DustParticleOptions BLOOD_PUFF = new DustParticleOptions(new Vector3f(0.6F, 0.05F, 0.05F),
            1.0F);
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(MoonBladeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ARC_TARGET =
            SynchedEntityData.defineId(MoonBladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> ARC_BEND =
            SynchedEntityData.defineId(MoonBladeEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> ARC_TICKS =
            SynchedEntityData.defineId(MoonBladeEntity.class, EntityDataSerializers.INT);
    private float damage;
    private @Nullable Vec3 arcStart;

    public MoonBladeEntity(EntityType<? extends MoonBladeEntity> type, Level level) { super(type, level); }

    public static void shoot(ServerPlayer owner, Variant variant, float damage) {
        shoot(owner, variant, damage, null);
    }

    /** Fires one blade; a {@code target} the blade can hit makes it a locked flight, any other flies straight. */
    public static void shoot(ServerPlayer owner, Variant variant, float damage, @Nullable LivingEntity target) {
        MoonBladeEntity blade = new MoonBladeEntity(ModEntityTypes.MOON_BLADE.get(), owner.level());
        blade.setOwner(owner);
        blade.entityData.set(VARIANT, (byte) variant.ordinal());
        blade.damage = damage;
        blade.setPos(owner.getX(), owner.getEyeY() - SPAWN_DROP - blade.getBbHeight() / 2.0F, owner.getZ());
        blade.shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0.0F, SPEED, 0.0F);
        if (target != null && blade.canHitEntity(target)) blade.lockOn(target, owner);
        owner.level().addFreshEntity(blade);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.8F, variant.bleeds() ? 0.8F : 1.4F);
    }

    private void lockOn(LivingEntity target, ServerPlayer owner) {
        Vec3 start = position();
        Vec3 end = target.getBoundingBox().getCenter();
        MoonBladeArc arc = MoonBladeArc.aim(start, end, SPEED, owner.getRandom());
        int ticks = Math.min(arc.flightTicks(), MAX_ARC_FLIGHT_TICKS);
        entityData.set(ARC_TARGET, target.getId());
        entityData.set(ARC_BEND, arc.bend().toVector3f());
        entityData.set(ARC_TICKS, ticks);
        arcStart = start;
        setDeltaMovement(arc.pointAt(end, 1.0D / ticks).subtract(start));
    }

    public Variant getVariant() {
        Variant[] variants = Variant.values();
        return variants[Math.floorMod(entityData.get(VARIANT), variants.length)];
    }

    /** Ticks this blade lives: the straight lifetime, or the locked flight plus its grace. */
    public int getLifetimeTicks() {
        int ticks = entityData.get(ARC_TICKS);
        return ticks > 0 ? ticks + ARC_GRACE_TICKS : LIFETIME_TICKS;
    }

    public boolean isLocked() { return entityData.get(ARC_TARGET) != NO_TARGET; }

    /** The target this blade flies at, while it is locked on and the target is still there to be hit. */
    public @Nullable LivingEntity getLockedTarget() {
        int id = entityData.get(ARC_TARGET);
        if (id == NO_TARGET) return null;
        return level().getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    /** The curve of a locked flight, or null for a straight one; the client reads it off the synced data. */
    public @Nullable MoonBladeArc getArc() {
        int ticks = entityData.get(ARC_TICKS);
        if (ticks <= 0 || !isLocked()) return null;
        if (arcStart == null) arcStart = position();
        return new MoonBladeArc(arcStart, new Vec3(entityData.get(ARC_BEND)), ticks);
    }

    /** The roll of a locked blade for its turn, in degrees, positive to the left; 0 on a straight flight. */
    public float getBankDegrees() {
        MoonBladeArc arc = getArc();
        LivingEntity target = getLockedTarget();
        return arc == null || target == null ? 0.0F : arc.bankDegrees(target.getBoundingBox().getCenter());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(VARIANT, (byte) Variant.MOON.ordinal());
        builder.define(ARC_TARGET, NO_TARGET);
        builder.define(ARC_BEND, new Vector3f());
        builder.define(ARC_TICKS, 0);
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
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() == HitResult.Type.MISS && target != null && arrives(target, motion)) {
            hit = new EntityHitResult(target, position().add(motion));
        }
        if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit)) {
            hitTargetOrDeflectSelf(hit);
        }
        if (isRemoved()) return;

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
    }

    /** Points this tick's motion along the arc at the target's current center; null, and motion kept, otherwise. */
    private @Nullable LivingEntity followArc() {
        MoonBladeArc arc = getArc();
        if (arc == null) return null;
        LivingEntity target = getLockedTarget();
        if (target == null) {
            if (!level().isClientSide()) entityData.set(ARC_TARGET, NO_TARGET);
            return null;
        }
        double t = Math.min(1.0D, tickCount / (double) arc.flightTicks());
        setDeltaMovement(arc.pointAt(target.getBoundingBox().getCenter(), t).subtract(position()));
        return target;
    }

    private boolean arrives(LivingEntity target, Vec3 motion) {
        return tickCount >= entityData.get(ARC_TICKS)
                || getBoundingBox().move(motion).intersects(target.getBoundingBox());
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel server)) return;

        Entity target = result.getEntity();
        Entity owner = getOwner();
        DamageSource source = ModDamageTypes.source(this, owner, ModDamageTypes.MOON_BLADE);
        if (target.hurt(source, damage)) {
            EnchantmentHelper.doPostAttackEffects(server, target, source);
            if (getVariant().bleeds() && target instanceof LivingEntity living) {
                living.addEffect(ModEffects.instance(ModEffects.BLEEDING, BLEEDING_TICKS), owner);
            }
        }
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel server)) return;

        Vec3 at = result.getLocation();
        ParticleOptions puff = getVariant().bleeds() ? BLOOD_PUFF : ParticleTypes.END_ROD;
        server.sendParticles(puff, at.x, at.y, at.z, PUFF_COUNT, 0.15D, 0.15D, 0.15D, 0.02D);
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
