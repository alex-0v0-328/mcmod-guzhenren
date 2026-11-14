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
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * 攻击修饰在心跳外的即时刷新入口。
 * <ul>
 *   <li>订阅原因：体质乘承受力（未开窍[Awakening]肉体也乘，它不发空窍[Aperture]事件）；
 *       力道效果被他处摘除见 {@link PathEffectEndedEvent}；转数定基础承受力；筋骨、皮改承受力；换主手即时增减拳头加成；
 *       最大生命与击退抗性另归 {@code gameplay.attribute.AttributeEvents}。</li>
 *   <li>⚠ 只靠心跳，面板攻击行最多滞后 1 秒。</li>
 *   <li>⚠ NeoForge 在效果表变化前发三个效果事件，{@code refreshAttack} 故以 {@link TickTask} 延到本 tick 末刷新。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AttackDamageService
 * @since 1.0.0
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

    private static void refreshAttack(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        player.server.tell(new TickTask(player.server.getTickCount(), () -> AttackDamageService.refresh(player)));
    }
}
