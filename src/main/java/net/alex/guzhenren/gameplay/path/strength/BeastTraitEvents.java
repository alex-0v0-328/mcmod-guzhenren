package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.ShieldRaisingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 经事件作用到下层肉体包的兽力[Beast Strength]特性。目前一项：护盾点数 × {@link PathStrengthData#getShieldMultiplier}
 * （石龟 1.5，天蓬蛊[Heavenly Mugwort Gu] 180 → 270），时长不变；肉体包向上读力量记录会成包循环。
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastStrength#getShieldBonus
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BeastTraitEvents {

    private BeastTraitEvents() {}

    @SubscribeEvent
    public static void onShieldRaising(ShieldRaisingEvent event) {
        event.setPoints(event.getPoints() * PathStrengthService.get(event.getPlayer()).getShieldMultiplier());
    }
}
