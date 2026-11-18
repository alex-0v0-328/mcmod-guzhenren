package net.alex.guzhenren.gameplay.trade;

import java.util.List;
import java.util.function.Supplier;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 宝黄天[Treasure Yellow Heaven]交易魂 NPC 枚举：定义各交易者的注册标识与延迟构建的报价表。
 * <ul>
 *   <li>每个枚举项以 {@link #getId()} 注册独立实体类型并共用 {@link SoulTraderEntity} 逻辑（具体模型与贴图由客户端
 *       渲染器按实体类型选配，同一种魂体外观可复用于多个 NPC）。</li>
 *   <li>⚠ 报价表通过 {@code Supplier} 在 {@link #getOffers()} 首次调用时惰性构建：注册实体类型时本枚举已加载，而
 *       {@code ModItems} 物品尚未完成注册，严禁在枚举构造期直接求值。</li>
 *   <li>{@link #TEST_TRADE_GU_IMMORTAL} 为首个测试交易者，换出的白豕蛊[White Boar Gu]不附带炼化归属数据，与野
 *       外直接捕获的野生蛊相同。</li>
 * </ul>
 */

public enum SoulTrader {

    TEST_TRADE_GU_IMMORTAL("test_trade_gu_immortal", () -> List.of(
            SoulTradeOffer.of(new ItemStack(Items.PORKCHOP, 64), TradeCost.of(ModItems.PRIMEVAL_STONE.get(), 10)),
            SoulTradeOffer.of(new ItemStack(ModItems.WHITE_BOAR_GU_1.get()), TradeCost.of(Items.DIRT, 192))));

    private final String id;
    private final Supplier<List<SoulTradeOffer>> offers;

    SoulTrader(String id, Supplier<List<SoulTradeOffer>> offers) {
        this.id = id;
        this.offers = offers;
    }

    public String getId() { return this.id; }

    public List<SoulTradeOffer> getOffers() { return this.offers.get(); }
}
