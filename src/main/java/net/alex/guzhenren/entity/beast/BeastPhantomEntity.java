package net.alex.guzhenren.entity.beast;

import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Beast Phantom [兽力虚影]: the beast whose full strength one hit just brought to bear, shown for
 * {@link #LIFETIME_TICKS} beside its owner, {@link #SIDE_OFFSET} blocks to a side picked at random and
 * {@link #FORWARD_OFFSET} ahead, facing where the owner faces. It plays its family's attack once, then idles.
 *
 * <p>A pure display: no collision, no hitbox for attacks or projectiles, never saved; one summoned by command has
 * no owner and goes on its first tick. Both sides place it from the owner every tick, so the client ignores the server's position packets.
 * It goes when its time runs out or its owner is gone from this level, dead, or logged out, which covers death,
 * dimension change and logout. {@link #findOwned} is how the one-phantom rule asks whether one is still up.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.path.strength.BeastPhantomEvents
 * @since 1.0.0
 */

public final class BeastPhantomEntity extends Entity implements GeoEntity {

    public static final int LIFETIME_TICKS = 40;
    public static final double SIDE_OFFSET = 1.2D;
    public static final double FORWARD_OFFSET = 0.5D;
    private static final double SEARCH_RANGE = 4.0D;
    private static final double CULL_MARGIN = 1.5D;
    private static final int NO_OWNER = -1;
    private static final EntityDataAccessor<Integer> OWNER =
            SynchedEntityData.defineId(BeastPhantomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FAMILY =
            SynchedEntityData.defineId(BeastPhantomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RIGHT_SIDE =
            SynchedEntityData.defineId(BeastPhantomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation BOAR_ATTACK =
            RawAnimation.begin().thenPlay("animation.attack_toss").thenLoop("animation.idle");
    private static final RawAnimation SWIPE_ATTACK =
            RawAnimation.begin().thenPlay("animation.attack_swipe").thenLoop("animation.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BeastPhantomEntity(EntityType<? extends BeastPhantomEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static BeastPhantomEntity summon(ServerPlayer owner, BeastStrengthFamily family, boolean rightSide) {
        BeastPhantomEntity phantom = new BeastPhantomEntity(ModEntityTypes.BEAST_PHANTOM.get(), owner.level());
        phantom.entityData.set(OWNER, owner.getId());
        phantom.entityData.set(FAMILY, family.ordinal());
        phantom.entityData.set(RIGHT_SIDE, rightSide);
        phantom.follow(owner);
        phantom.xo = phantom.getX();
        phantom.yo = phantom.getY();
        phantom.zo = phantom.getZ();
        phantom.yRotO = phantom.getYRot();
        owner.level().addFreshEntity(phantom);
        return phantom;
    }

    public static @Nullable BeastPhantomEntity findOwned(ServerPlayer owner) {
        AABB area = owner.getBoundingBox().inflate(SEARCH_RANGE);
        return owner.level().getEntitiesOfClass(BeastPhantomEntity.class, area,
                phantom -> phantom.isAlive() && phantom.getOwnerId() == owner.getId()).stream().findFirst().orElse(null);
    }

    public int getOwnerId() { return this.entityData.get(OWNER); }

    public BeastStrengthFamily getFamily() {
        BeastStrengthFamily[] families = BeastStrengthFamily.values();
        return families[Mth.clamp(this.entityData.get(FAMILY), 0, families.length - 1)];
    }

    public boolean isRightSide() { return this.entityData.get(RIGHT_SIDE); }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(OWNER, NO_OWNER);
        builder.define(FAMILY, BeastStrengthFamily.BOAR.ordinal());
        builder.define(RIGHT_SIDE, true);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = this.level().getEntity(getOwnerId());
        if (!this.level().isClientSide()
                && (!(owner instanceof Player player) || !player.isAlive() || this.tickCount >= LIFETIME_TICKS)) {
            this.discard();
            return;
        }
        if (owner != null) follow(owner);
    }

    private void follow(Entity owner) {
        float yaw = owner.getYRot();
        double radians = Math.toRadians(yaw);
        double side = isRightSide() ? SIDE_OFFSET : -SIDE_OFFSET;
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
        Vec3 right = new Vec3(-Math.cos(radians), 0.0D, -Math.sin(radians));
        Vec3 position = owner.position().add(forward.scale(FORWARD_OFFSET)).add(right.scale(side));
        this.setPos(position);
        this.setYRot(yaw);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {}

    @Override
    public @NotNull AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(CULL_MARGIN); }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isAttackable() { return false; }

    @Override
    public boolean canBeHitByProjectile() { return false; }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {}

    //region Animation [动画]
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "attack", 0,
                state -> state.setAndContinue(getFamily() == BeastStrengthFamily.BOAR ? BOAR_ATTACK : SWIPE_ATTACK)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
    //endregion
}
