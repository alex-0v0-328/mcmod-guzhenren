package net.alex.guzhenren.gameplay.path.strength;

import com.mojang.serialization.MapCodec;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 苦力状态：苦力蛊[Hardship Strength Gu]运行时伤势借出承受力。
 * <ul>
 *   <li>永久流派效果，由该蛊自动催动（{@code AutoUse}）开关；{@link #isActive} 让一轮抽多个兽种。</li>
 *   <li>档位实读生命占比：低于 75% 起算，高于 60% 为 1 档，每再低 15% 加一档，≤ 15% 为 5 档；只按档抬承受力（常态承受力
 *       的百分比，向下取整），不加力量点[Strength Points]，仅限持人力钧力流或兽力虚影流者。</li>
 *   <li>⚠ 边界以 {@code max * bound} 比较而非商，恰在边界落入较低档。</li>
 *   <li>⚠ {@link #HEALED_SHARE} 含自动催动的浮点容差，回复停在 75% 即算痊愈。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthAmplifierProvider
 * @since 1.0.0
 */

public final class HardshipStrength {

    private HardshipStrength() {}

    public static final int TIERS = Tier.values().length;
    public static final float START_BELOW = 0.75F;
    public static final float HEALED_SHARE = START_BELOW - 1.0E-4F;

    private enum Tier {

        ONE(0.60F, 5),
        TWO(0.45F, 10),
        THREE(0.30F, 25),
        FOUR(0.15F, 50),
        FIVE(0.0F, 100);

        private final float floor;
        private final int capacityPercent;

        Tier(float floor, int capacityPercent) {
            this.floor = floor;
            this.capacityPercent = capacityPercent;
        }

        private int getLevel() { return ordinal() + 1; }

        private static Tier of(int level) { return values()[Math.min(level, TIERS) - 1]; }
    }

    public static boolean isActive(@NotNull Player player) {
        return PathEffectService.find(player, ModPathEffectTypes.HARDSHIP_STRENGTH) != null;
    }

    public static boolean isHurt(float health, float maxHealth) { return health < maxHealth * HEALED_SHARE; }

    public static int getTier(float health, float maxHealth) {
        if (health <= 0.0F || !isHurt(health, maxHealth)) return 0;

        for (Tier tier : Tier.values()) {
            if (health > maxHealth * tier.floor) return tier.getLevel();
        }
        return TIERS;
    }

    public static int getCapacityBonus(int normalCapacity, int tier) {
        if (tier <= 0) return 0;
        return (int) ((long) Math.max(0, normalCapacity) * Tier.of(tier).capacityPercent / 100L);
    }

    public static @Nullable StrengthAmplifier getAmplifier(@NotNull ResourceLocation source, boolean branchHeld,
                                                           int normalCapacity, int tier) {
        int bonus = branchHeld ? getCapacityBonus(normalCapacity, tier) : 0;
        return bonus > 0 ? new StrengthAmplifier(source, null, 0, bonus) : null;
    }

    public record State() implements StrengthAmplifierProvider {

        public static final State INSTANCE = new State();
        public static final MapCodec<State> CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public @Nullable StrengthAmplifier getAmplifier(@NotNull Player player, int permanentPoints,
                                                        int normalCapacity) {
            return HardshipStrength.getAmplifier(ModPathEffectTypes.HARDSHIP_STRENGTH.getId(),
                    PathStrengthService.getHeldBranch(player) != StrengthPathBranch.NORMAL, normalCapacity,
                    getTier(player.getHealth(), player.getMaxHealth()));
        }
    }
}
