package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 永久兽力[Beast Strength]或人力层数变更事件：由 {@link PathStrengthService} 在写入或移除永久力道效果后同步发布，供
 * {@link AttackDamageEvents} 即时刷新攻击力修饰符。
 */

public class StrengthChangedEvent extends Event {

    private final ServerPlayer player;

    public StrengthChangedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
