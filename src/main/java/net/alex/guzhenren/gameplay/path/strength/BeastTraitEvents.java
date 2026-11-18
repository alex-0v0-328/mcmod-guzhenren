package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.ShieldRaisingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 兽力[Beast Strength]被动特性向底层 {@code gameplay.body} 包注入加成的事件订阅入口：监听 {@link ShieldRaisingEvent} 将
 * 新升起的护盾点数乘以 {@link PathStrengthData#getShieldMultiplier}（如石龟之力 ×1.5，持续时长不变），避免
 * {@code gameplay.body} 反向依赖力道包。
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BeastTraitEvents {

    private BeastTraitEvents() {}

    @SubscribeEvent
    public static void onShieldRaising(ShieldRaisingEvent event) {
        event.setPoints(event.getPoints() * PathStrengthService.get(event.getPlayer()).getShieldMultiplier());
    }
}
