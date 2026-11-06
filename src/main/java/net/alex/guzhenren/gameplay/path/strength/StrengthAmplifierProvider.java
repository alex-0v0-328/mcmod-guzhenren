package net.alex.guzhenren.gameplay.path.strength;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A path effect payload that amplifies the body's strength while it runs. {@code StrengthService.getAmplifiers} asks
 * every running payload that implements it, handing in the permanent points [力量点] and the normal carrying
 * capacity [常态承受力] so a provider can size itself by either; it answers one {@link StrengthAmplifier} or
 * {@code null} for nothing right now.
 *
 * <p>Timed beast buffs are not providers: their worth is declared on the vanilla effect ({@link BeastStrengthBuff})
 * and read by {@link PathStrengthService#getBeastBuffs}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public interface StrengthAmplifierProvider {

    @Nullable StrengthAmplifier getAmplifier(@NotNull Player player, int permanentPoints, int normalCapacity);
}
