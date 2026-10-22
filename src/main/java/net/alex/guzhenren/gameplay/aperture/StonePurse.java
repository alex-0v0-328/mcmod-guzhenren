package net.alex.guzhenren.gameplay.aperture;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The three things nourishing [温养] needs from the primeval stones [元石] a player carries, so the aperture does not
 * depend on the item package that keeps them: {@link #canAfford} asks whether essence and stones together cover a
 * cost, {@link #topUp} runs the top-up line, and {@link #spend} pays a cost, essence first, then stones.
 *
 * <p>The item side fills it: the mod's entry point hands {@code ApertureNourishService.setStonePurse} the one built
 * on {@code PrimevalStoneSupply}, before any player can nourish.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureNourishService
 * @since 1.0.0
 */

public record StonePurse(BiPredicate<Player, Long> canAfford, Consumer<ServerPlayer> topUp,
        BiPredicate<ServerPlayer, Long> spend) {}
