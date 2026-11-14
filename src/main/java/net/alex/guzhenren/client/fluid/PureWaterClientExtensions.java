package net.alex.guzhenren.client.fluid;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;

/**
 * 纯水客户端渲染：原版水贴图条按 wiki《纯水》的颜色烘进 PNG，无运行时着色，不取生物群系水色；水下雾与屏幕覆盖层沿用原
 * 版水，镜头读 {@code #minecraft:water} 标签，{@code EntityMixin} 判定眼睛在水下。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class PureWaterClientExtensions implements IClientFluidTypeExtensions {

    private static final ResourceLocation STILL = Guzhenren.id("block/pure_water_still");
    private static final ResourceLocation FLOWING = Guzhenren.id("block/pure_water_flow");

    @Override
    public @NotNull ResourceLocation getStillTexture() { return STILL; }

    @Override
    public @NotNull ResourceLocation getFlowingTexture() { return FLOWING; }
}
