package net.alex.guzhenren.gameplay.trade;

import java.util.List;
import java.util.function.Supplier;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 宝黄天[Treasure Yellow Heaven] 的交易 NPC。
 * <ul>
 *   <li>各以 {@link #getId()} 注册实体类型（同熊：一 NPC 一实体类型），共用 {@link SoulTraderEntity}；渲染器另选模型贴
 *       图，一种魂体外观可代多个 NPC；{@link #getOffers()} 按需构建：注册实体类型时已加载本枚举，物品尚无。</li>
 *   <li>{@code TEST_TRADE_GU_IMMORTAL}：首个测试交易者；10 元石[Primeval Stone] 换 64 生猪排，3 组泥土换 1 只白豕
 *       蛊[White Boar Gu]（如捕获般野生）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulTraderEntity
 * @since 1.0.0
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
