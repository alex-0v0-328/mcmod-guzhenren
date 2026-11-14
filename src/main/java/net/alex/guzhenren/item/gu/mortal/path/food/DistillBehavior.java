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
 * 酒虫[Liquor Worm]行为：普通真元[Essence]分阶段精炼为精炼储备。
 * <ul>
 *   <li>注册四档，各限本转；生效调 {@link ApertureEssenceService#beginDistilling} 并挂一天流派效果作精炼真值；三阶段
 *       （抽干、改道、1:2 花费）在服务与效果中。</li>
 *   <li>⚠ 运行期普通真元池按设计为空，以真元为门槛者须查可花费数，否则全程拒绝。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see StateEffects#grantDistilling
 * @since 1.0.0
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
