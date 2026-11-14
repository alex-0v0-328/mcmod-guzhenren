package net.alex.guzhenren.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 温养[Nourish]运镜改写本帧镜头的位置、朝向与离体标记；仅客户端，由 {@link GameRendererMixin} 在镜头摆好后调用。
 * ⚠ 不用事件或 API：{@code ViewportEvent.ComputeCameraAngles} 只给角度，且 NeoForge 21.1.265 在 {@code setup} 设位置前
 * 发出；Epic Fight 第三人称镜头取消原版 {@code setup}，连该事件也不发。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mixin(Camera.class)
public interface CameraAccessor {

    @Invoker("setPosition")
    void guzhenren$setPosition(Vec3 position);

    @Invoker("setRotation")
    void guzhenren$setRotation(float yRot, float xRot, float roll);

    @Accessor("detached")
    void guzhenren$setDetached(boolean detached);
}
