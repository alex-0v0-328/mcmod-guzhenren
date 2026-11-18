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
 * 人如故[Man As Before Gu]物品：继承 {@link AsBeforeGuItem}，将持有者自身状态回溯至游戏内一天前。
 * <ul>
 *   <li>仅回溯 {@link PlayerSnapshot} 记录的身体与神魂维度：生命值、饥饿度、药水与流派效果、魂魄、念头及临时僵尸化形
 *       态；历史快照存于玩家自身的 {@link PlayerRewindService} 环形缓冲区中，且仅在随身携带已炼化人如故期间采样记
 *       录。</li>
 *   <li>若缓冲区为空或尚未攒够一天前的快照，在扣费前以 {@link #FAILED_NO_RECORD} 拒绝催动。</li>
 * </ul>
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
