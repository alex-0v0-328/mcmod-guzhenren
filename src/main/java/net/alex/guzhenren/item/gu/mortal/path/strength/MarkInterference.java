package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 道痕互斥的蛊侧门槛：力道蛊分支与已持分支互斥时拒绝；兽力[Beast Strength]、人力行为先问，限时增益行为给兽力增益
 * 时也问；{@link PathStrengthService} 写入口强制同一规则，这里只出措辞。
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthPathBranch#interferesWith
 * @since 1.0.0
 */

public final class MarkInterference {

    private MarkInterference() {}

    private static final String FAILED_MARK_INTERFERENCE = "guzhenren.item.failed.mark_interference";

    public static @Nullable Refusal check(@NotNull Player player, @NotNull StrengthPathBranch branch) {
        return PathStrengthService.getInterference(player, branch) != null
                ? new Refusal(FAILED_MARK_INTERFERENCE) : null;
    }
}
