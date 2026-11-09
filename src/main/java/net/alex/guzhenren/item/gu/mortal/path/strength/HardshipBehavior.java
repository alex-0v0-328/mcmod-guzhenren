package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The Hardship Strength behavior [苦力]: an auto state ({@link AutoUseBehavior}) that starts under 75% of max health
 * on a body holding a permanent human jun [人力钧力流] or beast phantom [兽力虚影流] strength whose normal capacity
 * [常态承受力] gains at least one point at the lightest tier -- less would be billed for nothing -- and runs for a
 * {@link #DURATION_SECONDS} window whatever the health does meanwhile (at 75% and above it lends nothing). What it
 * lends, tier by tier, is {@link HardshipStrength}'s.
 *
 * @author Alex
 * @version 1.0.0
 * @see AutoUse
 * @see HardshipStrength
 * @since 1.0.0
 */

public record HardshipBehavior() implements AutoUseBehavior {

    public static final int DURATION_SECONDS = 30;

    @Override
    public @NotNull PathEffectType<?> getStateType() { return ModPathEffectTypes.HARDSHIP_STRENGTH; }

    @Override
    public boolean canStart(@NotNull ServerPlayer player) {
        PathStrengthData data = PathStrengthService.get(player);
        boolean branch = data.hasPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH)
                || data.hasPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM);
        return branch && HardshipStrength.getCapacityBonus(StrengthService.getNormalCapacity(player), 1) > 0;
    }

    @Override
    public int getDurationSeconds() { return DURATION_SECONDS; }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.HARDSHIP_STRENGTH,
                AutoUse.getEndTick(player, this), HardshipStrength.State.INSTANCE));
    }
}
