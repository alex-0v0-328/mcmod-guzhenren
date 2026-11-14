package net.alex.guzhenren.entity;

import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * {@link ActionEntity} 的 GeckoLib 4 动画控制器：同步动作映射为过渡与播放时钟；非循环动画带当前动画进
 * {@code State.TRANSITIONING} 时 GeckoLib 于过渡 tick 0 轮询，故 {@link #process} 同帧先初始化再定位，免迟到的追踪者无
 * 姿态或闪首帧。
 */

final class ActionAnimationController<T extends GeoAnimatable> extends AnimationController<T> {

    private final Supplier<? extends ActionEntity.ActionFlags> action;
    private final LongSupplier elapsedTicks;
    private final IntSupplier sequence;
    private int lastSequence = Integer.MIN_VALUE;
    private double partialTick;

    ActionAnimationController(T animatable, AnimationStateHandler<T> handler,
                              Supplier<? extends ActionEntity.ActionFlags> action, LongSupplier elapsedTicks,
                              IntSupplier sequence) {
        super(animatable, "main", 3, handler);
        this.action = action;
        this.elapsedTicks = elapsedTicks;
        this.sequence = sequence;
    }

    @Override
    public void process(GeoModel<T> model, AnimationState<T> state, Map<String, GeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double tick, boolean crashIfBoneMissing) {
        if (this.lastSequence != this.sequence.getAsInt()) {
            this.lastSequence = this.sequence.getAsInt();
            this.forceAnimationReset();
            this.lastPollTime = Double.NEGATIVE_INFINITY;
        }
        this.partialTick = state.getPartialTick();
        this.transitionLength(this.action.get().isLooping() ? 3 : 0);
        super.process(model, state, bones, snapshots, tick, crashIfBoneMissing);
        if (!this.action.get().isLooping() && this.getAnimationState() == State.TRANSITIONING
                && this.getCurrentAnimation() != null) {
            this.boneAnimationQueues.clear();
            super.process(model, state, bones, snapshots, tick, crashIfBoneMissing);
        }
    }

    @Override
    protected double adjustTick(double tick) {
        double normalTick = super.adjustTick(tick);
        if (this.action.get().isLooping()) return normalTick;
        return this.getAnimationState() == State.TRANSITIONING
                ? 0.0D : this.elapsedTicks.getAsLong() + this.partialTick;
    }
}
