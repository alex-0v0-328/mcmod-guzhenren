package net.alex.guzhenren.item.gu.mortal.path.human;

import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 第二空窍蛊[Second Aperture Gu]行为：第二空窍唯一开启途径，人道[Human Path]。
 * <ul>
 *   <li>一次性蛊，炼化与催动免费即时，无状态，可堆叠，注册五档；门槛只拒绝不低于本蛊档位的第二空窍：升级须严格更高并整
 *       体覆盖（回初阶、甲等 8/10、进度归零、保留已绑定道）。</li>
 *   <li>其余放行：无主空窍、无空窍、任意转数差，低于持有者的蛊无需比主空窍；僵尸化时可用，所开空窍此后僵尸化皆读作
 *       LIVING。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @see ApertureService#openSecond
 * @since 1.0.0
 */

public record SecondApertureBehavior() implements GuBehavior {


    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        ApertureData data = ApertureService.get(player);
        int index = data.getSecondIndex();
        return index >= 0 && data.get(index).rank().ordinal() >= gu.getRank().ordinal()
                ? new Refusal(Refusal.RANK_MISMATCH) : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        ApertureService.openSecond(player, gu.getRank());
    }
}
