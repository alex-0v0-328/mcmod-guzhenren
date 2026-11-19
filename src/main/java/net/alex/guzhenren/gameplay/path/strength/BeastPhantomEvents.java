package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 空手近战命中结算兽力虚影流[Beast Strength Phantom]连招轮收尾与过载反噬的事件入口。
 * <ul>
 *   <li>触发门槛：伤害直接来源为服务端玩家、主手为空（{@link AttackDamageService#isFistHand}）、伤害大于 {@code 0}、目
 *       标存活，且不属于气波、范围冲击[Area Impact]、蓄力一拳[Charged Punch]或终点破坏打击[Crash Strike]等派生伤害，同
 *       时不会被目标当前的受击无敌帧吞掉。</li>
 *   <li>收尾招（{@link ComboRound#isFinishing}）的命中先调 {@link ComboRound#strike} 抽兽种，把抽中那一击尚未进攻击属
 *       性的解放增量直接加入本次护甲前伤害，再调 {@link ComboRound#markLanded} 记下溢出量。</li>
 *   <li>过载反噬在伤害实际落到目标身上之后（{@link LivingDamageEvent.Post}，被盾牌完全挡下或被无敌帧吞掉的拳不
 *       算）结算：每次出手（采矿模式原版拳与连招前几段同样）经 {@link ComboRound#settleOverload} 去重后结算一次，读
 *       数含本击的解放；这一招已发出远程拳时与弹射物爆开共用同一份记账。</li>
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

        if (ComboRound.isFinishing(player)) {
            float gain = ComboRound.strike(player, player.getRandom().nextFloat());
            if (gain > 0.0F) event.setAmount(event.getAmount() + gain);
            ComboRound.markLanded(player, event.getAmount(), target.getHealth());
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getDirectEntity() instanceof ServerPlayer player) || event.getEntity() == player) return;
        if (isDerived(source) || event.getNewDamage() <= 0.0F || !AttackDamageService.isFistHand(player)) return;

        ComboRound.settleOverload(player);
    }

    private static boolean isFistHit(DamageSource source, float amount, LivingEntity target) {
        if (isDerived(source) || amount <= 0.0F || !target.isAlive()) return false;
        return target.invulnerableTime <= HURT_FRAME_GATE || source.is(DamageTypeTags.BYPASSES_COOLDOWN);
    }

    private static boolean isDerived(DamageSource source) {
        return source.is(ModDamageTypes.AREA_IMPACT) || source.is(ModDamageTypes.QI_WAVE)
                || source.is(ModDamageTypes.CHARGED_PUNCH) || source.is(ModDamageTypes.CRASH_STRIKE);
    }
}
