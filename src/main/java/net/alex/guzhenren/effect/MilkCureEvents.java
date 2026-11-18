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
 * 牛奶解除限制监听器：在本模组所有 {@link MobEffect} 中，仅流血效果（{@link ModEffects#BLEEDING}）背后无持久状态而允许
 * 被牛奶解除，其余效果均为流派效果或肉身状态的图标投影。监听 {@link MobEffectEvent.Remove} 时，若解药为
 * {@link EffectCures#MILK} 且目标属于本模组命名空间而非流血，则取消本次移除；其余移除方式（如不死图腾、
 * {@code /effect clear}、死亡或原版效果）一律放行。
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
        return effect.is(key -> key.location().getNamespace().equals(Guzhenren.MOD_ID));
    }
}
