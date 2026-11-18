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
 * 双端 Mixin，使处于纯水中的实体在原版水标签判定中同样被视为处于水中。
 * <ul>
 *   <li>NeoForge 对实体的接触水体、眼部浸水与水深读取三个方法仅按原版 {@code WATER_TYPE} 作答，不自动识别
 *       {@code #minecraft:water} 标签下的纯水；本类通过 {@code @ModifyReturnValue} 将纯水流体类型纳入这三个判定，从而使
 *       {@code isInWater}、{@code isUnderWater} 及其衍生原版机制（鱼类呼吸、末影人遇水受伤、溺尸转化、水下挖掘与视野遮
 *       罩）正确生效。</li>
 *   <li>⚠ 不使用事件或 API：NeoForge 21.1.265 对这三个标签重载仅在 {@code NeoForgeMod.WATER_TYPE} 上提供已弃用的垫片，
 *       且不存在可扩展至其他自定义流体类型的公共事件或标志位。</li>
 * </ul>
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
