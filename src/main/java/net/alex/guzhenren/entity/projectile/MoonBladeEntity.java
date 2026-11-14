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
 * 月刃：月光蛊[Moonlight Gu]、月芒蛊[Moonglow Gu]、血月蛊[Blood Moon Gu]射出的月牙。
 * <ul>
 *   <li>无目标直飞，无重力阻力，每 tick {@link #SPEED} 格，活 {@link #LIFETIME_TICKS}，遇首个实体或方块即止；血月刃附流
 *       血。</li>
 *   <li>锁定目标时改飞 {@link MoonBladeArc}：每 tick 重读目标中心，每发掷一次左右弯；飞行 tick 内走完弦，到时或碰到目标
 *       即命中，落点即目标；方块或他物照样拦截；目标中途消失（死亡、移除、卸载）则弃弧直飞。</li>
 *   <li>命中归服务端，伤害由 {@link #shoot} 设定；客户端按同步数据飞同一路径，仅供绘制；不存档：只活约 1 秒，区块卸载即
 *       丢；每刃发布一个 {@link ProjectileThrownEvent}，射手据此做投掷动作。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.mortal.path.moon.MoonBladeBehavior
 * @since 1.0.0
 */

public final class MoonBladeEntity extends Projectile {

    /** 两种刃：贴图、命中烟雾、是否致流血。 */
    public enum Variant {
        MOON(false), BLOOD(true);

        private final boolean bleeds;

        Variant(boolean bleeds) { this.bleeds = bleeds; }

        public boolean bleeds() { return bleeds; }
    }

    public static final float SPEED = 1.5F;
    public static final int LIFETIME_TICKS = 16;
    /** 锁定刃比飞行多活的 tick，末 tick 命中在消失前仍生效。 */
    public static final int ARC_GRACE_TICKS = 2;
    /** 锁定飞行上限；{@code KillingMoveService} 只锁 26 格内（18 tick），在上限内。 */
    public static final int MAX_ARC_FLIGHT_TICKS = 20;
    private static final int NO_TARGET = -1;
    private static final Variant[] VARIANTS = Variant.values();
    /** 刃出现在眼下方的格数，射手看到刃面而非刃边。 */
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

    /** 发射一刃；可命中的 {@code target} 使其锁定飞行，否则直飞。 */
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

    /** 存活 tick：直飞寿命，或锁定飞行加宽限。 */
    public int getLifetimeTicks() {
        int ticks = entityData.get(ARC_TICKS);
        return ticks > 0 ? ticks + ARC_GRACE_TICKS : LIFETIME_TICKS;
    }

    public boolean isLocked() { return entityData.get(ARC_TARGET) != NO_TARGET; }

    /** 飞向的目标；仅锁定中且目标仍可命中时有值。 */
    private @Nullable LivingEntity getLockedTarget() {
        int id = entityData.get(ARC_TARGET);
        if (id == NO_TARGET) return null;
        return level().getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    /** 锁定飞行的曲线，直飞为 null；客户端从同步数据读。 */
    public @Nullable MoonBladeArc getArc() {
        int ticks = entityData.get(ARC_TICKS);
        if (ticks <= 0 || !isLocked()) return null;
        if (arcStart == null) arcStart = position();
        return new MoonBladeArc(arcStart, new Vec3(entityData.get(ARC_BEND)), ticks);
    }

    /** 锁定刃转弯侧倾角（度），向左为正；直飞为 0。 */
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

    /** 沿弧线把本 tick 运动指向目标当前中心；否则返回 null，运动不变。 */
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
