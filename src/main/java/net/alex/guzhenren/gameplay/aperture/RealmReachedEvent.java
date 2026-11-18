package net.alex.guzhenren.gameplay.aperture;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 境界提升事件：{@link ApertureNourishService} 升阶或升转并写入空窍后同步发布，携带更新后的 {@link Aperture}；空窍包不
 * 依赖上层 {@code advancement} 包，进度触发器通过订阅此事件判定境界达成。
 */

public class RealmReachedEvent extends Event {

    private final ServerPlayer player;
    private final Aperture aperture;

    public RealmReachedEvent(@NotNull ServerPlayer player, @NotNull Aperture aperture) {
        this.player = player;
        this.aperture = aperture;
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull Aperture getAperture() { return aperture; }
}
