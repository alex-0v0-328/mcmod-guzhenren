package net.alex.guzhenren.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.alex.guzhenren.GuzhenrenClient;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hands this mod's effect icons to Forge 1.20.1's client extension lookup; client only, and port only.
 *
 * <p>{@code IClientMobEffectExtensions#of} reads {@code MobEffect#getEffectRendererInternal}, whose field only
 * {@code MobEffect#initializeClient} fills, from inside the {@code MobEffect} constructor. Several of this mod's
 * effect classes are final and shared with the mainline, so {@link #guzhenren$supplyIcon} answers the lookup from
 * {@link GuzhenrenClient#getMobEffectExtensions} for an effect that supplied none of its own.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuzhenrenClient
 * @since 1.0.0
 */

@Mixin(MobEffect.class)
public abstract class MobEffectMixin {

    @ModifyReturnValue(method = "getEffectRendererInternal", at = @At("RETURN"), remap = false)
    private Object guzhenren$supplyIcon(Object renderer) {
        return renderer != null ? renderer : GuzhenrenClient.getMobEffectExtensions((MobEffect) (Object) this);
    }
}
