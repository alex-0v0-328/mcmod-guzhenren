package net.alex.guzhenren.entity.gu;

import java.util.function.Supplier;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * 野生希望蛊[Hope Gu]实体：继承 {@link FlyingGuEntity}，是世界自然生成的开窍[Awakening]来源。
 * <ul>
 *   <li>{@link #isSeeking} 仅向尚未开窍的玩家靠拢；但只要任意玩家（无论是否开窍）进入 {@link #DETECT_RANGE} 即启动 1 分
 *       钟消散倒计时，玩家走远不会暂停或重置；倒计时有意不写入存档，区块重载后重新起算。</li>
 *   <li>⚠ 消散倒计时认任意玩家而不绑定特定玩家，且一旦启动不可取消或重启。</li>
 *   <li>实体无几何模型，本体由客户端 Photon 萤光粒子表现（{@code HopeGuEffectEvents}）；消散前
 *       {@link #FADE_BURST_TICKS} tick 广播 {@link #FADE_EVENT}，客户端根据 {@link #isFading} 切换小光点并向四周散
 *       开。</li>
 *   <li>动态光源由 {@code assets/guzhenren/dynamiclights/entity/} 下的 LambDynamicLights 配置声明，未安装该模组时仅保持
 *       粒子自身全亮。</li>
 * </ul>
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
