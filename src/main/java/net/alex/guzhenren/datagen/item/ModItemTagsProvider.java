package net.alex.guzhenren.datagen.item;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.builder.ItemRegistrar;
import net.alex.guzhenren.registry.item.ModItemTags;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 物品标签。
 *
 * <ul>
 *   <li>蛊食物标签（{@code boar_feed}、{@code bear_feed}、{@code jin_feed}、{@code liquor_feed} 等）、僵尸与全力以赴
 *       蛊[All-Out Effort Gu]食物标签：只引用已注册或原版物品，不写裸 id。</li>
 *   <li>钢筋蛊[Steel Tendon Gu]所食通用 {@code c:ingots/steel}、{@code c:storage_blocks/steel} 嵌进加载器
 *       {@code c:ingots}、{@code c:storage_blocks}。</li>
 *   <li>本模组物品在 {@link ItemRegistrar} 注册链声明自身标签（酒、玉、钢、对配方查看器隐藏的进度图标），先写，各标签内
 *       按注册顺序；原版成员随后写在此。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class ModItemTagsProvider extends ItemTagsProvider {

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
        tag(ModItemTags.TIGER_FEED).addTag(Tags.Items.FOODS_RAW_MEAT);
        tag(ModItemTags.TURTLE_FEED).add(Items.SEAGRASS);
        tag(ModItemTags.HORSE_FEED).add(Items.HAY_BLOCK);
        tag(ModItemTags.OX_FEED).add(Items.WHEAT);
        tag(ModItemTags.BEEF_FEED).add(Items.BEEF);
        tag(ModItemTags.ANVIL_FEED).add(Items.ANVIL);
        tag(ModItemTags.COBBLESTONE_FEED).add(Items.COBBLESTONE);
        tag(ModItemTags.POTATO_FEED).add(Items.POTATO);
        tag(ModItemTags.SMOOTH_STONE_FEED).add(Items.SMOOTH_STONE);
        tag(ModItemTags.BRONZE_SKIN_FEED).add(Items.COPPER_INGOT);
        tag(ModItemTags.IRON_SKIN_FEED).add(Items.IRON_INGOT);
        tag(ModItemTags.STEEL_TENDON_FEED).addTag(ModItemTags.INGOTS_STEEL);
        tag(ModItemTags.IRON_BONE_FEED).add(Items.IRON_INGOT);

        tag(Tags.Items.INGOTS).addTag(ModItemTags.INGOTS_STEEL);
        tag(Tags.Items.STORAGE_BLOCKS).addTag(ModItemTags.STORAGE_BLOCKS_STEEL);

        tag(ModItemTags.IRON_FEED).add(Items.RAW_IRON);
        tag(ModItemTags.IRON_FEED_SMELTED).add(Items.IRON_INGOT);

        tag(ModItemTags.ALL_OUT_FEED).add(Items.STONE);

        tag(ModItemTags.ZOMBIE_FEED).add(Items.ROTTEN_FLESH);

        //  TODO(placeholder): dirt is a placeholder food for the Malicious Thought Gu, pending Alex's real food.
        tag(ModItemTags.MALICIOUS_THOUGHT_FEED).add(Items.DIRT);

        tag(ModItemTags.CASUAL_FEED).add(Items.WHEAT_SEEDS);

        //  TODO(placeholder): smooth stone is a placeholder food for the Amplify Effect Gu, pending Alex's real food.
        tag(ModItemTags.AMPLIFY_EFFECT_FEED).add(Items.SMOOTH_STONE);

        tag(ModItemTags.MOONLIGHT_FEED).add(ModItems.MOON_ORCHID_PETALS.get());
        tag(ModItemTags.BLOOD_MOON_FEED).add(ModItems.BLOOD_BOTTLE.get());
        tag(ModItemTags.LITTLE_LIGHT_FEED).add(Items.GLOWSTONE_DUST);
    }
}
