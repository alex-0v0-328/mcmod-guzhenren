package net.alex.guzhenren.datagen.item;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.builder.ItemRegistrar;
import net.alex.guzhenren.registry.item.ModItemTags;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Writes the item tags, including the larders a Gu feeds from.
 *
 * <p>Extends {@link net.minecraft.data.tags.ItemTagsProvider}. Defines the feed tags ({@code boar_feed},
 * {@code bear_feed}, {@code jin_feed}, {@code liquor_feed}, etc.), the zombie feed tag, and the
 * all-out-effort feed tag, files the steel under the common {@code c:ingots/steel} and
 * {@code c:storage_blocks/steel} tags the Steel Tendon Gu eats from, and hides the Advancement Icon [进度图标]
 * from recipe viewers through the common {@code c:hidden_from_recipe_viewers} tag. Every tag references the
 * registered item or a vanilla item, never a raw id. An item declared through {@link ItemRegistrar} names its own
 * tags on its registration chain; those are written first, each tag's members in registration order.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class ModItemTagsProvider extends ItemTagsProvider {

    // The loader's convention tags other mods' steel joins; only datagen names them, so the keys live here.
    private static final TagKey<Item> INGOTS_STEEL =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/steel"));
    private static final TagKey<Item> STORAGE_BLOCKS_STEEL =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/steel"));

    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()),
                Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        for (ItemRegistrar.Entry entry : ItemRegistrar.of(ModItems.ITEMS).getEntries()) {
            for (TagKey<Item> tag : entry.tags()) tag(tag).add(entry.item().get());
        }

        tag(ModItemTags.BOAR_FEED).add(Items.PORKCHOP);
        tag(ModItemTags.BEAR_FEED).add(Items.HONEY_BOTTLE);
        tag(ModItemTags.BEEF_FEED).add(Items.BEEF);
        tag(ModItemTags.RABBIT_FEED).add(Items.RABBIT);
        tag(ModItemTags.ANVIL_FEED).add(Items.ANVIL);
        tag(ModItemTags.COBBLESTONE_FEED).add(Items.COBBLESTONE);
        tag(ModItemTags.POTATO_FEED).add(Items.POTATO);
        tag(ModItemTags.SMOOTH_STONE_FEED).add(Items.SMOOTH_STONE);
        tag(ModItemTags.JADE_SKIN_FEED).add(ModItems.JADE.get(), ModItems.JADEITE.get());
        tag(ModItemTags.WHITE_JADE_FEED).add(ModItems.JADE.get(), ModItems.JADEITE.get());
        tag(ModItemTags.BRONZE_SKIN_FEED).add(Items.COPPER_INGOT);
        tag(ModItemTags.IRON_SKIN_FEED).add(Items.IRON_INGOT);
        tag(ModItemTags.STEEL_TENDON_FEED).addTag(INGOTS_STEEL);
        tag(ModItemTags.IRON_BONE_FEED).add(Items.IRON_INGOT);

        tag(INGOTS_STEEL).add(ModItems.STEEL_INGOT.get());
        tag(Tags.Items.INGOTS).addTag(INGOTS_STEEL);
        tag(STORAGE_BLOCKS_STEEL).add(ModItems.STEEL_BLOCK.get());
        tag(Tags.Items.STORAGE_BLOCKS).addTag(STORAGE_BLOCKS_STEEL);

        tag(ModItemTags.JIN_FEED).add(Items.RAW_IRON);
        tag(ModItemTags.JIN_FEED_SMELTED).add(Items.IRON_INGOT);

        tag(ModItemTags.LIQUOR_FEED).add(ModItems.LIQUOR.get(), ModItems.SOUR_LIQUOR.get(),
                ModItems.SWEET_LIQUOR.get(), ModItems.BITTER_LIQUOR.get(), ModItems.SPICY_LIQUOR.get());

        tag(ModItemTags.ALL_OUT_FEED).add(Items.STONE);

        tag(ModItemTags.ZOMBIE_FEED).add(Items.ROTTEN_FLESH);

        //  TODO(placeholder): dirt is a placeholder food for the Malicious Thought Gu, pending Alex's real food.
        tag(ModItemTags.MALICIOUS_THOUGHT_FEED).add(Items.DIRT);

        tag(ModItemTags.CASUAL_FEED).add(Items.WHEAT_SEEDS);

        //  TODO(placeholder): smooth stone is a placeholder food for the Amplify Effect Gu, pending Alex's real food.
        tag(ModItemTags.AMPLIFY_EFFECT_FEED).add(Items.SMOOTH_STONE);

        tag(Tags.Items.HIDDEN_FROM_RECIPE_VIEWERS).add(ModItems.ADVANCEMENT_ICON.get());
    }
}
