package net.alex.guzhenren.mixin;

import net.alex.guzhenren.gameplay.flight.SecondStageService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 双端 Mixin，在二段飞行[Second-Stage Flight]激活期间将 {@code Player#travel} 整体交由二段巡航逻辑接管（客户端垂直按键
 * 屏蔽见 {@link LocalPlayerMixin}）。
 * <ul>
 *   <li>{@link #guzhenren$cruise} 在 {@code Player#travel} 头部调用 {@link SecondStageService#travel}，仅对未骑乘且持有
 *       同步标记的本地客户端或服务端玩家生效并取消原版移动（客户端上的远端玩家不执行 {@code travel}）。</li>
 *   <li>原版飞行分支按键盘输入缩放速度并施加 0.91 阻力，而二段巡航必须严格沿视线矢量推进，因此必须整体替换移动逻辑而非
 *       仅修改飞行速度读数。</li>
 *   <li>⚠ 不使用事件或 API：NeoForge 21.1.265 的 {@code Player#travel} 不派发任何取消事件，而 {@code PlayerTickEvent}
 *       的执行时机晚于移动结算。</li>
 * </ul>
 */

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void guzhenren$cruise(Vec3 travelVector, CallbackInfo ci) {
        if (SecondStageService.travel((Player) (Object) this)) ci.cancel();
    }
}
