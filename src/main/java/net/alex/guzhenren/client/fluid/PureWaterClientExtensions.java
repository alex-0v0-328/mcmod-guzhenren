package net.alex.guzhenren.client.fluid;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;

/**
 * 纯水客户端渲染扩展：原版水贴图条已按 wiki《纯水》调色预烘焙进 PNG，不做运行时着色且不读取生物群系水色；水下雾与屏幕覆
 * 盖层沿用原版水效果，镜头依据 {@code #minecraft:water} 标签与 {@code EntityMixin} 判定眼睛是否位于水下。
 */

public class PureWaterClientExtensions implements IClientFluidTypeExtensions {

    private static final ResourceLocation STILL = Guzhenren.id("block/pure_water_still");
    private static final ResourceLocation FLOWING = Guzhenren.id("block/pure_water_flow");

    @Override
    public @NotNull ResourceLocation getStillTexture() { return STILL; }

    @Override
    public @NotNull ResourceLocation getFlowingTexture() { return FLOWING; }
}
