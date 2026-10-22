package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Adds the Dragonpill Cricket Gu's lift to a jump as it starts: {@link DragonpillCricketGuEffect#JUMP_BONUS} per
 * level on top of the upward speed vanilla just set, the same amount the mainline adds to the jump-strength
 * attribute.
 *
 * @author Alex
 * @version 1.0.0
 * @see DragonpillCricketGuEffect
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class DragonpillCricketJump {

    private DragonpillCricketJump() {}

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance instance = ModEffects.get(entity, ModEffects.DRAGONPILL_CRICKET_GU);
        if (instance == null) return;

        Vec3 motion = entity.getDeltaMovement();
        double lift = DragonpillCricketGuEffect.JUMP_BONUS * (instance.getAmplifier() + 1);
        entity.setDeltaMovement(motion.x, motion.y + lift, motion.z);
    }
}
