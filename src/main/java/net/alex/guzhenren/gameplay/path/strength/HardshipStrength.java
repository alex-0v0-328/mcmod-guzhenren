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
 * 苦力状态：苦力蛊[Hardship Strength Gu]开启期间按伤势档位放大伤害倍率，并决定兽力虚影流[Beast Strength Phantom]暴击抽
 * 几种兽、人力钧力流[Human Jun Strength]范围伤害[Area Damage]放大几档。
 * <ul>
 *   <li>作为带到期时刻的流派效果由苦力蛊自动催动（{@code AutoUse}）开关；仅对已入人力钧力流或兽力虚影流（含生效中的限
 *       时兽力）的玩家提供伤害倍率（{@link StrengthAmplifier#damage}，不加力量点[Strength Points]、不加承受力）。</li>
 *   <li>伤势分五档（{@link #getTier}）：当前生命低于最大生命的 75%（{@link #START_BELOW}）起算，高于 60% 为 1 档，每再
 *       低 15% 升一档，不高于 15% 为 5 档；五档伤害倍率依次 +10% / 20% / 30% / 40% / 50%（{@link #getDamagePercent}）。
 *       </li>
 *   <li>{@link #getTier(Player)} 是其他规则读档位的唯一入口：苦力未开或未入两流时为 {@code 0}；兽力暴击据此抽
 *       {@code 1 + 档位} 种（{@link StrengthCrit#getCount}），人力流范围据此放大（{@link AreaDamage#getWidenTier}）。</li>
 *   <li>⚠ 档位分界统一用乘法 {@code health > maxHealth * floor} 比较而非除法，恰好落在分界点时归入伤势更重的一档；
 *       {@link #HEALED_SHARE} 留有 {@code 1e-4} 浮点容差，确保生命恢复至 75% 时稳定判定为痊愈。</li>
 * </ul>
 */

public final class HardshipStrength {

    private HardshipStrength() {}

    public static final int TIERS = Tier.values().length;
    public static final float START_BELOW = 0.75F;
    public static final float HEALED_SHARE = START_BELOW - 1.0E-4F;

    private enum Tier {

        ONE(0.60F, 10),
        TWO(0.45F, 20),
        THREE(0.30F, 30),
        FOUR(0.15F, 40),
        FIVE(0.0F, 50);

        private final float floor;
        private final int damagePercent;

        Tier(float floor, int damagePercent) {
            this.floor = floor;
            this.damagePercent = damagePercent;
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

    public static int getTier(@NotNull Player player) {
        if (!isActive(player) || PathStrengthService.getHeldBranch(player) == StrengthPathBranch.NORMAL) return 0;
        return getTier(player.getHealth(), player.getMaxHealth());
    }

    public static int getDamagePercent(int tier) { return tier <= 0 ? 0 : Tier.of(tier).damagePercent; }

    public static @Nullable StrengthAmplifier getAmplifier(@NotNull ResourceLocation source, boolean branchHeld,
                                                           int tier) {
        int percent = branchHeld ? getDamagePercent(tier) : 0;
        return percent > 0 ? StrengthAmplifier.damage(source, percent) : null;
    }

    public record State() implements StrengthAmplifierProvider {

        public static final State INSTANCE = new State();
        public static final MapCodec<State> CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public @Nullable StrengthAmplifier getAmplifier(@NotNull Player player, int permanentPoints,
                                                        int capacity) {
            return HardshipStrength.getAmplifier(ModPathEffectTypes.HARDSHIP_STRENGTH.getId(),
                    PathStrengthService.getHeldBranch(player) != StrengthPathBranch.NORMAL,
                    getTier(player.getHealth(), player.getMaxHealth()));
        }
    }
}
