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
 * strength. The state is the permanent path effect {@code ModPathEffectTypes.HARDSHIP_STRENGTH}, started and stopped
 * by the Gu's auto use ({@code AutoUse}); {@link #isActive} is what the round keeper reads to let a round release
 * several beast families at once.
 *
 * <p>The injury tier is read live from the health share ({@link #getTier}): above 60% and under 75% is tier 1,
 * then every 15% further down one more, to tier 5 at 15% or less; 75% and above is no tier. Its amplifier adds
 * {@link #POINT_PERCENT} of the permanent points [力量点] (rounded down) and no family. Its capacity bonus
 * [承受力] depends on the branch: a beast phantom [兽力虚影] body carries the points it gains, any other body
 * gains {@link #HUMAN_CAPACITY_PERCENT} of its normal capacity [常态承受力] (rounded down). With no permanent
 * points the state adds nothing.
 *
 * <p>⚠ Provisional, awaiting Alex: the five point shares 10/20/30/40/50% and the five human capacity shares
 * 100/125/150/175/200%.
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

    public static final int TIERS = 5;
    public static final float START_BELOW = 0.75F;
    public static final float HEALED_SHARE = START_BELOW - 1.0E-4F;
    private static final float[] TIER_CEILINGS = { 0.60F, 0.45F, 0.30F, 0.15F };
    private static final int[] POINT_PERCENT = { 10, 20, 30, 40, 50 };
    private static final int[] HUMAN_CAPACITY_PERCENT = { 100, 125, 150, 175, 200 };

    public static boolean isActive(@NotNull Player player) {
        return PathEffectService.find(player, ModPathEffectTypes.HARDSHIP_STRENGTH) != null;
    }

    public static boolean isHurt(float health, float maxHealth) { return health < maxHealth * HEALED_SHARE; }

    public static int getTier(float health, float maxHealth) {
        if (health <= 0.0F || !isHurt(health, maxHealth)) return 0;

        int tier = 1;
        for (float ceiling : TIER_CEILINGS) {
            if (health > maxHealth * ceiling) break;
            tier++;
        }
        return tier;
    }

    public static int getPoints(int permanentPoints, int tier) {
        if (tier <= 0 || permanentPoints <= 0) return 0;
        return (int) ((long) permanentPoints * POINT_PERCENT[Math.min(tier, TIERS) - 1] / 100L);
    }

    public static int getCapacityBonus(boolean beastPhantom, int points, int normalCapacity, int tier) {
        if (tier <= 0 || points <= 0) return 0;
        if (beastPhantom) return points;
        return (int) ((long) Math.max(0, normalCapacity) * HUMAN_CAPACITY_PERCENT[Math.min(tier, TIERS) - 1] / 100L);
    }

    public static @Nullable StrengthAmplifier getAmplifier(@NotNull ResourceLocation source, boolean beastPhantom,
                                                           int permanentPoints, int normalCapacity, int tier) {
        int points = getPoints(permanentPoints, tier);
        if (points <= 0) return null;
        return new StrengthAmplifier(source, null, points,
                getCapacityBonus(beastPhantom, points, normalCapacity, tier));
    }

    public record State() implements StrengthAmplifierProvider {

        public static final State INSTANCE = new State();
        public static final MapCodec<State> CODEC = MapCodec.unit(INSTANCE);
        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public @Nullable StrengthAmplifier getAmplifier(@NotNull Player player, int permanentPoints,
                                                        int normalCapacity) {
            return HardshipStrength.getAmplifier(ModPathEffectTypes.HARDSHIP_STRENGTH.getId(),
                    PathStrengthService.holdsBranch(player, StrengthPathBranch.BEAST_STRENGTH_PHANTOM),
                    permanentPoints, normalCapacity, getTier(player.getHealth(), player.getMaxHealth()));
        }
    }
}
