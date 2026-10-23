package net.alex.guzhenren.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.alex.guzhenren.gameplay.flight.FlightService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Scales the local player's horizontal flight during a burst [疾飞]; client only.
 *
 * <p>Two readings of the flying speed move a flying player: the horizontal one, {@code Player#getFlyingSpeed}
 * (doubled while sprinting), which {@link #guzhenren$scaleBurstFlight} scales, and the climb and descent one inside
 * {@code LocalPlayer#aiStep}, which {@link LocalPlayerMixin} scales, both by
 * {@link FlightService#BURST_MULTIPLIER} while the synced burst mark is present. Only the local player runs this
 * physics, so the multiple never reaches other players, the integrated server's players or the save. It wraps every
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
    private float guzhenren$scaleBurstFlight(float speed) {
        return (Object) this instanceof LocalPlayer player && player.getAbilities().flying
                && FlightService.isBursting(player) ? speed * FlightService.BURST_MULTIPLIER : speed;
    }
}
