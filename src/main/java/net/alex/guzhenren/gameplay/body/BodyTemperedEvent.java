package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 筋骨淬炼完成事件：{@link BodyService} 更新筋（{@link Tendon}）或骨（{@link Bone}）档位后在游戏总线上同步发布。
 * <ul>
 *   <li>最大生命值、击退抗性与肉身承受力分别位于上层 {@code gameplay.attribute} 与 {@code gameplay.path.strength}，通过
 *       订阅此事件刷新，避免 {@code gameplay.body} 反向依赖。</li>
 *   <li>皮肤变更单独发布 {@link SkinWornEvent} 与 {@link SkinShedEvent}（固定减伤在受击时实时读取，承受力加成借这两个事
 *       件刷新攻击力）。</li>
 * </ul>
 */

public class BodyTemperedEvent extends Event {

    private final ServerPlayer player;

    public BodyTemperedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
