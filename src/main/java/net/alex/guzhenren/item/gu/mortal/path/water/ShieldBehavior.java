package net.alex.guzhenren.item.gu.mortal.path.water;

import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 护盾授予行为：通过 {@link ShieldService#raise} 为玩家升起一层指定类型、护盾值与持续时长的护盾（{@code TimedEffects}
 * 通过检索本行为反查赋予某类护盾的蛊虫）。
 * <ul>
 *   <li>由天蓬蛊[Heavenly Mugwort Gu]携带，并搭配 {@link net.alex.guzhenren.item.gu.mortal.behavior.RecastBehavior} 允
 *       许在冷却期间再次催动。</li>
 *   <li>每次催动均将护盾值补满并重置持续时间（覆盖刷新而非叠加护盾值）；功倍蛊[Amplify Effect Gu]仅倍增护盾持续时长，不
 *       倍增护盾值。</li>
 * </ul>
 */

public record ShieldBehavior(ShieldType type, float points, int durationTicks) implements GuBehavior {

    @Override
    public boolean canBeAmplified() { return true; }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        Amplification amplification = AmplifyEffectGuItem.bill(player, gu);
        ShieldService.raise(player, type, points, durationTicks * amplification.factor());
        TimedEffects.markShieldAmplified(player, type, amplification);
    }
}
