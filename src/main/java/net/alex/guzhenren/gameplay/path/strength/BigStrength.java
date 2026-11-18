package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.MapCodec;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 大力状态：大力蛊[Big Strength Gu]运行时临时加力量点[Strength Points]，并带点数一半的承受力。
 * <ul>
 *   <li>限时流派效果，点数读该效果的力道道痕点（同限时兽力[Beast Strength]），承受加成为点数
 *       ÷ {@link #CAPACITY_DIVISOR}，向下取整：承受力已满的身体也能发挥一半。</li>
 *   <li>非兽力增幅：超当前承受力部分只截断，不走肉体过载；不限流派，不计永久兽力准入，不进全面解放基数。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthAmplifierProvider
 * @since 1.0.0
 */

public final class BigStrength {

    private BigStrength() {}

    public static final int POINTS = 32;
    public static final int CAPACITY_DIVISOR = 2;

    public static boolean isActive(@NotNull Player player) {
        return PathEffectService.find(player, ModPathEffectTypes.BIG_STRENGTH) != null;
    }

    public static int getCapacityBonus(int points) { return Math.max(0, points) / CAPACITY_DIVISOR; }

    public static @Nullable StrengthAmplifier getAmplifier(@NotNull ResourceLocation source, int points) {
        return points > 0 ? new StrengthAmplifier(source, null, points, getCapacityBonus(points)) : null;
    }

    public record State() implements StrengthAmplifierProvider {

        public static final State INSTANCE = new State();
        public static final MapCodec<State> CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public @Nullable StrengthAmplifier getAmplifier(@NotNull Player player, int permanentPoints,
                                                        int normalCapacity) {
            PathEffect<State> effect = PathEffectService.find(player, ModPathEffectTypes.BIG_STRENGTH);
            if (effect == null) return null;
            return BigStrength.getAmplifier(effect.source(),
                    PathStrengthData.getPoints(effect.getMarks(GuPath.STRENGTH).points()));
        }
    }
}
