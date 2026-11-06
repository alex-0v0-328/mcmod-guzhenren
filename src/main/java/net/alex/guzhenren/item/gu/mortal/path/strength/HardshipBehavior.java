package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.attribute.StrengthService;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The Hardship Strength behavior [苦力]: an auto state ({@link AutoUseBehavior}) that starts under 75% of max health
 * on a body whose permanent strength points [力量点] lend at least one point at the lightest tier -- fewer would be
 * billed for nothing -- and runs until the body is back at 75%. What it lends, tier by
 * tier, is {@link HardshipStrength}'s.
 *
 * @author Alex
 * @version 1.0.0
 * @see AutoUse
 * @see HardshipStrength
 * @since 1.0.0
 */

public record HardshipBehavior() implements AutoUseBehavior {

    @Override
    public @NotNull PathEffectType<?> getStateType() { return ModPathEffectTypes.HARDSHIP_STRENGTH; }

    @Override
    public boolean canStart(@NotNull ServerPlayer player) {
        return HardshipStrength.getPoints(StrengthService.getPermanentPoints(player), 1) > 0;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.HARDSHIP_STRENGTH, PathEffect.PERMANENT,
                HardshipStrength.State.INSTANCE));
    }
}
