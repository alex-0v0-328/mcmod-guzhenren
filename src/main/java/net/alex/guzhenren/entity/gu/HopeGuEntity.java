package net.alex.guzhenren.entity.gu;

import java.util.function.Supplier;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
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
 *   <li>世界里没有模型，身体是客户端 Photon 萤光（{@code HopeGuEffectEvents}）；消散前 {@link #FADE_BURST_TICKS} tick 广播
 *       {@link #FADE_EVENT}，客户端据 {@link #isFading} 换小帧并四向散开。</li>
 *   <li>发光不在代码：{@code assets/guzhenren/dynamiclights/entity/} 的 LambDynamicLights 定义，没装该模组就只有粒子全亮。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.gu.FlyingGuEntity
 * @since 1.0.0
 */

public class HopeGuEntity extends FlyingGuEntity {

    public static final byte FADE_EVENT = 100;
    private static final int FADE_TICKS = Ticks.MINUTE;
    private static final int FADE_BURST_TICKS = 4;
    private static final long NOT_SIGHTED = -1L;
    private long fadeAtTick = NOT_SIGHTED;
    private boolean fading;

    public HopeGuEntity(EntityType<? extends HopeGuEntity> type, Level level,
                        Supplier<Item> caughtGu) {
        super(type, level, caughtGu);
    }

    @Override
    public boolean isSeeking(Player player) { return !ApertureService.isAwakened(player); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (fadeAtTick == NOT_SIGHTED) {
            if (level().getNearestPlayer(this, DETECT_RANGE) != null) {
                fadeAtTick = level().getGameTime() + FADE_TICKS;
            }
            return;
        }
        long now = level().getGameTime();
        if (!fading && now >= fadeAtTick - FADE_BURST_TICKS) {
            fading = true;
            level().broadcastEntityEvent(this, FADE_EVENT);
        }
        if (now >= fadeAtTick) discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == FADE_EVENT) {
            fading = true;
        } else {
            super.handleEntityEvent(id);
        }
    }

    public boolean isFading() { return fading; }
}
