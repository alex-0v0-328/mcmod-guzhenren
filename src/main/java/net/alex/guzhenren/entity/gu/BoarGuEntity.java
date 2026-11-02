package net.alex.guzhenren.entity.gu;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * A wild boar Gu [野生豕蛊] that wanders freely instead of seeking players.
 *
 * <p>The shared {@link RestingFlyingGuEntity} lifecycle supplies the three movement goals, the
 * synchronized flight phase and the animation controller; the white, black and flower variants differ only in
 * their caught item and renderer texture. The ladybug model's animation contract is {@code animation.idle} while
 * resting, {@code animation.lift} for 0.8 seconds followed by looping {@code animation.fly}, and
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
}
