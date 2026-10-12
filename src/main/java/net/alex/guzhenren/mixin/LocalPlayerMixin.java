package net.alex.guzhenren.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.alex.guzhenren.gameplay.flight.FlightService;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Scales the local player's climb and descent during a burst [疾飞]; client only.
 *
 * <p>{@link #guzhenren$scaleBurstClimb} scales the one flying-speed reading inside {@code LocalPlayer#aiStep}, the
 * vertical step taken while flying, by {@link FlightService#BURST_MULTIPLIER} while the synced burst mark is
 * present; {@link PlayerMixin} scales the horizontal flight.
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
    private float guzhenren$scaleBurstClimb(float speed) {
        return FlightService.isBursting((LocalPlayer) (Object) this) ? speed * FlightService.BURST_MULTIPLIER : speed;
    }
}
