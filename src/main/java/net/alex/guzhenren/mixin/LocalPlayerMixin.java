package net.alex.guzhenren.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Scales the local player's climb and descent in second-stage flight [二段飞行]; client only.
 *
 * <p>{@link #guzhenren$scaleSecondStageClimb} scales the one flying-speed reading inside {@code LocalPlayer#aiStep},
 * the vertical step taken while flying, by {@link SecondStageService#CLIMB_MULTIPLIER} while the synced mark is
 * present; {@link PlayerMixin} sets the horizontal flight.
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
    private float guzhenren$scaleSecondStageClimb(float speed) {
        return SecondStageService.isActive((LocalPlayer) (Object) this)
                ? speed * SecondStageService.CLIMB_MULTIPLIER : speed;
    }
}
