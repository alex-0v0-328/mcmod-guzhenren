package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureChangedEvent;
import net.alex.guzhenren.gameplay.body.BodyTemperedEvent;
import net.alex.guzhenren.gameplay.body.ExtremePhysiqueChangedEvent;
import net.alex.guzhenren.gameplay.body.PhysiqueChangedEvent;
import net.alex.guzhenren.gameplay.body.SkinShedEvent;
import net.alex.guzhenren.gameplay.body.SkinWornEvent;
import net.alex.guzhenren.gameplay.path.PathEffectEndedEvent;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * 玩家攻击力修饰符在每秒心跳之外的即时刷新事件入口。
 * <ul>
 *   <li>当体质、十绝体[Ten Extreme Physiques]、力道点数或流派效果、空窍转数、皮肤、筋骨或主手物品变更时立即调
 *       {@link AttackDamageService#refresh}，避免面板攻击力滞后至多 1 秒（最大生命值与击退抗性另归
 *       {@code gameplay.attribute.AttributeEvents}）。</li>
 *   <li>苦力蛊[Hardship Strength Gu]运行中，玩家受伤或回血后立即刷新，使随伤势档位变化的伤害倍率不滞后于过载反噬的读
 *       数。</li>
 *   <li>⚠ NeoForge 的药水效果增删/到期事件与回血事件在实体状态实际更新前触发，因此 {@code refreshAttack} 通过
 *       {@link TickTask} 延迟到当前服务端 tick 末尾再刷新攻击力。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class AttackDamageEvents {

    private AttackDamageEvents() {}

    @SubscribeEvent
    public static void onPhysiqueChanged(PhysiqueChangedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onExtremePhysiqueChanged(ExtremePhysiqueChangedEvent event) {
        AttackDamageService.refresh(event.getPlayer());
    }

    @SubscribeEvent
    public static void onStrengthChanged(StrengthChangedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onPathEffectEnded(PathEffectEndedEvent event) {
        Object payload = event.getEffect().payload();
        if (payload instanceof BeastStrength || payload instanceof HumanStrength) {
            AttackDamageService.refresh(event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onApertureChanged(ApertureChangedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onSkinWorn(SkinWornEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onSkinShed(SkinShedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onBodyTempered(BodyTemperedEvent event) { AttackDamageService.refresh(event.getPlayer()); }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.MAINHAND) return;
        if (event.getEntity() instanceof ServerPlayer player) AttackDamageService.refresh(player);
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) { refreshAttack(event.getEntity()); }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) { refreshAttack(event.getEntity()); }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) { refreshAttack(event.getEntity()); }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && HardshipStrength.isActive(player)) {
            AttackDamageService.refresh(player);
        }
    }

    @SubscribeEvent
    public static void onHealed(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && HardshipStrength.isActive(player)) {
            refreshAttack(player);
        }
    }

    private static void refreshAttack(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        player.server.tell(new TickTask(player.server.getTickCount(), () -> AttackDamageService.refresh(player)));
    }
}
