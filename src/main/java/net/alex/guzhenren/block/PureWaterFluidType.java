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
 * 纯水流体类型（{@link FluidType}）：在 {@code ModFluidTypes} 复刻原版水属性的基础上，补齐 NeoForge 按流体类型身份分派
 * 的回调。
 * <ul>
 *   <li>当 NeoForge 在流体标签之外查询实体能否游泳（{@link #canSwim}）、是否溺水（{@link #canDrownIn}）、水下是否脱离载
 *       具（{@link #canRideVehicleUnder}）以及无限水源转化（{@link #canConvertToSource}）时，统一对齐原版水类型的判定逻
 *       辑（例如怕水生物不可游、具备水下呼吸能力的生物不溺水、跟随 {@code waterSourceConversion} 游戏规则）。</li>
 *   <li>{@link #canDrownIn} 内部调用已标记过时的 {@link LivingEntity#canBreatheUnderwater()}，是因为该方法仅对外部调用
 *       方过时（外部应调 {@code canDrownInFluidType} 并最终路由至此），NeoForge 原版水类型分支自身同样直接读取该方
 *       法。</li>
 * </ul>
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
