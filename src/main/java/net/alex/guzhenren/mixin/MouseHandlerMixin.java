package net.alex.guzhenren.mixin;

import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.client.slider.HoldSlider;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Holds the camera still while the radial menu [扇形选择器] or the hold slider [长按滑条] is open; client only.
 *
 * <p>{@link #guzhenren$steerHeldPicker} hands the mouse movement {@code turnPlayer} would have turned the player by
 * to {@link RadialMenu#steer} (the slider's {@link HoldSlider#steer} takes only the sideways part) and skips the
 * turn; {@code handleAccumulatedMovement} clears the movement afterwards as usual. Movement keys are untouched, so
 * the player keeps walking.
 *
 * @author Alex
 * @version 1.0.0
 * @see RadialMenu
 * @see HoldSlider
 * @since 1.0.0
 */

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void guzhenren$steerHeldPicker(double movementTime, CallbackInfo info) {
        if (RadialMenu.isOpen()) {
            RadialMenu.steer(accumulatedDX, accumulatedDY);
        } else if (HoldSlider.isOpen()) {
            HoldSlider.steer(accumulatedDX);
        } else {
            return;
        }
        info.cancel();
    }
}
