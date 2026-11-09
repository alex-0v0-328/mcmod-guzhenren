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
 * <p>⚠ Not an event or an API: NeoForge 21.1.265 has no hook on the climb and descent {@code aiStep} adds from the
 * flying speed; reading the speed as zero at that one call leaves every other reader of the flying speed untouched.
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
