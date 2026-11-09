package net.alex.guzhenren.entity.beast;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * of a steady fist renew it before it goes, so it goes only {@link #getShownTicks} after the last round. Its
 * {@link Slot} places it in one of two layers seen from the front (Alex's layout): the ground layer,
 * {@link #FORWARD_OFFSET} ahead of the owner, puts the first {@link #SIDE_OFFSET} blocks to the right and the second as
 * far to the left; the back layer, {@link #FAN_BACK_OFFSET} behind the owner as if grown from the heels, is a fan of
 * {@link #FAN_ROWS} rows of pairs that opens downward from the apex, each row {@link #FAN_COLUMN} further out and
 * {@link #FAN_ROW} lower than the one above, the lowest row {@link #FAN_BASE_HEIGHT} up, so a third-person camera
 * sees the whole fan behind the owner and no fan slot stands straight behind a ground one. It plays its family's
 * attack once, then idles.
 *
 * <p>{@link #show} sets an owner's phantoms to a round's families: a family already up is renewed in its slot -- its
 * life starts over, nothing respawns -- a new one takes the first free slot in declaration order ({@link #pickSlot}:
 * the ground pair, then the fan from its lowest row up to its apex), and any other goes. A family that
 * cannot manifest ({@code BeastStrengthFamily.canManifest}) is left out and takes no slot. A renewal bumps a
 * synced counter so the client restarts its own age for the fade: in over {@link #FADE_IN_TICKS}, out over the last
 * {@link #FADE_OUT_TICKS}. ⚠ {@link #LINGER_TICKS} and the fan's {@link #FAN_BACK_OFFSET}, {@link #FAN_COLUMN},
 * {@link #FAN_ROW} and {@link #FAN_BASE_HEIGHT} are provisional, for Alex to tune in the client.
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
    public static final double FAN_BACK_OFFSET = 0.5D;
    public static final int FAN_ROWS = 4;
    public static final double FAN_COLUMN = 1.0D;
    public static final double FAN_ROW = 0.6D;
    public static final double FAN_BASE_HEIGHT = 0.5D;
    private static final double SEARCH_RANGE = 4.0D;
    private static final double CULL_MARGIN = 1.5D;
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

        RIGHT(0, true),
        LEFT(0, false),
        FAN_RIGHT_4(4, true),
        FAN_LEFT_4(4, false),
        FAN_RIGHT_3(3, true),
        FAN_LEFT_3(3, false),
        FAN_RIGHT_2(2, true),
        FAN_LEFT_2(2, false),
        FAN_RIGHT_1(1, true),
        FAN_LEFT_1(1, false);

        private final int row;
        private final boolean right;

        Slot(int row, boolean right) {
            this.row = row;
            this.right = right;
        }

        /** 0 for the ground layer, else the fan row counted from its apex. */
        public int getRow() { return this.row; }

        public boolean isRight() { return this.right; }
    }

    public BeastPhantomEntity(EntityType<? extends BeastPhantomEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void show(ServerPlayer owner, List<BeastStrengthFamily> drawn) {
        List<BeastStrengthFamily> families = BeastStrengthFamily.getManifesting(drawn);
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

            Slot slot = pickSlot(taken);
            if (slot == null) continue;

            taken.add(slot);
            summon(owner, family, slot);
        }
    }

    public static @Nullable Slot pickSlot(@NotNull Set<Slot> taken) {
        for (Slot slot : Slot.values()) if (!taken.contains(slot)) return slot;
        return null;
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

    public static Vec3 getOffset(Slot slot) {
        int row = slot.getRow();
        double side = row == 0 ? SIDE_OFFSET : FAN_COLUMN * (row - 0.5D);
        double up = row == 0 ? 0.0D : FAN_BASE_HEIGHT + FAN_ROW * (FAN_ROWS - row);
        double ahead = row == 0 ? FORWARD_OFFSET : -FAN_BACK_OFFSET;
        return new Vec3(slot.isRight() ? side : -side, up, ahead);
    }

    private void follow(Entity owner) {
        float yaw = owner.getYRot();
        double radians = Math.toRadians(yaw);
        Vec3 offset = getOffset(getSlot());
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
        Vec3 right = new Vec3(-Math.cos(radians), 0.0D, -Math.sin(radians));
        Vec3 position = owner.position().add(forward.scale(offset.z)).add(right.scale(offset.x)).add(0.0D, offset.y,
                0.0D);
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
