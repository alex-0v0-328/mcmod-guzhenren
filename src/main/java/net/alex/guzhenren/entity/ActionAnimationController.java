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
 * Maps the synchronized actions of an {@link ActionEntity} onto GeckoLib 4's transition and playback clocks.
 *
 * <p>In {@link #process}, once a non-looping animation reaches {@code State.TRANSITIONING} with a
 * current animation, GeckoLib polls at transition tick zero, so it initializes then seeks in the
 * same frame -- otherwise a late tracker either has no pose or briefly displays the first frame.
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
        this.transitionLength(this.action.get().loops() ? 3 : 0);
        super.process(model, state, bones, snapshots, tick, crashIfBoneMissing);
        if (!this.action.get().loops() && this.getAnimationState() == State.TRANSITIONING
                && this.getCurrentAnimation() != null) {
            this.boneAnimationQueues.clear();
            super.process(model, state, bones, snapshots, tick, crashIfBoneMissing);
        }
    }

    @Override
    protected double adjustTick(double tick) {
        double normalTick = super.adjustTick(tick);
        if (this.action.get().loops()) return normalTick;
        return this.getAnimationState() == State.TRANSITIONING
                ? 0.0D : this.elapsedTicks.getAsLong() + this.partialTick;
    }
}
