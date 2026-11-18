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
 * 仅客户端 Mixin，在打开扇形轮盘（{@link RadialMenu}）、长按滑条（{@link HoldSlider}）或打坐锁视角
 * （{@link NourishInputEvents#holdsTurn}）期间截获并屏蔽镜头转动。
 * <ul>
 *   <li>{@link #guzhenren$holdTurn} 在 {@code turnPlayer} 头部取消原版视角转动（原版随后仍会照常清零累积鼠标位移，移动
 *       键不受影响），并将 {@code accumulatedDX} 与 {@code accumulatedDY} 转交轮盘或滑条（滑条仅读取横向位移）用于界面
 *       操控；打坐期间则直接丢弃位移，连同 Epic Fight 包装在 {@code LocalPlayer#turn} 上的第三人称镜头转动一并拦
 *       截。</li>
 *   <li>⚠ 不使用事件或 API：{@code CalculatePlayerTurnEvent} 只能调整灵敏度缩放（无法归零微小转动），也读取不到原始累积
 *       位移且无法阻止教程钩子感知转动，唯有在方法头部直接取消才能整份接管鼠标位移。</li>
 * </ul>
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
