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
 * The Hardship Strength [苦力] state: while the Hardship Strength Gu [苦力蛊] runs, the body's injury lends it
 * carrying capacity. The state is the permanent path effect {@code ModPathEffectTypes.HARDSHIP_STRENGTH}, started and
 * stopped by the Gu's auto use ({@code AutoUse}); {@link #isActive} is what the round keeper reads to let a round draw
 * several beast families at once.
 *
 * <p>The injury tier is read live from the health share ({@link #getTier}): above 60% and under 75% is tier 1,
 * then every 15% further down one more, to tier 5 at 15% or less; 75% and above is no tier. Each {@link Tier} holds the
 * health share it stays above (its floor) and its capacity share. It lends no strength points [力量点]: its amplifier
 * only raises the carrying capacity [承受力] by the tier's share of the normal capacity [常态承受力] (rounded down),
 * 5/10/25/50/100%, and only on a body holding the human jun [人力钧力流] or the beast phantom [兽力虚影流] branch
 * ({@code PathStrengthService.getHeldBranch}); any other body gains nothing.
 *
 * <p>⚠ The bounds compare the health with {@code max * bound}, never a quotient, so a health set to exactly a bound
 * lands in the lower tier as written. {@link #HEALED_SHARE} carries the auto use's float tolerance, so a heal that
 * stops at 75% reads as healed.
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
