package net.alex.guzhenren.entity.gu;

import java.util.function.Supplier;
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
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 共用飞行、降落、歇息生命周期的飞行蛊。
 * <ul>
 *   <li>阶段与降落请求归服务端，同步为实体数据，客户端生成、追踪、加载后都能选对常驻动画；过渡动画仅服务端切换时触发，
 *       新追踪或加载的客户端只看常驻状态。</li>
 *   <li>GeckoLib 共用 {@code main} 控制器（{@link #createMainController}）循环 {@link #getSteadyAnimation}，含待机、飞
 *       行、起降动画与触发。</li>
 *   <li>{@link #takeOff} 只对歇息的蛊播起飞：只有它收着鞘翅，中止的降落仍是飞行姿势。</li>
 *   <li>{@link #getMaxHeadYRot} 为 0：模型无头骨，转头全靠身体；原版 75 度下身体落后头部，歇息转向难以看出。</li>
 *   <li>{@link #readAdditionalSaveData}：AI 目标状态不存档；存档降落重挂请求交降落目标，存档歇息由其直接接回，不重播降
 *       落。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public abstract class RestingFlyingGuEntity extends FlyingGuEntity implements GeoEntity {

    protected static final String MAIN = "main";
    protected static final int TRANSITION_TICKS = 5;
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation FLY_ANIM = RawAnimation.begin().thenLoop("animation.fly");
    private static final RawAnimation LIFT_ANIM = RawAnimation.begin().thenPlay("animation.lift");
    private static final RawAnimation LAND_ANIM = RawAnimation.begin().thenPlay("animation.land");
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
        builder.define(DATA_FLIGHT_PHASE, FlightPhase.FLYING.getPhaseId());
        builder.define(DATA_WANTS_TO_LAND, false);
    }

    public FlightPhase getPhase() {
        return FlightPhase.fromId(entityData.get(DATA_FLIGHT_PHASE));
    }

    public boolean isWantingToLand() {
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
        boolean grounded = getPhase() == FlightPhase.RESTING;
        setPhase(FlightPhase.FLYING);
        entityData.set(DATA_WANTS_TO_LAND, false);
        if (grounded && !level().isClientSide()) playTakeoffAnimation();
    }

    private void setPhase(FlightPhase next) {
        if (getPhase() != next) entityData.set(DATA_FLIGHT_PHASE, next.getPhaseId());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("FlightPhase", getPhase().getPhaseId());
        tag.putBoolean("WantsToLand", isWantingToLand());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        FlightPhase savedPhase = FlightPhase.fromId(tag.getByte("FlightPhase"));
        entityData.set(DATA_FLIGHT_PHASE, savedPhase.getPhaseId());
        boolean wantsToLand = tag.getBoolean("WantsToLand");
        if (savedPhase == FlightPhase.LANDING) wantsToLand = true;
        entityData.set(DATA_WANTS_TO_LAND, wantsToLand);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(createMainController());
    }

    protected AnimationController<RestingFlyingGuEntity> createMainController() {
        return new AnimationController<>(this, MAIN, TRANSITION_TICKS,
                state -> state.setAndContinue(getSteadyAnimation()))
                .triggerableAnim("lift", LIFT_ANIM).triggerableAnim("land", LAND_ANIM);
    }

    protected RawAnimation getSteadyAnimation() { return getPhase() == FlightPhase.RESTING ? IDLE_ANIM : FLY_ANIM; }

    protected void playLandingAnimation() { triggerAnim(MAIN, "land"); }

    protected void playTakeoffAnimation() { triggerAnim(MAIN, "lift"); }

    @Override
    public int getMaxHeadYRot() { return 0; }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public enum FlightPhase {

        FLYING,
        LANDING,
        RESTING;

        private byte getPhaseId() {
            return (byte) ordinal();
        }

        private static FlightPhase fromId(byte id) {
            FlightPhase[] phases = values();
            return id >= 0 && id < phases.length ? phases[id] : FLYING;
        }
    }
}
