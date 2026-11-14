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
 * 元泉[Spirit Spring]的客户端渲染；贴图由 Alex 预先上色，无运行时着色。
 *
 * <ul>
 *   <li>水下雾取 HUD 真元[Essence]条强调色 {@link ModPalette#APERTURE}，与 HUD 同一种天蓝。</li>
 *   <li>暂无水下覆盖贴图，元泉内只显示雾，待 Alex 画好再补。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
