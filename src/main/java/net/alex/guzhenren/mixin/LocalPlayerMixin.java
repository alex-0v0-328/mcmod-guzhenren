package net.alex.guzhenren.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Holds the local player's jump and sneak out of second-stage flight [二段飞行]; client only.
 *
 * <p>{@code LocalPlayer#aiStep} adds a climb or a descent of three times the flying speed to the velocity before the
 * travel; {@link #guzhenren$holdSecondStageAltitude} reads that flying speed as zero while the synced mark is
 * present, so the cruise ({@link PlayerMixin}) follows the look alone and inherits no drift from the keys.
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerMixin
 * @since 1.0.0
 */

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
    private float guzhenren$holdSecondStageAltitude(float speed) {
        return SecondStageService.isActive((LocalPlayer) (Object) this) ? 0.0F : speed;
    }
}
