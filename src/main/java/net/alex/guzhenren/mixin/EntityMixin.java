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
 * 让纯水中的实体算作在水中；双端。
 * <ul>
 *   <li>NeoForge 只按水流体类型答实体的三个原版水标签问题（接触水、眼在水中、水高度），不看纯水所在的
 *       {@code #minecraft:water} 标签；每个包装把一个答案放宽到纯水，于是 {@code isInWater}、{@code isUnderWater} 及其
 *       衍生（鱼呼吸、末影人遇水受伤、溺尸转化、水下挖掘、水下遮罩）都视其为水；移动、溺水、游泳本走流体类型。</li>
 *   <li>⚠ 不用事件或 API：NeoForge 21.1.265 的三个标签重载是 {@code NeoForgeMod.WATER_TYPE} 上的弃用垫片，也无事件或流
 *       体类型标志能放宽到别的流体类型。</li>
 * </ul>
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
