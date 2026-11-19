package net.alex.guzhenren.gameplay.path.strength;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 兽力虚影抽取完成事件：由 {@link ComboRound} 在连招轮收尾招抽取兽种时同步发布，供上层
 * {@code net.alex.guzhenren.entity.beast} 生成或更新背后的兽力虚影实体，避免 {@code gameplay} 包反向依赖 {@code entity}
 * 包。
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
