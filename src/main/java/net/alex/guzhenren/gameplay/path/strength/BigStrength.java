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
 * 大力状态：大力蛊[Big Strength Gu]生效期间提供的临时力量点[Strength Points]与折半承受力增幅。
 * <ul>
 *   <li>作为限时流派效果挂载，力量点数取自该效果上的力道道痕[Path Marks]点，并附带 {@code points / CAPACITY_DIVISOR}
 *       （向下取整，即一半）的临时承受力加成，使承受力已满的身体也能发挥出至少一半的新增力量。</li>
 *   <li>属于非兽力增幅（{@code beastFamily == null}）：超出当前总承受力的部分直接截断而不触发肉体过载反噬；不限制力道流
 *       派分支，不计入永久兽力准入，也不计入全面解放基数。</li>
 * </ul>
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
