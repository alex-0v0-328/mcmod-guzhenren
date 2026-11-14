package net.alex.guzhenren.registry.item;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 物品标签，多为补食料，按所喂蛊族命名（{@code boar_feed} 等）。
 *
 * <ul>
 *   <li>数据包可改成员，键名才是契约；region 顺序随 {@code GuPath}，同 {@code ModItems}。</li>
 *   <li>{@link #INGOTS_STEEL}、{@link #STORAGE_BLOCKS_STEEL} 是 {@code c:} 约定标签，含他模组的钢，
 *       同样喂钢筋蛊[Steel Tendon Gu]。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModItemTags {

    private ModItemTags() {}

    //region Rule Path [律道]
    public static final TagKey<Item> AMPLIFY_EFFECT_FEED = key("amplify_effect_feed");
    //endregion

    //region Metal Path [金道]
    public static final TagKey<Item> BRONZE_SKIN_FEED = key("bronze_skin_feed");
    public static final TagKey<Item> IRON_SKIN_FEED = key("iron_skin_feed");
    public static final TagKey<Item> STEEL_TENDON_FEED = key("steel_tendon_feed");
    public static final TagKey<Item> IRON_BONE_FEED = key("iron_bone_feed");
    //endregion

    //region Water Path [水道]
    public static final TagKey<Item> SMOOTH_STONE_FEED = key("smooth_stone_feed");
    //endregion

    //region Earth Path [土道]
    public static final TagKey<Item> JADE_SKIN_FEED = key("jade_skin_feed");
    public static final TagKey<Item> WHITE_JADE_FEED = key("white_jade_feed");
    //endregion

    //region Light Path [光道]
    public static final TagKey<Item> LITTLE_LIGHT_FEED = key("little_light_feed");
    //endregion

    //region Moon Path [月道]
    public static final TagKey<Item> MOONLIGHT_FEED = key("moonlight_feed");
    //endregion

    //region Strength Path [力道]
    public static final TagKey<Item> BOAR_FEED = key("boar_feed");
    public static final TagKey<Item> BEAR_FEED = key("bear_feed");
    public static final TagKey<Item> TIGER_FEED = key("tiger_feed");
    public static final TagKey<Item> TURTLE_FEED = key("turtle_feed");
    public static final TagKey<Item> HORSE_FEED = key("horse_feed");
    public static final TagKey<Item> OX_FEED = key("ox_feed");
    public static final TagKey<Item> BEEF_FEED = key("beef_feed");
    public static final TagKey<Item> IRON_FEED = key("iron_feed");
    public static final TagKey<Item> IRON_FEED_SMELTED = key("iron_feed_smelted");
    public static final TagKey<Item> ANVIL_FEED = key("anvil_feed");
    public static final TagKey<Item> COBBLESTONE_FEED = key("cobblestone_feed");
    public static final TagKey<Item> POTATO_FEED = key("potato_feed");
    public static final TagKey<Item> ALL_OUT_FEED = key("all_out_feed");
    //endregion

    //region Wisdom Path [智道]
    public static final TagKey<Item> CASUAL_FEED = key("casual_feed");
    public static final TagKey<Item> MALICIOUS_THOUGHT_FEED = key("malicious_thought_feed");
    //endregion

    //region Blood Path [血道]
    public static final TagKey<Item> BLOOD_MOON_FEED = key("blood_moon_feed");
    //endregion

    //region Food Path [食道]
    public static final TagKey<Item> LIQUOR_FEED = key("liquor_feed");
    //endregion

    //region Transformation Path [变化道]
    public static final TagKey<Item> ZOMBIE_FEED = key("zombie_feed");
    //endregion

    //region Common tags [通用标签]
    public static final TagKey<Item> INGOTS_STEEL = common("ingots/steel");
    public static final TagKey<Item> STORAGE_BLOCKS_STEEL = common("storage_blocks/steel");
    //endregion

    private static TagKey<Item> key(String name) {
        return TagKey.create(Registries.ITEM, Guzhenren.id(name));
    }

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
