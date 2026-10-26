package net.alex.guzhenren.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.alex.guzhenren.registry.fluid.ModFluidTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes an entity in the Pure Water [纯水] count as in water; both sides.
 *
 * <p>NeoForge answers the three vanilla water-tag questions an entity asks of itself -- touching water, eyes in
 * water, water height -- with the water fluid type alone, not the {@code #minecraft:water} tag the Pure Water
 * sits in. Each wrapper here widens one answer to the Pure Water type too, so {@code isInWater},
 * {@code isUnderWater} and everything built on them (fish breathing, endermen burning, drowned conversion,
 * submerged mining, the underwater overlay) treat it as water. Movement, drowning and swimming already ride the
 * fluid type and need nothing here.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyReturnValue(method = "updateFluidHeightAndDoFluidPushing(Lnet/minecraft/tags/TagKey;D)Z",
            at = @At("RETURN"))
    private boolean guzhenren$touchPureWater(boolean touching, TagKey<Fluid> fluidTag, double motionScale) {
        return touching
                || fluidTag == FluidTags.WATER && guzhenren$self().isInFluidType(ModFluidTypes.PURE_WATER.get());
    }

    @ModifyReturnValue(method = "isEyeInFluid", at = @At("RETURN"))
    private boolean guzhenren$eyeInPureWater(boolean inFluid, TagKey<Fluid> fluidTag) {
        return inFluid
                || fluidTag == FluidTags.WATER && guzhenren$self().isEyeInFluidType(ModFluidTypes.PURE_WATER.get());
    }

    @ModifyReturnValue(method = "getFluidHeight", at = @At("RETURN"))
    private double guzhenren$pureWaterHeight(double height, TagKey<Fluid> fluidTag) {
        return fluidTag == FluidTags.WATER
                ? Math.max(height, guzhenren$self().getFluidTypeHeight(ModFluidTypes.PURE_WATER.get())) : height;
    }

    @Unique
    private Entity guzhenren$self() { return (Entity) (Object) this; }
}
