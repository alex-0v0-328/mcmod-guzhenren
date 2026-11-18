package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 力道分支道痕互斥的物品侧门槛检查：当待催动蛊虫所属分支与玩家已持有的力道分支互斥
 * （{@link StrengthPathBranch#interferesWith}）时返回拒绝提示；永久兽力、永久人力及赋予限时兽力增益的行为在催动前均先调
 * 用 {@link #check}（{@link PathStrengthService} 写入端同样强校验该互斥规则，本类负责在扣费前给出统一提示语）。
 */

public final class MarkInterference {

    private MarkInterference() {}

    private static final String FAILED_MARK_INTERFERENCE = "guzhenren.item.failed.mark_interference";

    public static @Nullable Refusal check(@NotNull Player player, @NotNull StrengthPathBranch branch) {
        return PathStrengthService.getInterference(player, branch) != null
                ? new Refusal(FAILED_MARK_INTERFERENCE) : null;
    }
}
