package net.alex.guzhenren.item.gu.mortal.path.heaven;

import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 寿蛊[Lifespan Gu]行为：供一次性消耗蛊（{@link net.alex.guzhenren.item.gu.OneShotGuItem}）在注册的
 * {@code [minYears, maxYears]} 闭区间内随机抽取年数并通过 {@link BodyService#addLifespan} 增加玩家寿元；四档寿蛊各自约
 * 定区间，物品名称对应基础寿元量，随机浮动仅决定零头。
 */

public record LifespanBehavior(int minYears, int maxYears) implements GuBehavior {

    private static final String MSG_GAINED = "guzhenren.item.gu.lifespan_gained";

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        int years = minYears + player.getRandom().nextInt(maxYears - minYears + 1);
        BodyService.addLifespan(player, years);
        ActionBar.inform(player, MSG_GAINED, years);
    }
}
