package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身蜕皮事件：{@link BodyService#shedSkin} 脱下皮肤（含限时玉皮到期）后同步发布，供上层移除永久皮肤附带的流派效果并刷
 * 新承受力与攻击力，避免 {@code gameplay.body} 反向依赖。
 */

public class SkinShedEvent extends Event {

    private final ServerPlayer player;

    public SkinShedEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
