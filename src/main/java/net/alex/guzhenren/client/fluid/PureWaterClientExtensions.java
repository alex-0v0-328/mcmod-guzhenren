package net.alex.guzhenren.client.fluid;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;

/**
 * Client rendering of the Pure Water [纯水]: vanilla's water strips multiplied by {@code #9FB4FF} at 170 alpha,
 * baked into the PNGs, so no runtime tint and no biome water color. Underwater fog and the screen overlay stay
 * vanilla water's: the camera reads the {@code #minecraft:water} tag, and {@code EntityMixin} puts the eyes
 * under water.
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
