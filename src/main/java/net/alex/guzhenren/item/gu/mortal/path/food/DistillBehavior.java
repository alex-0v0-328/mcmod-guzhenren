package net.alex.guzhenren.item.gu.mortal.path.food;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.StateEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 酒虫[Liquor Worm]行为：将普通真元分阶段精炼为高一阶的精炼真元储备。
 * <ul>
 *   <li>共注册一至四转四档，仅限主空窍处于本蛊同转时催动；生效时调用 {@link ApertureEssenceService#beginDistilling} 并
 *       通过 {@link StateEffects#grantDistilling} 挂载持续一整天的流派效果作为精炼状态真值（抽干、改道与 1:2 折算消耗三
 *       阶段逻辑位于服务与效果类中）。</li>
 *   <li>⚠ 精炼运行期间普通真元池按设计保持为空，任何以真元为前置门槛的逻辑必须查询可花费真元总量而非普通真元池余额，否
 *       则在精炼期间会全程误判为真元不足。</li>
 * </ul>
 */

public record DistillBehavior() implements GuBehavior {


    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        if (ApertureService.getRank(player) != gu.getRank()) {
            return new Refusal(Refusal.RANK_MISMATCH);
        }
        return ApertureEssenceService.canDistill(player) ? null : new Refusal(Refusal.EFFECT_ACTIVE);
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        ApertureEssenceService.beginDistilling(player);
        StateEffects.grantDistilling(player, gu, gu.getTier(), Ticks.DAY);
    }
}
