package net.alex.guzhenren.mixin;

import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 二段飞行[Second-Stage Flight]时把玩家移动交给二段巡航；双端。
 * <ul>
 *   <li>{@link SecondStageService#travel} 移动了玩家时 {@link #guzhenren$cruise} 在方法头取消 {@code Player#travel}：
 *       限未骑乘、带标记的飞行者，在模拟它的一端（本机客户端、服务端自身那步）；客户端上的远端玩家从不跑移动。</li>
 *   <li>原版飞行分支按输入缩放飞行速度、以 0.91 阻尼；巡航随视线，故替换整个移动而非一次速度读数。</li>
 *   <li>⚠ 不用事件或 API：NeoForge 21.1.265 的 {@code Player#travel} 不发事件，{@code PlayerTickEvent} 晚于移动。</li>
 * </ul>
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
