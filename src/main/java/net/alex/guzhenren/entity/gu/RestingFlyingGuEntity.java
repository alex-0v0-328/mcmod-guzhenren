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
 * 具备「飞行—降落—地面歇息」三阶段生命周期的野生飞行蛊基类。
 * <ul>
 *   <li>飞行阶段（{@link FlightPhase}）与降落请求由服务端维护并同步至实体数据，使客户端在生成、进入追踪或区块加载时都能
 *       选对常驻循环动画；起飞与着地过渡动画仅在服务端阶段切换时触发，后加入追踪的客户端直接播放常驻状态。</li>
 *   <li>GeckoLib 动画统一由 {@link #createMainController} 创建的 {@code main} 控制器驱动，常驻循环取自
 *       {@link #getSteadyAnimation}，并注册 {@code lift}（起飞）与 {@code land}（着地）两个可触发过渡动画。</li>
 *   <li>{@link #takeOff} 仅在从 {@link FlightPhase#RESTING} 起飞时触发 {@code lift} 动画：只有歇息状态收起了鞘翅，半途
 *       中止的降落原本就处于展翅飞行姿态。</li>
 *   <li>{@link #getMaxHeadYRot} 固定返回 {@code 0}：甲虫与瓢虫模型无独立头骨，转向全靠旋转身体；若保留原版 75 度头身偏
 *       角上限，歇息时小幅转头会被身体滞后吞掉。</li>
 *   <li>{@link #readAdditionalSaveData} 恢复阶段与降落标志以弥补 AI 目标自身不存档：读档时若处于降落阶段则自动补挂降落
 *       请求交由 {@link LandRestGoal} 接管，若处于歇息阶段则由 {@link LandRestGoal} 直接进入歇息计时而不重播降落。</li>
 * </ul>
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
