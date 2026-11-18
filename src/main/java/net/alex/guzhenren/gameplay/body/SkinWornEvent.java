package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身换皮事件：{@link BodyService#wearSkin} 穿上限时玉皮或永久铜皮、铁皮后同步发布（脱皮改发 {@link SkinShedEvent}），
 * 供上层 {@code network} 包向追踪客户端广播穿戴闪光并刷新承受力与攻击力。
 */

public class SkinWornEvent extends Event {

    private final ServerPlayer player;
    private final Skin skin;

    public SkinWornEvent(@NotNull ServerPlayer player, @NotNull Skin skin) {
        this.player = player;
        this.skin = skin;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull Skin getSkin() { return skin; }
}
