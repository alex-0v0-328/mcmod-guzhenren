package net.alex.guzhenren.item.gu.mortal.path.heaven;

import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 寿蛊[Lifespan Gu]行为：经 {@link BodyService#addLifespan} 加寿元，取注册上下限间随机值；四档各有区间，名字即承诺，
 * 随机只定零头。
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.gu.OneShotGuItem
 * @since 1.0.0
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
