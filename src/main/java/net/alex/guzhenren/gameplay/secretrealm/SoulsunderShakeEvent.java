package net.alex.guzhenren.gameplay.secretrealm;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 荡魂山[Soulsunder Mountain]震荡玩家前同步发布的可取消事件：取消即本次不扣魂魄，供上层（福地免震荡白名单）豁免，秘境
 * 包不反向依赖上层。
 */

public class SoulsunderShakeEvent extends Event implements ICancellableEvent {

    private final ServerPlayer player;

    public SoulsunderShakeEvent(@NotNull ServerPlayer player) { this.player = player; }

    public @NotNull ServerPlayer getPlayer() { return player; }
}
