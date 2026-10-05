package net.alex.guzhenren.entity;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;

/**
 * A wild boar Gu [野生豕蛊] that wanders freely instead of seeking players.
 *
 * <p>The shared {@link RestingFlyingGuEntity} lifecycle supplies the three movement goals and the
 * synchronized flight phase; the white, black and flower variants differ only in their caught item and
 * renderer texture. The ladybug model's animation contract is {@code animation.idle} while resting,
 * {@code animation.lift} for 0.8 seconds followed by looping {@code animation.fly}, and
 * {@code animation.land} for 0.8 seconds followed by looping {@code animation.idle}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class BoarGuEntity extends RestingFlyingGuEntity {

    public BoarGuEntity(EntityType<? extends BoarGuEntity> type, Level level, Supplier<Item> caughtGu) {
        super(type, level, caughtGu);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, MAIN, TRANSITION_TICKS, state -> switch (phase()) {
            case FLYING, LANDING -> state.setAndContinue(FLY_ANIM);
            case RESTING -> state.setAndContinue(IDLE_ANIM);
        }).triggerableAnim("lift", LIFT_ANIM).triggerableAnim("land", LAND_ANIM));
    }
}
