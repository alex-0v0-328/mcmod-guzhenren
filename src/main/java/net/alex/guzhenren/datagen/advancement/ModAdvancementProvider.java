package net.alex.guzhenren.datagen.advancement;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.item.AdvancementIconItem;
import net.alex.guzhenren.registry.advancement.ModCriteriaTriggers;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

/**
 * The advancement tree [进度]: a root per milestone family, generated under {@code advancement/}.
 * The tree opens with the first Hope Gu [希望蛊] ritual -- obtaining it and holding the ritual to the
 * end are two AND-ed criteria ({@code obtained_gu} + {@code used_gu}). The realm [境界] chain hangs below it,
 * one {@code reached_rank} criterion each: Rank II to Rank V, with Rank III Peak branching off Rank III. Each
 * realm shows its rank's bare numeral ({@link AdvancementIconItem}); Rank III Peak shows Rank III's peak variant.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.advancement.HopeGuUsedTrigger
 * @see net.alex.guzhenren.advancement.RankReachedTrigger
 * @since 1.0.0
 */

public class ModAdvancementProvider extends AdvancementProvider {

    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                  ExistingFileHelper existingFileHelper) {
        super(output, registries, existingFileHelper, List.of(new GuzhenrenAdvancements()));
    }

    private static class GuzhenrenAdvancements implements AdvancementGenerator {

        @Override
        public void generate(HolderLookup.@NotNull Provider registries,
                             @NotNull Consumer<AdvancementHolder> saver,
                             @NotNull ExistingFileHelper existingFileHelper) {
            AdvancementHolder awakening = Advancement.Builder.advancement()
                .display(new ItemStack(ModItems.HOPE_GU.get()),
                    Component.translatable("advancements.guzhenren.first_awakening.title"),
                    Component.translatable("advancements.guzhenren.first_awakening.description"),
                    ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                    AdvancementType.TASK, true, true, false)
                .addCriterion("obtained_gu", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.HOPE_GU.get()))
                .addCriterion("used_gu", ModCriteriaTriggers.USED_HOPE_GU.get().createCriterion())
                .requirements(AdvancementRequirements.Strategy.AND)
                .save(saver, Guzhenren.id("first_awakening"), existingFileHelper);

            AdvancementIconItem iconItem = ModItems.ADVANCEMENT_ICON.get();
            AdvancementHolder two = realm(saver, existingFileHelper, awakening, "rank_two",
                iconItem.icon(Rank.TWO, Stage.NONE), AdvancementType.GOAL, Rank.TWO, Stage.LOWEST);
            AdvancementHolder three = realm(saver, existingFileHelper, two, "rank_three",
                iconItem.icon(Rank.THREE, Stage.NONE), AdvancementType.GOAL, Rank.THREE, Stage.LOWEST);
            realm(saver, existingFileHelper, three, "rank_three_peak",
                iconItem.icon(Rank.THREE, Stage.PEAK), AdvancementType.TASK, Rank.THREE, Stage.PEAK);
            AdvancementHolder four = realm(saver, existingFileHelper, three, "rank_four",
                iconItem.icon(Rank.FOUR, Stage.NONE), AdvancementType.GOAL, Rank.FOUR, Stage.LOWEST);
            realm(saver, existingFileHelper, four, "rank_five",
                iconItem.icon(Rank.FIVE, Stage.NONE), AdvancementType.GOAL, Rank.FIVE, Stage.LOWEST);
        }

        private static AdvancementHolder realm(Consumer<AdvancementHolder> saver, ExistingFileHelper existingFileHelper,
                                               AdvancementHolder parent, String name, ItemStack icon,
                                               AdvancementType type, Rank rank, Stage stage) {
            return Advancement.Builder.advancement()
                .parent(parent)
                .display(icon,
                    Component.translatable("advancements.guzhenren." + name + ".title"),
                    Component.translatable("advancements.guzhenren." + name + ".description"),
                    null, type, true, true, false)
                .addCriterion("reached_rank", ModCriteriaTriggers.REACHED_RANK.get().createCriterion(rank, stage))
                .save(saver, Guzhenren.id(name), existingFileHelper);
        }
    }
}
