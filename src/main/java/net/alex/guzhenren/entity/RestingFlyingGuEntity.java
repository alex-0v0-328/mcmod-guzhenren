package net.alex.guzhenren.entity;

import java.util.function.Supplier;
import net.alex.guzhenren.entity.ai.FleePlayerGoal;
import net.alex.guzhenren.entity.ai.LandRestGoal;
import net.alex.guzhenren.entity.ai.WanderCourseGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A flying Gu [飞行蛊] with a shared flight, landing and rest lifecycle.
 *
 * <p>The server owns the phase and landing request. Both values are synchronized entity data, so a
 * client can select the correct steady animation after spawning, tracking or loading an entity. The
 * server triggers the concrete entity's one-shot transition animation when a phase transition needs
 * one; a newly tracked or loaded client only selects the synchronized steady state.
 *
 * <p>The GeckoLib side is shared too: one {@code main} controller, the idle, fly, lift and land
 * animations, and the lift and land triggers fired on take-off and landing.
 *
 * <p>{@link #takeOff}: only a resting Gu has closed its wing cases; an aborted landing is still in
 * the flight pose.
 *
 * <p>{@link #readAdditionalSaveData}: goal state is not serialized. A saved landing re-arms its
 * request so the landing goal picks it up again; a saved rest is resumed by that goal directly,
 * without replaying the landing.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public abstract class RestingFlyingGuEntity extends FlyingGuEntity implements GeoEntity {

    protected static final String MAIN = "main";
    protected static final int TRANSITION_TICKS = 5;
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.idle");
    protected static final RawAnimation FLY_ANIM = RawAnimation.begin().thenLoop("animation.fly");
    protected static final RawAnimation LIFT_ANIM = RawAnimation.begin().thenPlay("animation.lift");
    protected static final RawAnimation LAND_ANIM = RawAnimation.begin().thenPlay("animation.land");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final double FLEE_RANGE = 6.0D;
    public static final double ESCAPE_RANGE = 10.0D;
    private static final EntityDataAccessor<Byte> DATA_FLIGHT_PHASE = SynchedEntityData.defineId(
            RestingFlyingGuEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_WANTS_TO_LAND = SynchedEntityData.defineId(
            RestingFlyingGuEntity.class, EntityDataSerializers.BOOLEAN);

    protected RestingFlyingGuEntity(EntityType<? extends RestingFlyingGuEntity> type, Level level,
                                    Supplier<Item> caughtGu) {
        super(type, level, caughtGu);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FleePlayerGoal(this));
        goalSelector.addGoal(1, new LandRestGoal(this));
        goalSelector.addGoal(2, new WanderCourseGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLIGHT_PHASE, FlightPhase.FLYING.id());
        builder.define(DATA_WANTS_TO_LAND, false);
    }

    public FlightPhase phase() {
        return FlightPhase.fromId(entityData.get(DATA_FLIGHT_PHASE));
    }

    public boolean wantsToLand() {
        return entityData.get(DATA_WANTS_TO_LAND);
    }

    public void requestLanding() {
        entityData.set(DATA_WANTS_TO_LAND, true);
    }

    public void beginLanding() {
        entityData.set(DATA_WANTS_TO_LAND, true);
        setPhase(FlightPhase.LANDING);
    }

    public void beginResting() {
        setPhase(FlightPhase.RESTING);
        entityData.set(DATA_WANTS_TO_LAND, false);
        if (!level().isClientSide()) playLandingAnimation();
    }

    public void takeOff() {
        boolean grounded = phase() == FlightPhase.RESTING;
        setPhase(FlightPhase.FLYING);
        entityData.set(DATA_WANTS_TO_LAND, false);
        if (grounded && !level().isClientSide()) playTakeoffAnimation();
    }

    private void setPhase(FlightPhase next) {
        if (phase() != next) entityData.set(DATA_FLIGHT_PHASE, next.id());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("FlightPhase", phase().id());
        tag.putBoolean("WantsToLand", wantsToLand());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        FlightPhase savedPhase = FlightPhase.fromId(tag.getByte("FlightPhase"));
        entityData.set(DATA_FLIGHT_PHASE, savedPhase.id());
        boolean wantsToLand = tag.getBoolean("WantsToLand");
        if (savedPhase == FlightPhase.LANDING) wantsToLand = true;
        entityData.set(DATA_WANTS_TO_LAND, wantsToLand);
    }

    protected void playLandingAnimation() { triggerAnim(MAIN, "land"); }

    protected void playTakeoffAnimation() { triggerAnim(MAIN, "lift"); }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public enum FlightPhase {

        FLYING,
        LANDING,
        RESTING;

        private byte id() {
            return (byte) ordinal();
        }

        private static FlightPhase fromId(byte id) {
            FlightPhase[] phases = values();
            return id >= 0 && id < phases.length ? phases[id] : FLYING;
        }
    }
}
