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
 * 大力状态：大力蛊[Big Strength Gu]生效期间提供的增幅力量点[Strength Points]。
 * <ul>
 *   <li>作为限时流派效果挂载，力量点数取自该效果上的力道道痕[Path Marks]点，登记为一个增幅来源
 *       （{@link StrengthAmplifier#points}）：不带承受力，超出承受力的部分全额发挥并结算过载反噬。</li>
 *   <li>属于非兽力增幅：不限制力道流派分支，不过准入，不出兽影，也不计入暴击。</li>
 * </ul>
 */

public final class BigStrength {

    private BigStrength() {}

    public static final int POINTS = 32;

    public static boolean isActive(@NotNull Player player) {
        return PathEffectService.find(player, ModPathEffectTypes.BIG_STRENGTH) != null;
    }

    public static @Nullable StrengthAmplifier getAmplifier(@NotNull ResourceLocation source, int points) {
        return points > 0 ? StrengthAmplifier.points(source, points) : null;
    }

    public record State() implements StrengthAmplifierProvider {

        public static final State INSTANCE = new State();
        public static final MapCodec<State> CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public @Nullable StrengthAmplifier getAmplifier(@NotNull Player player, int permanentPoints,
                                                        int capacity) {
            PathEffect<State> effect = PathEffectService.find(player, ModPathEffectTypes.BIG_STRENGTH);
            if (effect == null) return null;
            return BigStrength.getAmplifier(effect.source(),
                    PathStrengthData.getPoints(effect.getMarks(GuPath.STRENGTH).points()));
        }
    }
}
