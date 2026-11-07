package net.alex.guzhenren.gameplay.path.strength;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.attribute.AttackDamageService;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The fist hit that opens a {@link StrengthRound} [攻击轮次] of the beast strength phantom path [兽力虚影流]: a
 * {@link LivingIncomingDamageEvent} with the player as the DIRECT entity, a fist in the main hand
 * ({@link AttackDamageService#isFistHand}), a positive amount on a living target and a damage type of the body's own
 * strength. An arrow, a Moon Blade, a Qi Wave, the area impact [范围冲击] or the charged punch [蓄力一拳] carries
 * none, and a hit the target's hurt
 * frames would swallow opens nothing. The same gate charges the body overload [肉体过载] on every such hit, inside a
 * round or not ({@link BodyOverload#recoil}), before the round is asked to open.
 *
 * <p>The opening hit has not yet taken the release into its attack attribute -- the player tick refreshes it the
 * next tick -- so the gain {@link StrengthRound#open} returns is added to this hit's amount, before armor. Later hits
 * of the round carry it through the attribute.
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
