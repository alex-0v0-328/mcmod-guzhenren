package net.alex.guzhenren.effect;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Milk [牛奶] cures none of this mod's own effects but Bleeding [流血]. Each other one is the icon, timer, modifier or
 * pulse of a state held elsewhere -- a path effect [流派效果], a qi pool, the body -- that the heartbeat projects
 * again, so curing one only paused what it carries until the next beat; Bleeding has no state behind it.
 *
 * <p>Event subscriber on {@link MobEffectEvent.Remove}: a removal whose cure is {@link EffectCures#MILK} and whose
 * effect sits in this mod's namespace is canceled. Every other removal passes: another cure (a totem), a removal
 * with no cure ({@code /effect clear}, death) and every vanilla effect, the aftermath's weakness among them.
 *
 * @author Alex
 * @version 1.0.0
 * @see AftermathEffect
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class MilkCureEvents {

    private MilkCureEvents() {}

    @SubscribeEvent
    public static void onRemove(MobEffectEvent.Remove event) {
        Holder<MobEffect> effect = event.getEffect();
        if (event.getCure() == EffectCures.MILK && isOwn(effect) && !effect.is(ModEffects.BLEEDING.getKey())) {
            event.setCanceled(true);
        }
    }

    private static boolean isOwn(@NotNull Holder<MobEffect> effect) {
        return effect.unwrapKey().map(key -> key.location().getNamespace().equals(Guzhenren.MOD_ID)).orElse(false);
    }
}
