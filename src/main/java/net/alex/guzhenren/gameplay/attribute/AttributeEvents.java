package net.alex.guzhenren.gameplay.attribute;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.body.BodyTemperedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 心跳外重算最大生命与击退抗性的事件入口。
 * <ul>
 *   <li>{@link ApertureChangedEvent}：转数定最大生命；{@link BodyTemperedEvent}：骨影响两者。</li>
 *   <li>攻击修饰由力道派生，另归 {@code gameplay.path.strength.AttackDamageEvents}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see MaxHealthService
 * @see KnockbackResistanceService
 * @since 1.0.0
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
