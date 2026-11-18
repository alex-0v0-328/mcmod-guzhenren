package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身体质或死灵状态变更事件：{@link BodyService} 增删体质、复活、进入临时僵尸化或转为僵尸后同步发布；上层
 * {@code gameplay.path.strength} 通过订阅此事件刷新攻击力修饰符，避免 {@code gameplay.body} 反向依赖。
 */

public class PhysiqueChangedEvent extends Event {

    private final ServerPlayer player;

    public PhysiqueChangedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
