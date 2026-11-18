package net.alex.guzhenren.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 仅客户端访问器，供 {@link GameRendererMixin} 在温养[Nourish]运镜生效时直接改写 {@link Camera} 的位置、旋转角与离体标
 * 记。
 * <ul>
 *   <li>⚠ 不使用事件或 API：NeoForge 21.1.265 的 {@code ViewportEvent.ComputeCameraAngles} 仅提供角度修改且触发于
 *       {@code setup} 设置位置之前，而 Epic Fight 第三人称镜头会取消原版 {@code setup} 导致该事件根本不触发。</li>
 * </ul>
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
