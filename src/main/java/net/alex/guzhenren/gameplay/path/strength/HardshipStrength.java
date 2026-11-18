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
 * 苦力状态：苦力蛊[Hardship Strength Gu]开启期间按当前已损生命比例动态提升肉身承受力，
 * 并使兽力虚影流[Beast Strength Phantom]每轮可抽取多个兽种。
 * <ul>
 *   <li>作为永久流派效果由苦力蛊自动催动（{@code AutoUse}）开关；仅对已入人力钧力流或兽力虚影流的玩家提供承受力加成（按
 *       常态承受力的百分比向下取整，不直接增加力量点[Strength Points]）。</li>
 *   <li>伤势分五档（{@link #getTier}）：当前生命低于最大生命的 75%（{@link #START_BELOW}）起算，高于 60% 为 1 档，每再
 *       低 15% 升一档，不高于 15% 为 5 档。</li>
 *   <li>⚠ 档位分界统一用乘法 {@code health > maxHealth * floor} 比较而非除法，恰好落在分界点时归入较低档；
 *       {@link #HEALED_SHARE} 留有 {@code 1e-4} 浮点容差，确保生命恢复至 75% 时稳定判定为痊愈。</li>
 * </ul>
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
