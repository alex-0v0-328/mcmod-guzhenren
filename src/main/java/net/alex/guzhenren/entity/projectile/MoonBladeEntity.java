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
import org.joml.Vector3f;

/**
 * A Moon Blade [月刃]: the crescent the Moonlight [月光蛊], Moonglow [月芒蛊] and Blood Moon Gu [血月蛊] fire. It flies
 * straight, without gravity or drag, at {@link #SPEED} blocks a tick for {@link #LIFETIME_TICKS}, and ends on the
 * first entity or block it meets; a Blood Moon Blade [血月刃] also leaves Bleeding [流血] on what it hurts.
 *
 * <p>The server owns the hit: the damage lives only there, set once by {@link #shoot}, and the client copy flies
 * the same line just to be drawn, from the synced {@link Variant} and its age. The blade is never saved: it lives
 * under a second, so a chunk unloading under it simply drops it.
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
    public static final int BLEEDING_TICKS = 5 * Ticks.SECOND;
    private static final int PUFF_COUNT = 6;
    private static final DustParticleOptions BLOOD_PUFF = new DustParticleOptions(new Vector3f(0.6F, 0.05F, 0.05F),
            1.0F);
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(MoonBladeEntity.class, EntityDataSerializers.BYTE);
    private float damage;

    public MoonBladeEntity(EntityType<? extends MoonBladeEntity> type, Level level) { super(type, level); }

    public static void shoot(ServerPlayer owner, Variant variant, float damage) {
        MoonBladeEntity blade = new MoonBladeEntity(ModEntityTypes.MOON_BLADE.get(), owner.level());
        blade.setOwner(owner);
        blade.entityData.set(VARIANT, (byte) variant.ordinal());
        blade.damage = damage;
        blade.setPos(owner.getX(), owner.getEyeY() - blade.getBbHeight() / 2.0F, owner.getZ());
        blade.shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0.0F, SPEED, 0.0F);
        owner.level().addFreshEntity(blade);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.8F, variant.bleeds() ? 0.8F : 1.4F);
    }

    public Variant getVariant() {
        Variant[] variants = Variant.values();
        return variants[Math.floorMod(entityData.get(VARIANT), variants.length)];
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(VARIANT, (byte) Variant.MOON.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > LIFETIME_TICKS) {
            discard();
            return;
        }
        Vec3 motion = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit)) {
            hitTargetOrDeflectSelf(hit);
        }
        if (isRemoved()) return;

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
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
    }

    @Override
    public boolean shouldBeSaved() { return false; }
}
