package net.alex.guzhenren.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.alex.guzhenren.client.camera.CombatCameraRig;
import net.alex.guzhenren.client.cinematic.CultivationCamera;
import net.alex.guzhenren.client.screen.immortalaperture.map.MapPage;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 仅客户端 Mixin，在每帧世界渲染的镜头架设完成后依次应用战斗构图帧（{@link CombatCameraRig#getFrame}）
 * 与温养[Nourish]运镜帧（{@link CultivationCamera#getFrame}）；福地地图页全屏时整帧跳过世界渲染。
 * <ul>
 *   <li>跳过世界渲染包在 {@code render} 调 {@code renderLevel} 的那一处（{@link MapPage#hidesWorld}），整个
 *       {@code renderLevel} 连同光影模组挂在它里面的首尾钩子一起不跑；主帧缓冲每帧开头已清屏，地图页铺满整窗。</li>
 *   <li>注入点位于 {@code renderLevel} 调用 {@code Camera#setup} 之后、视锥剔除之前，此时原版与 Epic Fight 第三人称镜头
 *       均已计算完毕，改写结果会同时作用于视锥剔除、方块实体渲染与声音监听位置。</li>
 *   <li>⚠ 不使用事件或 API 的原因见 {@link CameraAccessor}。</li>
 * </ul>
 */

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;"
                    + "Lnet/minecraft/world/entity/Entity;ZZF)V"))
    private void guzhenren$frameCultivation(DeltaTracker deltaTracker, CallbackInfo info) {
        CameraAccessor accessor = (CameraAccessor) mainCamera;
        CombatCameraRig.Frame combat = CombatCameraRig.getFrame(mainCamera);
        if (combat != null) {
            accessor.guzhenren$setPosition(combat.position());
            accessor.guzhenren$setRotation(combat.yaw(), combat.pitch(), combat.roll());
        }
        CultivationCamera.Frame frame = CultivationCamera.getFrame(mainCamera);
        if (frame == null) return;
        accessor.guzhenren$setPosition(frame.position());
        accessor.guzhenren$setRotation(frame.yaw(), frame.pitch(), frame.roll());
        accessor.guzhenren$setDetached(frame.detached());
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"))
    private boolean guzhenren$keepLevelUnderMap(GameRenderer renderer, DeltaTracker deltaTracker) {
        return !MapPage.hidesWorld();
    }
}
