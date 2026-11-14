package net.alex.guzhenren.mixin;

import net.alex.guzhenren.client.event.NourishInputEvents;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.client.slider.HoldSlider;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 扇形选择器或长按滑条打开、或打坐时锁住镜头；仅客户端。
 * <ul>
 *   <li>{@link #guzhenren$holdTurn} 把 {@code turnPlayer} 本要转玩家的位移交 {@link RadialMenu#steer}（滑条
 *       {@link HoldSlider#steer} 只取横向）并跳过转动，{@code handleAccumulatedMovement} 照常清零；移动键不变。</li>
 *   <li>打坐（{@link NourishInputEvents#holdsTurn}）整份丢弃位移，身体与镜头都不转；Epic Fight 第三人称镜头的转动包在
 *       {@code turnPlayer} 内的 {@code LocalPlayer#turn} 上，方法头取消一并挡住。</li>
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
    private void guzhenren$holdTurn(double movementTime, CallbackInfo info) {
        if (RadialMenu.isOpen()) {
            RadialMenu.steer(accumulatedDX, accumulatedDY);
        } else if (HoldSlider.isOpen()) {
            HoldSlider.steer(accumulatedDX);
        } else if (!NourishInputEvents.holdsTurn()) {
            return;
        }
        info.cancel();
    }
}
