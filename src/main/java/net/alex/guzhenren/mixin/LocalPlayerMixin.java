package net.alex.guzhenren.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 仅客户端 Mixin，在二段飞行[Second-Stage Flight]期间屏蔽本机跳跃与潜行键带来的垂直升降速度（与 {@link PlayerMixin} 配
 * 合）。
 * <ul>
 *   <li>{@code LocalPlayer#aiStep} 会在移动前按 3 倍飞行速度叠加按键升降量；当 {@link SecondStageService#isActive} 为真
 *       时，{@link #guzhenren$holdSecondStageAltitude} 将此处的飞行速度读数屏蔽为 0，确保巡航严格跟随视线方向而不因按键
 *       发生高度漂移。</li>
 *   <li>⚠ 不使用事件或 API：NeoForge 21.1.265 未对 {@code aiStep} 的该段升降逻辑提供任何事件钩子，本修改点仅拦截此处读
 *       数而不影响其他位置对飞行速度的读取。</li>
 * </ul>
 */

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
    private float guzhenren$holdSecondStageAltitude(float speed) {
        return SecondStageService.isActive((LocalPlayer) (Object) this) ? 0.0F : speed;
    }
}
