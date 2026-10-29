package net.alex.guzhenren.mixin;

import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands a player's travel to the second-stage cruise [二段飞行] while the stage is on; both ends.
 *
 * <p>{@link #guzhenren$cruise} cancels {@code Player#travel} whenever {@link SecondStageService#travel} moved the
 * player: an unmounted flier with the mark, on the end that simulates it (the local player's client, and the
 * server's own step). Vanilla's flying branch would scale the flying speed by the input and drag it at 0.91; the
 * cruise follows the look instead, so it replaces the whole travel rather than one speed reading. Remote players on
 * a client never travel, mark or not.
 *
 * @author Alex
 * @version 1.0.0
 * @see LocalPlayerMixin
 * @since 1.0.0
 */

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void guzhenren$cruise(Vec3 travelVector, CallbackInfo ci) {
        if (SecondStageService.travel((Player) (Object) this)) ci.cancel();
    }
}
