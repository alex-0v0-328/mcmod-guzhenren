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
 * The Pure Water [纯水] fluid type: NeoForge's water type, copied property for property by
 * {@code ModFluidTypes}, plus the overrides that type answers through its own identity.
 *
 * <p>NeoForge asks "is this the water type?" in a handful of places a tag cannot reach -- who swims, who drowns,
 * who dismounts under the surface -- so each is answered here the way water answers it: entities sensitive to
 * water cannot swim, entities that breathe underwater never drown, and a vehicle that dismounts underwater
 * does so in pure water too. Source conversion follows the {@code waterSourceConversion} game rule.
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
