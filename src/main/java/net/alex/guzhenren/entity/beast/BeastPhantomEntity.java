package net.alex.guzhenren.entity.beast;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
 * A Beast Phantom [兽力虚影]: one beast family an attack round [攻击轮次] releases, shown for {@link #LIFETIME_TICKS}
 * and a further {@link #LINGER_TICKS} beside its owner, facing where the owner faces: the linger lets the next round
 * of a steady fist renew it before it goes, so it goes only {@link #getShownTicks} after the last round. Its {@link Slot} places it: the first {@link #SIDE_OFFSET} blocks
 * to the right and {@link #FORWARD_OFFSET} ahead, the second as far to the left, the third {@link #BACK_OFFSET}
 * straight behind. It plays its family's attack once, then idles.
 *
 * <p>{@link #show} sets an owner's phantoms to a round's families: a family already up is renewed in its slot -- its
 * life starts over, nothing respawns -- a new one takes the first free slot, and any other goes. A renewal bumps a
 * synced counter so the client restarts its own age for the fade: in over {@link #FADE_IN_TICKS}, out over the last
 * {@link #FADE_OUT_TICKS}. ⚠ {@link #LINGER_TICKS} is provisional.
 *
 * <p>A pure display: no collision, no hitbox for attacks or projectiles, no damage, never saved; one summoned by
 * command has no owner and goes on its first tick. Both sides place it from the owner every tick, so the client
 * ignores the server's position packets. It goes when its time runs out or its owner is gone from this level, dead,
 * or logged out, which covers death, dimension change and logout.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.path.strength.StrengthRound
 * @since 1.0.0
 */

public final class BeastPhantomEntity extends Entity implements GeoEntity, BeastPhantomVisual {

    public static final int LIFETIME_TICKS = 20;
    public static final int LINGER_TICKS = 5;
    public static final double SIDE_OFFSET = 1.2D;
    public static final double FORWARD_OFFSET = 0.5D;
    public static final double BACK_OFFSET = 1.2D;
    private static final double SEARCH_RANGE = 4.0D;
    private static final double CULL_MARGIN = 4.5D;
    private static final int NO_OWNER = -1;
    private static final EntityDataAccessor<Integer> OWNER =
            SynchedEntityData.defineId(BeastPhantomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FAMILY =
            SynchedEntityData.defineId(BeastPhantomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLOT =
            SynchedEntityData.defineId(BeastPhantomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RENEWALS =
            SynchedEntityData.defineId(BeastPhantomEntity.class, EntityDataSerializers.INT);
    private static final int FADE_IN_TICKS = 3;
    private static final Map<BeastStrengthFamily, RawAnimation> ATTACKS =
            BeastPhantomVisual.byFamily(family -> BeastPhantomVisual.getAttack(family, "idle"));
    private static final int FADE_OUT_TICKS = 10;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int bornTick;

    public enum Slot {

        RIGHT,
        LEFT,
        BACK
    }

    public BeastPhantomEntity(EntityType<? extends BeastPhantomEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void show(ServerPlayer owner, List<BeastStrengthFamily> families) {
        List<BeastPhantomEntity> owned = findOwned(owner);
        EnumSet<Slot> taken = EnumSet.noneOf(Slot.class);
        for (BeastPhantomEntity phantom : owned) {
            if (!families.contains(phantom.getFamily())) {
                phantom.discard();
                continue;
            }
            phantom.renew();
            taken.add(phantom.getSlot());
        }
        for (BeastStrengthFamily family : families) {
            if (owned.stream().anyMatch(phantom -> phantom.isAlive() && phantom.getFamily() == family)) continue;

            Slot slot = Arrays.stream(Slot.values()).filter(free -> !taken.contains(free)).findFirst().orElse(null);
            if (slot == null) return;

            taken.add(slot);
            summon(owner, family, slot);
        }
    }

    public static BeastPhantomEntity summon(ServerPlayer owner, BeastStrengthFamily family, Slot slot) {
        BeastPhantomEntity phantom = new BeastPhantomEntity(ModEntityTypes.BEAST_PHANTOM.get(), owner.level());
        phantom.entityData.set(OWNER, owner.getId());
        phantom.entityData.set(FAMILY, family.ordinal());
        phantom.entityData.set(SLOT, slot.ordinal());
        phantom.follow(owner);
        phantom.xo = phantom.getX();
        phantom.yo = phantom.getY();
        phantom.zo = phantom.getZ();
        phantom.yRotO = phantom.getYRot();
        owner.level().addFreshEntity(phantom);
        return phantom;
    }

    public static List<BeastPhantomEntity> findOwned(ServerPlayer owner) {
        AABB area = owner.getBoundingBox().inflate(SEARCH_RANGE);
        return owner.level().getEntitiesOfClass(BeastPhantomEntity.class, area,
                phantom -> phantom.isAlive() && phantom.getOwnerId() == owner.getId());
    }

    public static @Nullable BeastPhantomEntity findOwned(ServerPlayer owner, BeastStrengthFamily family) {
        return findOwned(owner).stream().filter(phantom -> phantom.getFamily() == family).findFirst().orElse(null);
    }

    public void renew() {
        this.bornTick = this.tickCount;
        this.entityData.set(RENEWALS, this.entityData.get(RENEWALS) + 1);
    }

    public static int getShownTicks() { return LIFETIME_TICKS + LINGER_TICKS; }

    public float getAge(float partialTick) { return this.tickCount - this.bornTick + partialTick; }

    @Override
    public float getFade(float partialTick) {
        float age = getAge(partialTick);
        return Mth.clamp(Math.min(age / FADE_IN_TICKS, (getShownTicks() - age) / FADE_OUT_TICKS), 0.0F, 1.0F);
    }

    public int getOwnerId() { return this.entityData.get(OWNER); }

    @Override
    public BeastStrengthFamily getFamily() {
        BeastStrengthFamily[] families = BeastStrengthFamily.values();
        return families[Mth.clamp(this.entityData.get(FAMILY), 0, families.length - 1)];
    }

    public Slot getSlot() {
        Slot[] slots = Slot.values();
        return slots[Mth.clamp(this.entityData.get(SLOT), 0, slots.length - 1)];
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(OWNER, NO_OWNER);
        builder.define(FAMILY, BeastStrengthFamily.BOAR.ordinal());
        builder.define(SLOT, Slot.RIGHT.ordinal());
        builder.define(RENEWALS, 0);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (RENEWALS.equals(key) && this.level().isClientSide()) this.bornTick = this.tickCount;
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = this.level().getEntity(getOwnerId());
        if (!this.level().isClientSide()
                && (!(owner instanceof Player player) || !player.isAlive()
                || this.tickCount - this.bornTick > getShownTicks())) {
            this.discard();
            return;
        }
        if (owner != null) follow(owner);
    }

    private void follow(Entity owner) {
        float yaw = owner.getYRot();
        double radians = Math.toRadians(yaw);
        Slot slot = getSlot();
        double side = switch (slot) {
            case RIGHT -> SIDE_OFFSET;
            case LEFT -> -SIDE_OFFSET;
            case BACK -> 0.0D;
        };
        double ahead = slot == Slot.BACK ? -BACK_OFFSET : FORWARD_OFFSET;
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
        Vec3 right = new Vec3(-Math.cos(radians), 0.0D, -Math.sin(radians));
        Vec3 position = owner.position().add(forward.scale(ahead)).add(right.scale(side));
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
                state -> state.setAndContinue(ATTACKS.get(getFamily()))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
    //endregion
}
