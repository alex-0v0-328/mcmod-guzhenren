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
 * 扇形选择器或长按滑条打开时锁住镜头；仅客户端。
 * <ul>
 *   <li>{@link #guzhenren$steerHeldPicker} 把 {@code turnPlayer} 本要转玩家的位移交 {@link RadialMenu#steer}（滑条
 *       {@link HoldSlider#steer} 只取横向）并跳过转动，{@code handleAccumulatedMovement} 照常清零；移动键不变。</li>
 *   <li>⚠ 不用事件或 API：{@code CalculatePlayerTurnEvent} 只能设灵敏度（最小转动非零），看不到累积位移，教程钩子仍见转
 *       动；只有方法头取消才能整份交出位移。</li>
 * </ul>
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
