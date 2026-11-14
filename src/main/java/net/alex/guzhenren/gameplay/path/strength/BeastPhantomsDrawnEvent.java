package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 兽力虚影兽种设定事件，游戏事件总线。开轮及抽中兽种离轮那 tick 发出，同步分发，返回前虚影已就位；轮次不知谁在听：
 * 虚影实体在上层 {@code net.alex.guzhenren.entity.beast}，上调成环。
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthRound
 * @since 1.0.0
 */

public class BeastPhantomsDrawnEvent extends Event {

    private final ServerPlayer player;
    private final List<BeastStrengthFamily> families;

    public BeastPhantomsDrawnEvent(@NotNull ServerPlayer player, @NotNull List<BeastStrengthFamily> families) {
        this.player = player;
        this.families = List.copyOf(families);
    }

    public @NotNull ServerPlayer getPlayer() { return player; }

    public @NotNull List<BeastStrengthFamily> getFamilies() { return families; }
}
