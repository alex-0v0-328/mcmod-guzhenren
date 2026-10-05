package net.alex.guzhenren.gameplay.trade;

import java.util.List;
import java.util.function.Supplier;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;

/**
 * The trading NPCs of the Treasure Yellow Heaven [宝黄天]. Each registers its own entity type under
 * {@link #id()} (bear precedent: one NPC, one entity type) and shares {@link SoulTraderEntity}; the renderer
 * picks model and texture separately, because one soul look may stand for many NPCs.
 *
 * <p>{@link #offers()} is built on demand: the enum is loaded while entity types register, before any item
 * exists. {@code TEST_TRADE_GU_IMMORTAL} is the first, test-only trader: ten primeval
 * stones for 64 porkchops, and three stacks of dirt for one White Boar Gu, handed over wild like a caught Gu.
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulTraderEntity
 * @since 1.0.0
 */

public enum SoulTrader {

    TEST_TRADE_GU_IMMORTAL("test_trade_gu_immortal", () -> List.of(
            SoulTradeOffer.of(new ItemStack(Items.PORKCHOP, 64), new ItemCost(ModItems.PRIMEVAL_STONE, 10)),
            SoulTradeOffer.of(new ItemStack(ModItems.WHITE_BOAR_GU.get()), new ItemCost(Items.DIRT, 192))));

    private final String id;
    private final Supplier<List<SoulTradeOffer>> offers;

    SoulTrader(String id, Supplier<List<SoulTradeOffer>> offers) {
        this.id = id;
        this.offers = offers;
    }

    public String id() { return this.id; }

    public List<SoulTradeOffer> offers() { return this.offers.get(); }
}
