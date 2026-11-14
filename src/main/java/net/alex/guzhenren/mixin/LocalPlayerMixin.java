package net.alex.guzhenren.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 二段飞行[Second-Stage Flight]中屏蔽本机跳跃、潜行的升降；仅客户端。
 * <ul>
 *   <li>{@code LocalPlayer#aiStep} 移动前给速度加 3 倍飞行速度的升降；{@link #guzhenren$holdSecondStageAltitude} 在有同
 *       步标记时把此处飞行速度读成 0，巡航（{@link PlayerMixin}）只随视线、不受按键漂移。</li>
 *   <li>⚠ 不用事件或 API：NeoForge 21.1.265 对 {@code aiStep} 这段升降无钩子；只在此处读 0，别处飞行速度不受影响。</li>
 * </ul>
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
    private float guzhenren$holdSecondStageAltitude(float speed) {
        return SecondStageService.isActive((LocalPlayer) (Object) this) ? 0.0F : speed;
    }
}
