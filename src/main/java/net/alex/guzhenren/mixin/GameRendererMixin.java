package net.alex.guzhenren.mixin;

import net.alex.guzhenren.client.camera.CombatCameraRig;
import net.alex.guzhenren.client.cinematic.CultivationCamera;
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
 * 镜头摆好后依次按战斗构图（{@link CombatCameraRig#getFrame}）与温养[Nourish]运镜（{@link CultivationCamera#getFrame}）
 * 改写本帧镜头；仅客户端。
 * <ul>
 *   <li>注入点在 {@code renderLevel} 调用 {@code Camera#setup} 之后、视锥剔除之前：原版、Epic Fight 第三人称都已摆完，
 *       改写对剔除、方块与实体绘制、声音监听位置一并生效。</li>
 *   <li>⚠ 不用事件或 API：原因见 {@link CameraAccessor}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see CameraAccessor
 * @since 1.0.0
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
}
