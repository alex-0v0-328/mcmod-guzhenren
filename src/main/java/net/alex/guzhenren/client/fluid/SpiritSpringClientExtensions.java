package net.alex.guzhenren.client.fluid;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModPalette;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * 元泉客户端渲染扩展：贴图已由 Alex 预先调色，不做运行时着色。
 * <ul>
 *   <li>水下雾颜色复用 HUD 真元条强调色 {@link ModPalette#APERTURE}，保持与空窍面板相同的天蓝色调。</li>
 *   <li>目前暂缺专用水下覆盖层贴图，玩家进入元泉时仅渲染水下雾效，待后续补绘贴图后再行接入。</li>
 * </ul>
 */

public class SpiritSpringClientExtensions implements IClientFluidTypeExtensions {

    private static final ResourceLocation STILL = Guzhenren.id("block/spirit_spring_still");
    private static final ResourceLocation FLOWING = Guzhenren.id("block/spirit_spring_flow");

    @Override
    public @NotNull ResourceLocation getStillTexture() { return STILL; }

    @Override
    public @NotNull ResourceLocation getFlowingTexture() { return FLOWING; }

    @Override
    public @NotNull Vector3f modifyFogColor(@NotNull Camera camera, float partialTick, @NotNull ClientLevel level,
                                            int renderDistanceChunks, float distance, @NotNull Vector3f fogColor) {
        int accent = ModPalette.APERTURE;
        return fogColor.set(((accent >>> 16) & 0xFF) / 255.0F, ((accent >>> 8) & 0xFF) / 255.0F,
                (accent & 0xFF) / 255.0F);
    }
}
