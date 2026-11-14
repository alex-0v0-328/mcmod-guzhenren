package net.alex.guzhenren.gameplay.aperture;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 境界提升事件：{@link ApertureNourishService} 升阶或升转并写入空窍[Aperture]后同步发布，携带写入后的空窍；温
 * 养[Nourish]不认识订阅者，境界进度条件在上层 {@code net.alex.guzhenren.advancement}，向上调用会成依赖环。
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureNourishService
 * @since 1.0.0
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
