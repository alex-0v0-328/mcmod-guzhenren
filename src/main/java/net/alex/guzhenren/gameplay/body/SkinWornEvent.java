package net.alex.guzhenren.gameplay.body;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * {@link BodyService#wearSkin} 穿上玉皮或永久皮后发布的事件；各观看客户端的闪光由 {@code network} 订阅者发送，
 * 直接调用会成依赖环。蜕皮改发 {@link SkinShedEvent}。
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @see PhysiqueChangedEvent
 * @since 1.0.0
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
