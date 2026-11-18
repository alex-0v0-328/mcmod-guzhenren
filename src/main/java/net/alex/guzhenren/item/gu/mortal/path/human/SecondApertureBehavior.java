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
 * 第二空窍蛊[Second Aperture Gu]行为：开启或升级第二空窍的唯一途径（{@link ApertureService#openSecond}）。
 * <ul>
 *   <li>挂载于一次性消耗蛊（{@link net.alex.guzhenren.item.gu.OneShotGuItem}），炼化与催动均瞬发且不耗真元，无持久状
 *       态，共注册一至五转五档；{@link #checkEffect} 仅拒绝玩家已有转数不低于本蛊的第二空窍：高转第二空窍蛊可覆盖升级低
 *       转第二空窍（重置为初阶、甲等 8/10 资质、温养进度归零，但保留已绑定流派）。</li>
 *   <li>其余情况一律放行：无论是否有主空窍、主空窍转数高低（低于主空窍转数的第二空窍蛊同样可用）或是否处于僵尸化状态均
 *       可使用；僵尸化期间开辟的第二空窍在后续僵尸状态下统一按活窍（{@code LIVING}）运作。</li>
 * </ul>
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
