package net.alex.guzhenren.gameplay.aperture;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 随身元石[Primeval Stone]支付桥接记录：封装温养[Nourish]与冲刷窍壁对随身元石的三项操作，使 {@code gameplay.aperture}
 * 不依赖定义元石的物品包。
 * <ul>
 *   <li>{@link #canAfford} 判定当前真元加随身元石是否够付，{@link #topUp} 用元石将真元补过温养扣费线，{@link #spend} 先
 *       扣真元再扣元石完成支付。</li>
 *   <li>模组主类在构造期将基于 {@code PrimevalStoneSupply} 的实例注入
 *       {@link ApertureNourishService#setStonePurse}。</li>
 * </ul>
 */

public record StonePurse(BiPredicate<Player, Long> canAfford, Consumer<ServerPlayer> topUp,
        BiPredicate<ServerPlayer, Long> spend) {}
