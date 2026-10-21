package net.alex.guzhenren.registry.item;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The item tags this mod declares, most of them the larders a Gu feeds from.
 *
 * <p>Tag-key holder (not a DeferredRegister): each {@link TagKey} is a feed larder named after the Gu
 * family it feeds ({@code boar_feed}, {@code liquor_feed}, ...). A datapack retunes membership; the key
 * is the contract.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModItemTags {

    private ModItemTags() {}

    public static final TagKey<Item> BOAR_FEED = key("boar_feed");
    public static final TagKey<Item> BEAR_FEED = key("bear_feed");
    public static final TagKey<Item> BEEF_FEED = key("beef_feed");
    public static final TagKey<Item> RABBIT_FEED = key("rabbit_feed");
    public static final TagKey<Item> ANVIL_FEED = key("anvil_feed");
    public static final TagKey<Item> COBBLESTONE_FEED = key("cobblestone_feed");
    public static final TagKey<Item> POTATO_FEED = key("potato_feed");
    public static final TagKey<Item> JIN_FEED = key("jin_feed");
    public static final TagKey<Item> JIN_FEED_SMELTED = key("jin_feed_smelted");
    public static final TagKey<Item> LIQUOR_FEED = key("liquor_feed");
    public static final TagKey<Item> ALL_OUT_FEED = key("all_out_feed");
    public static final TagKey<Item> ZOMBIE_FEED = key("zombie_feed");
    public static final TagKey<Item> MALICIOUS_THOUGHT_FEED = key("malicious_thought_feed");
    public static final TagKey<Item> CASUAL_FEED = key("casual_feed");

    private static TagKey<Item> key(String name) {
        return TagKey.create(Registries.ITEM, Guzhenren.id(name));
    }
}
