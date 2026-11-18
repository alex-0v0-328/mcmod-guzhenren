package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 空手近战命中开启兽力虚影流[Beast Strength Phantom]攻击轮次与结算肉体过载的事件入口。
 * <ul>
 *   <li>触发门槛：伤害直接来源为服务端玩家、主手为空（{@link AttackDamageService#isFistHand}）、伤害大于 {@code 0}、目
 *       标存活，且不属于气波、范围冲击[Area Impact]、蓄力一拳[Charged Punch]或终点破坏打击[Crash Strike]等派生伤害，同
 *       时不会被目标当前的受击无敌帧吞掉。</li>
 *   <li>满足门槛的每一拳（无论是否处于轮次内）均先调用 {@link BodyOverload#recoil} 结算肉体过载反噬。</li>
 *   <li>开启新轮次的那一击因属性修饰符要到下一 tick 才包含全面解放增量，直接将 {@link StrengthRound#open} 返回的解放攻
 *       击增量加入本次护甲前伤害。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BeastPhantomEvents {

    private BeastPhantomEvents() {}

    private static final int HURT_FRAME_GATE = 10;

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        if (!(source.getDirectEntity() instanceof ServerPlayer player) || target == player) return;
        if (!isFistHit(source, event.getAmount(), target) || !AttackDamageService.isFistHand(player)) return;

        BodyOverload.recoil(player);
        float gain = StrengthRound.open(player, player.getRandom().nextFloat());
        if (gain > 0.0F) event.setAmount(event.getAmount() + gain);
    }

    private static boolean isFistHit(DamageSource source, float amount, LivingEntity target) {
        if (source.is(ModDamageTypes.AREA_IMPACT) || source.is(ModDamageTypes.QI_WAVE)
                || source.is(ModDamageTypes.CHARGED_PUNCH) || source.is(ModDamageTypes.CRASH_STRIKE)) return false;
        if (amount <= 0.0F || !target.isAlive()) return false;
        return target.invulnerableTime <= HURT_FRAME_GATE || source.is(DamageTypeTags.BYPASSES_COOLDOWN);
    }
}
