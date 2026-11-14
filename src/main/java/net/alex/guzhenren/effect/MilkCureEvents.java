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
 * 牛奶解除规则：本模组效果中牛奶只解流血。其余都是别处状态（流派效果、身体）的图标、计时、修饰符或脉冲，心跳重新投影，
 * 解掉只让其所载停到下一拍；流血背后无状态。监听 {@link MobEffectEvent.Remove}：解药为 {@link EffectCures#MILK} 且效果
 * 属本模组命名空间的移除被取消；其余放行，如别的解药（图腾）、无解药的移除（{@code /effect clear}、死亡）、所有原版效
 * 果。
 *
 * @author Alex
 * @version 1.0.0
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
        return effect.is(key -> key.location().getNamespace().equals(Guzhenren.MOD_ID));
    }
}
