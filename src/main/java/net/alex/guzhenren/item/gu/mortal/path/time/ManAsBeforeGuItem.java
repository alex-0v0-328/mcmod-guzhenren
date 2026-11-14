package net.alex.guzhenren.item.gu.mortal.path.time;

import net.alex.guzhenren.gameplay.rewind.PlayerRewindData;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindService;
import net.alex.guzhenren.gameplay.rewind.PlayerSnapshot;
import net.alex.guzhenren.item.gu.GuProperties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 人如故[Man As Before Gu]：携带者身体回到一天前。
 * <ul>
 *   <li>只回退 {@code PlayerSnapshot} 所列：生命、饥饿值、效果、魂魄、念、临时僵尸化形态；记忆是携带者身上
 *       {@code PlayerRewindService} 的环形缓冲，只在随身带已炼化人如故时记录。</li>
 *   <li>无记录或无一天前的记录时以 {@code FAILED_NO_RECORD} 拒绝，不扣费。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AsBeforeGuItem
 * @since 1.0.0
 */

public class ManAsBeforeGuItem extends AsBeforeGuItem {

    public ManAsBeforeGuItem(GuProperties guProperties) {
        super(guProperties);
    }

    @Override
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer server)) return null;

        PlayerRewindData memory = PlayerRewindService.get(server);
        if (memory.isEmpty()) return new Refusal(FAILED_NO_RECORD);
        return memory.pick(server.level().getGameTime()) == null ? new Refusal(FAILED_NO_RECORD) : null;
    }

    @Override
    protected void applyEffect(ServerPlayer player, ItemStack stack) {
        PlayerSnapshot snapshot = PlayerRewindService.pick(player, player.level().getGameTime());
        if (snapshot == null) return;

        PlayerRewindService.restore(player, snapshot);
    }
}
