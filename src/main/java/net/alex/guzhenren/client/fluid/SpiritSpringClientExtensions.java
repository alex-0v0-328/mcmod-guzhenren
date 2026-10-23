package net.alex.guzhenren.client.fluid;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModPalette;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * Client rendering of the Spirit Spring [元泉]: pre-colored textures (Alex colors the PNGs
 * directly, so no runtime tint) and an underwater fog taken from the HUD essence-bar accent
 * {@link ModPalette#APERTURE} -- the spring and the HUD share one sky blue. There is no water
 * overlay texture yet; being inside the spring shows fog only until Alex paints one.
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
