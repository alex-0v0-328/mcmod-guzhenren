package net.alex.guzhenren.gameplay.aperture;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 元石[Primeval Stone]袋：温养[Nourish]对随身元石的三项需求，使空窍[Aperture]不依赖存放元石的物品包。
 *
 * <ul>
 *   <li>{@link #canAfford} 问真元[Essence]加元石够否，{@link #topUp} 补足线，{@link #spend} 先真元后元石付款。</li>
 *   <li>入口类在可温养前把 {@code PrimevalStoneSupply} 所建实例交给 {@code ApertureNourishService.setStonePurse}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureNourishService
 * @since 1.0.0
 */

public record StonePurse(BiPredicate<Player, Long> canAfford, Consumer<ServerPlayer> topUp,
        BiPredicate<ServerPlayer, Long> spend) {}
