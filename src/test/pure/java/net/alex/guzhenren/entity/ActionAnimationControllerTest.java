package net.alex.guzhenren.entity;

import com.google.gson.JsonParser;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.util.GeckoLibUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ActionAnimationControllerTest {

    @Test
    void lateTrackerSamplesCurrentActionInsteadOfFirstFrame() {
        Clock clock = new Clock();
        Probe early = new Probe(clock);
        for (int elapsed = 0; elapsed <= 9; elapsed++) {
            clock.elapsed = elapsed;
            assertEquals(elapsed + 0.25D, early.sample(elapsed + 100.25D));
        }
        Probe late = new Probe(clock);
        assertEquals(9.25D, late.sample(0.25D));
    }

    @Test
    void sameActionSequenceRestartsEvenWithinOneRenderTick() {
        Clock clock = new Clock();
        Probe probe = new Probe(clock);
        clock.elapsed = 9;
        assertEquals(9.25D, probe.sample(100.25D));
        clock.elapsed = 0;
        clock.sequence++;
        assertEquals(0.25D, probe.sample(100.25D));
        clock.elapsed = 3;
        assertEquals(3.25D, probe.sample(103.25D));
    }

    private static final class Clock implements GeoAnimatable {

        private long elapsed;
        private int sequence;
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

        @Override
        public double getTick(Object related) { return this.elapsed; }
    }

    private static final class Probe {

        private final Clock clock;
        private final GeoBone bone = new GeoBone(null, "probe", false, 0.0D, false, false);
        private final GeoModel<Clock> model;
        private final AnimationController<Clock> controller;

        private Probe(Clock clock) {
            this.clock = clock;
            BakedAnimations animations = KeyFramesAdapter.GEO_GSON.fromJson(JsonParser.parseString("""
                    {"probe":{"animation_length":2,"bones":{"probe":{"position":{
                    "0":[0,0,0],"2":[40,0,0]}}}}}
                    """), BakedAnimations.class);
            Animation linear = animations.getAnimation("probe");
            this.model = new GeoModel<>() {
                @Override
                public ResourceLocation getModelResource(Clock entity) { return Guzhenren.id("probe"); }

                @Override
                public ResourceLocation getTextureResource(Clock entity) { return Guzhenren.id("probe"); }

                @Override
                public ResourceLocation getAnimationResource(Clock entity) { return Guzhenren.id("probe"); }

                @Override
                public Animation getAnimation(Clock entity, String name) { return linear; }
            };
            this.bone.saveInitialSnapshot();
            this.model.getAnimationProcessor().registerGeoBone(this.bone);
            this.controller = new ActionAnimationController<>(clock,
                    state -> state.setAndContinue(RawAnimation.begin().thenPlay("probe")),
                    () -> WildBoarEntity.Action.HURT, () -> clock.elapsed, () -> clock.sequence);
        }

        private double sample(double renderTick) {
            var state = new AnimationState<>(this.clock, 0.0F, 0.0F, 0.25F, false).withController(this.controller);
            this.controller.process(this.model, state, Map.of("probe", this.bone),
                    Map.of("probe", this.bone.saveSnapshot()), renderTick, true);
            var points = this.controller.getBoneAnimationQueues().get("probe").positionXQueue();
            assertFalse(points.isEmpty(), "Controller did not produce a pose");
            double tick = points.getLast().currentTick();
            this.controller.getBoneAnimationQueues().clear();
            return tick;
        }
    }
}
