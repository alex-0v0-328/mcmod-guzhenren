package net.alex.guzhenren.mixin;

import net.alex.guzhenren.client.radial.RadialMenu;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Holds the camera still while the radial menu [扇形选择器] is open; client only.
 *
 * <p>{@link #guzhenren$steerRadialMenu} hands the mouse movement {@code turnPlayer} would have turned the player by
 * to {@link RadialMenu#steer} and skips the turn; {@code handleAccumulatedMovement} clears the movement afterwards
 * as usual. Movement keys are untouched, so the player keeps walking.
 *
 * @author Alex
 * @version 1.0.0
 * @see RadialMenu
 * @since 1.0.0
 */

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void guzhenren$steerRadialMenu(double movementTime, CallbackInfo info) {
        if (!RadialMenu.isOpen()) return;

        RadialMenu.steer(accumulatedDX, accumulatedDY);
        info.cancel();
    }
}
