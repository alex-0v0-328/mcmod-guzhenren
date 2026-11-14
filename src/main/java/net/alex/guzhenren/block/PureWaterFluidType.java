package net.alex.guzhenren.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 纯水流体类型：{@code ModFluidTypes} 逐项复制 NeoForge 水类型属性，再补水类型凭身份回答的覆写。NeoForge 在标签够不到处
 * 问“是不是水类型”（谁能游、谁溺水、谁在水下下坐骑），这里都按水答：怕水的不能游，能水下呼吸的不溺水，水下卸乘客的载具
 * 在纯水里同样卸；源头转化跟随 {@code waterSourceConversion} 游戏规则。
 *
 * @author Alex
 * @version 1.0.0
 * @see PureWaterFluid
 * @since 1.0.0
 */

public class PureWaterFluidType extends FluidType {

    public PureWaterFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canConvertToSource(@NotNull FluidState state, @NotNull LevelReader reader, @NotNull BlockPos pos) {
        if (reader instanceof Level level) {
            return level.getGameRules().getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION);
        }
        return super.canConvertToSource(state, reader, pos);
    }

    @Override
    public boolean canSwim(@NotNull Entity entity) {
        return entity instanceof LivingEntity living ? !living.isSensitiveToWater() : super.canSwim(entity);
    }

    // canBreatheUnderwater is deprecated for callers of canDrownInFluidType, which lands here: NeoForge's own
    // water branch reads it the same way.
    @SuppressWarnings("deprecation")
    @Override
    public boolean canDrownIn(@NotNull LivingEntity entity) { return !entity.canBreatheUnderwater(); }

    @Override
    public boolean canRideVehicleUnder(@NotNull Entity vehicle, @NotNull Entity rider) {
        return !vehicle.dismountsUnderwater();
    }

    @Override
    public @Nullable PathType getBlockPathType(@NotNull FluidState state, @NotNull BlockGetter level,
                                               @NotNull BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
        return canFluidLog ? super.getBlockPathType(state, level, pos, mob, true) : null;
    }
}
