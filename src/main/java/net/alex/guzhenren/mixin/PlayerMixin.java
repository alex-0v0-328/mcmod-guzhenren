package net.alex.guzhenren.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sets the local player's horizontal flight in second-stage flight [二段飞行]; client only.
 *
 * <p>Two readings of the flying speed move a flying player: the horizontal one, {@code Player#getFlyingSpeed}
 * (doubled while sprinting), which {@link #guzhenren$setSecondStageFlight} replaces with
 * {@link SecondStageService#SPEED_MULTIPLIER} times the plain flying speed, so the sprint's doubling never stacks
 * on it, and the climb and descent one inside {@code LocalPlayer#aiStep}, which {@link LocalPlayerMixin} scales by
 * {@link SecondStageService#CLIMB_MULTIPLIER}, both while the synced mark is present and the player flies
 * unmounted, vanilla's own flying branch. Only the local player runs this physics, so the multiple never reaches
 * other players, the integrated server's players or the save. It wraps every
 * return of {@code Player#getFlyingSpeed} instead of adding an override to {@code LocalPlayer}, which vanilla does
 * not declare there: another mod adding the same override would clash with this required config.
 *
 * @author Alex
 * @version 1.0.0
 * @see LocalPlayerMixin
 * @since 1.0.0
 */

@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyReturnValue(method = "getFlyingSpeed", at = @At("RETURN"))
    private float guzhenren$setSecondStageFlight(float speed) {
        return (Object) this instanceof LocalPlayer player && player.getAbilities().flying && !player.isPassenger()
                && SecondStageService.isActive(player)
                ? player.getAbilities().getFlyingSpeed() * SecondStageService.SPEED_MULTIPLIER : speed;
    }
}
