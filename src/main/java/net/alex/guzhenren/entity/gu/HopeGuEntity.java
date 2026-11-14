package net.alex.guzhenren.entity.gu;

import java.util.function.Supplier;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * 野生希望蛊[Hope Gu]：世界自身的开窍[Awakening]来源。
 * <ul>
 *   <li>{@code seeks} 只盯未开窍者；任一玩家进入 {@code DETECT_RANGE} 起计 1 分钟消散，走远不停不重置；消散 tick 有意不
 *       存档，重载重计，反正会消失。</li>
 *   <li>⚠ 认任一玩家，不认特定玩家；计时一开，无法重开或重启。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.gu.FlyingGuEntity
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
    public boolean isSeeking(Player player) { return !ApertureService.isAwakened(player); }

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
