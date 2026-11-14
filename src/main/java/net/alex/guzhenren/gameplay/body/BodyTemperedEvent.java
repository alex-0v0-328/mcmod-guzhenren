package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * {@link BodyService} 淬炼筋或骨后发布的事件；同步分发，写入返回时订阅者已执行。
 *
 * <ul>
 *   <li>承受力、最大生命值、击退抗性是 {@code gameplay.attribute} 的属性修饰符，肉身直接调用会成依赖环。</li>
 *   <li>皮改发 {@link SkinWornEvent}、{@link SkinShedEvent}：减免逐次受击读取，承受力加成借这两个事件刷新攻击力。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @see PhysiqueChangedEvent
 * @since 1.0.0
 */

public class BodyTemperedEvent extends Event {

    private final ServerPlayer player;

    public BodyTemperedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
