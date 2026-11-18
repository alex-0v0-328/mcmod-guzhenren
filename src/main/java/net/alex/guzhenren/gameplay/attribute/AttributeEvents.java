package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.body.BodyTemperedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 心跳之外重算最大生命值与击退抗性的事件订阅入口。
 * <ul>
 *   <li>响应 {@link ApertureChangedEvent} 刷新受转数影响的最大生命值（{@link MaxHealthService}），响应
 *       {@link BodyTemperedEvent} 同时刷新受铁骨影响的最大生命值与击退抗性（{@link KnockbackResistanceService}）。</li>
 *   <li>攻击力修饰符由力道点数派生，单独归口在 {@code gameplay.path.strength.AttackDamageEvents}。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class AttributeEvents {

    private AttributeEvents() {}

    @SubscribeEvent
    public static void onApertureChanged(ApertureChangedEvent event) { MaxHealthService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onBodyTempered(BodyTemperedEvent event) {
        MaxHealthService.refresh(event.getPlayer());
        KnockbackResistanceService.refresh(event.getPlayer());
    }
}
