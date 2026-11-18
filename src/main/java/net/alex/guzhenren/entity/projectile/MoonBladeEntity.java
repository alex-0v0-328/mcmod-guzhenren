package net.alex.guzhenren.entity.projectile;

import java.util.function.Predicate;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * 月刃弹射物：月光蛊[Moonlight Gu]、月芒蛊[Moonglow Gu]与血月蛊[Blood Moon Gu]射出的月牙气刃（{@link Variant} 区分普通
 * 月刃与附带流血与血雾粒子的血月刃）。
 * <ul>
 *   <li>{@link #shoot(ServerPlayer, Variant, float, LivingEntity)} 在射手眼部下方 {@value #SPAWN_DROP} 格处生成月刃（使
 *       射手看到刃面而非刃边）：未传入可命中目标时沿视线直飞，不受重力与阻力影响，速度为 {@link #SPEED} 格/tick，存活
 *       {@link #LIFETIME_TICKS} tick，碰撞首个实体或方块即结算并销毁。</li>
 *   <li>传入可命中目标时改沿 {@link MoonBladeArc} 锁定飞行（{@link #getArc}，直飞时为 {@code null}；渲染侧倾角由
 *       {@link #getBankDegrees} 给出，向左为正、直飞为 0）：{@link #followArc} 每 tick 重读目标当前中心并更新速度向量，
 *       飞行 tick 上限为 {@link #MAX_ARC_FLIGHT_TICKS}（{@code KillingMoveService} 锁定距离不超过 26 格即 18 tick，处于
 *       上限内），存活寿命额外延长 {@link #ARC_GRACE_TICKS} tick（{@link #getLifetimeTicks}）以确保末 tick 命中在销毁前
 *       生效；若目标中途死亡、移除或卸载，则清除锁定并沿当前速度直飞。</li>
 *   <li>命中与伤害结算由服务端权威执行，客户端按同步数据重建同一飞行轨迹仅供渲染；因仅存活约 1 秒，实体不写入存档、区块
 *       卸载即丢弃；每次发射均在游戏事件总线发布 {@link ProjectileThrownEvent} 供射手播放投掷动作。</li>
 * </ul>
 */

public final class MoonBladeEntity extends Projectile {

    public enum Variant {
        MOON(false), BLOOD(true);

        private final boolean bleeds;

        Variant(boolean bleeds) { this.bleeds = bleeds; }

        public boolean bleeds() { return bleeds; }
    }

    public static final float SPEED = 1.5F;
    public static final int LIFETIME_TICKS = 16;
    public static final int ARC_GRACE_TICKS = 2;
    public static final int MAX_ARC_FLIGHT_TICKS = 20;
    private static final int NO_TARGET = -1;
    private static final Variant[] VARIANTS = Variant.values();
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
    private final Predicate<Entity> hittable = this::canHitEntity;
    private float damage;
    private @Nullable Vec3 arcStart;

    public MoonBladeEntity(EntityType<? extends MoonBladeEntity> type, Level level) { super(type, level); }

    public static void shoot(ServerPlayer owner, Variant variant, float damage) {
        shoot(owner, variant, damage, null);
    }

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
        NeoForge.EVENT_BUS.post(new ProjectileThrownEvent(owner));
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
        return VARIANTS[Math.floorMod(entityData.get(VARIANT), VARIANTS.length)];
    }

    public int getLifetimeTicks() {
        int ticks = entityData.get(ARC_TICKS);
        return ticks > 0 ? ticks + ARC_GRACE_TICKS : LIFETIME_TICKS;
    }

    public boolean isLocked() { return entityData.get(ARC_TARGET) != NO_TARGET; }

    private @Nullable LivingEntity getLockedTarget() {
        int id = entityData.get(ARC_TARGET);
        if (id == NO_TARGET) return null;
        return level().getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    public @Nullable MoonBladeArc getArc() {
        int ticks = entityData.get(ARC_TICKS);
        if (ticks <= 0 || !isLocked()) return null;
        if (arcStart == null) arcStart = position();
        return new MoonBladeArc(arcStart, new Vec3(entityData.get(ARC_BEND)), ticks);
    }

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
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, hittable);
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
