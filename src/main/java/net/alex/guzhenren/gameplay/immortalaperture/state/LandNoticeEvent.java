package net.alex.guzhenren.gameplay.immortalaperture.state;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 福地提示事件：功能在面板操作之外得出结果（如规划田轮到开垦或被取消）时同步发布，由面板层决定怎么告诉主人：开着地灵
 * 面板就随下一份快照显示，没开时只有 {@link #toChat} 为真才发聊天消息。
 * <ul>
 *   <li>{@link #text} 拼面板提示的翻译键（{@code guzhenren.blissful_land.panel.} 下），功能层与面板层共用。</li>
 * </ul>
 */

public class LandNoticeEvent extends Event {

    private static final String KEY = "guzhenren.blissful_land.panel.";
    private final ServerPlayer player;
    private final Component notice;
    private final boolean toChat;

    public LandNoticeEvent(@NotNull ServerPlayer player, @NotNull Component notice, boolean toChat) {
        this.player = player;
        this.notice = notice;
        this.toChat = toChat;
    }

    public static @NotNull Component text(@NotNull String key, Object... args) {
        return Component.translatable(KEY + key, args);
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull Component getNotice() { return notice; }

    public boolean toChat() { return toChat; }
}
