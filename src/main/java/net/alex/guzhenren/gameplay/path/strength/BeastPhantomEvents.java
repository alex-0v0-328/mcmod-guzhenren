package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * 空手出拳命中结算连招轮[Combo Round]收尾暴击[Crit]与过载反噬的事件入口。
 * <ul>
 *   <li>出拳判定（{@link #isPunch}）：伤害类型为 {@code minecraft:player_attack}、直接来源为服务端玩家、主手为空
 *       （{@link AttackDamageService#isFistHand}），且本 tick 已由 {@link ComboRound#markPunch} 标为出拳——原版挥拳由
 *       {@link #onAttackEntity} 标记，Epic Fight 出招由 {@code EpicFightServerEvents} 在更高优先级标记。二段飞行冲撞同
 *       样用 {@code player_attack} 但不经挥拳，荆棘反伤是 {@code thorns} 类型，两者都不算出拳；气波、范围伤害[Area
 *       Damage]、蓄力一拳[Charged Punch]与终点破坏打击[Crash Strike]各有自己的伤害类型，也不算。</li>
 *   <li>收尾招（{@link ComboRound#isFinishing}）的出拳先调 {@link ComboRound#strike} 掷暴击、抽兽种，把抽中那一击尚未进
 *       攻击属性的暴击增量直接加入本次护甲前伤害，再调 {@link ComboRound#markLanded} 记下溢出量；目标举盾时
 *       {@link #onShieldBlock} 在最低优先级读最终格挡量，经 {@link ComboRound#markBlocked} 从溢出里扣掉。命中时目标仍在
 *       受击无敌帧内（且伤害不绕过冷却）则不算。</li>
 *   <li>过载反噬在伤害实际落到目标身上之后（{@link LivingDamageEvent.Post}，被盾牌完全挡下或被无敌帧吞掉的拳不算；只打
 *       掉伤害吸收、没伤到生命的拳照算，见 {@link #hasLanded}）结算：每次出手（采矿模式原版拳与连招前几段同样）经
 *       {@link ComboRound#settleOverload} 去重后结算一次，读数含本击的暴击；这一招已发出远程攻击时与弹射物爆开共用同一
 *       份记账。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BeastPhantomEvents {

    private BeastPhantomEvents() {}

    private static final int HURT_FRAME_GATE = 10;

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ComboRound.markPunch(player);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        if (!(source.getDirectEntity() instanceof ServerPlayer player) || target == player) return;
        if (!isPunch(source, player) || event.getAmount() <= 0.0F || !target.isAlive()) return;
        if (target.invulnerableTime > HURT_FRAME_GATE && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) return;

        if (ComboRound.isFinishing(player)) {
            float gain = ComboRound.strike(player, player.getRandom().nextFloat());
            if (gain > 0.0F) event.setAmount(event.getAmount() + gain);
            ComboRound.markLanded(player, target, event.getAmount(), target.getHealth());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        DamageSource source = event.getDamageSource();
        LivingEntity target = event.getEntity();
        if (!event.getBlocked() || !(source.getDirectEntity() instanceof ServerPlayer player)) return;
        if (!isPunch(source, player)) return;

        ComboRound.markBlocked(player, target, event.getDamageContainer().getNewDamage(), target.getHealth(),
                event.getBlockedDamage());
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getDirectEntity() instanceof ServerPlayer player) || event.getEntity() == player) return;
        if (!isPunch(source, player) || !hasLanded(event)) return;

        ComboRound.settleOverload(player);
    }

    private static boolean hasLanded(LivingDamageEvent.Post event) {
        return event.getNewDamage() > 0.0F || event.getReduction(DamageContainer.Reduction.ABSORPTION) > 0.0F;
    }

    public static boolean isPunch(DamageSource source, ServerPlayer player) {
        return source.is(DamageTypes.PLAYER_ATTACK) && AttackDamageService.isFistHand(player)
                && ComboRound.isPunch(player);
    }
}
