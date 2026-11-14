package net.alex.guzhenren.gameplay.body;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 改变肉身行为的原版时机订阅者。
 *
 * <ul>
 *   <li>{@code onBreathe}：不死之躯水下可呼吸。{@code onIncomingDamage}：先扣所穿皮的固定减免，全额挡下即取消，
 *       不留击退、受伤音效、无敌帧；余量在原版盾牌格挡处交护盾（{@link ShieldService#guard}），全额吃下同样取消。</li>
 *   <li>{@code LOW} 优先级，晚于直接拒绝伤害的处理器，被拒伤害不耗护盾；攻击力刷新在
 *       {@code gameplay.path.strength.AttackDamageEvents}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BodyStateEvents {

    private BodyStateEvents() {}

    @SubscribeEvent
    public static void onBreathe(LivingBreatheEvent event) {
        if (event.getEntity() instanceof Player player && BodyService.isUndead(player)) event.setCanBreathe(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        float amount = event.getAmount();
        if (amount <= 0.0F) return;
        float reduced = SkinService.reduceDamage(player, event.getSource(), amount);
        if (reduced <= 0.0F) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(reduced);
        if (player instanceof ServerPlayer server && ShieldService.guard(server, event.getContainer())) {
            event.setCanceled(true);
        }
    }
}
