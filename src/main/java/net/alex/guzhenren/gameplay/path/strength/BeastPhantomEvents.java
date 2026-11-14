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
 * 开启兽力虚影流攻击轮次的拳击命中。
 * <ul>
 *   <li>门槛：玩家为直接来源、空手、伤害为正、目标存活、属肉体自身力量；箭、月刃、气波、范围冲击、蓄力一拳不算，
 *       会被受击无敌帧吞掉的不开轮。</li>
 *   <li>同门槛每击先扣肉体过载（{@link BodyOverload#recoil}），不论轮内外。</li>
 *   <li>开轮那击的属性下一 tick 才含解放，故 {@link StrengthRound#open} 的增量直接加到本击（护甲前）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthRound
 * @see BodyOverload
 * @since 1.0.0
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
                || source.is(ModDamageTypes.CHARGED_PUNCH)) return false;
        if (amount <= 0.0F || !target.isAlive()) return false;
        return target.invulnerableTime <= HURT_FRAME_GATE || source.is(DamageTypeTags.BYPASSES_COOLDOWN);
    }
}
