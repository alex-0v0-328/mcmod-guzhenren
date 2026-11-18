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
 * 肉身状态对原版生存与受伤事件的订阅入口。
 * <ul>
 *   <li>{@link #onBreathe}：死灵体质（僵尸与临时僵尸化）水下免耗氧气。</li>
 *   <li>{@link #onIncomingDamage}：先经 {@link SkinService#reduceDamage} 扣减所穿皮肤的固定减伤，若减至 {@code 0} 则直
 *       接取消事件（不产生击退、受伤音效与无敌帧）；剩余伤害再交由 {@link ShieldService#guard} 按原版盾牌格挡槽结算护
 *       盾，全额挡下时同样取消事件。</li>
 *   <li>⚠ {@link #onIncomingDamage} 使用 {@link EventPriority#LOW}，确保在直接免疫伤害的高优先级处理器之后运行，避免被
 *       免疫的攻击白扣护盾耐久；肉身变更触发的攻击力刷新另归 {@code gameplay.path.strength.AttackDamageEvents}。</li>
 * </ul>
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
