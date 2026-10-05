package net.alex.guzhenren.entity;

import java.util.function.Supplier;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The wild Hope Gu [希望蛊]: the world's own source of an awakening [开窍].
 *
 * <p>Extends {@link net.alex.guzhenren.entity.FlyingGuEntity}. Overrides {@code seeks} to target
 * only the unawakened -- a cultivator drifts past. Once any player comes within {@code DETECT_RANGE},
 * a one-minute fade clock starts; the window never reopens, and walking away neither resets nor pauses
 * it. The fade tick is unserialized on purpose -- a reloaded Gu starts over, and these despawn anyway.
 *
 * <p>⚠ It fades once any player has come into range, not once a particular one has. That sighting
 * starts a clock, and nothing restarts it afterward.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.FlyingGuEntity
 * @since 1.0.0
 */

public class HopeGuEntity extends FlyingGuEntity {

    private static final int FADE_TICKS = Ticks.MINUTE;
    private static final long NOT_SIGHTED = -1L;
    private static final int MOTES_PER_TICK = 1;
    private static final double MOTE_SPREAD = 0.25;
    private long fadeAtTick = NOT_SIGHTED;

    public HopeGuEntity(EntityType<? extends HopeGuEntity> type, Level level,
                        Supplier<Item> caughtGu) {
        super(type, level, caughtGu);
    }

    @Override
    public boolean seeks(Player player) { return !ApertureService.isAwakened(player); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (int i = 0; i < MOTES_PER_TICK; i++) {
                level().addParticle(ParticleTypes.END_ROD, getRandomX(MOTE_SPREAD), getRandomY(),
                        getRandomZ(MOTE_SPREAD), 0.0, 0.0, 0.0);
            }
            return;
        }

        if (fadeAtTick == NOT_SIGHTED) {
            if (level().getNearestPlayer(this, DETECT_RANGE) != null) {
                fadeAtTick = level().getGameTime() + FADE_TICKS;
            }
            return;
        }
        if (level().getGameTime() >= fadeAtTick) discard();
    }
}
