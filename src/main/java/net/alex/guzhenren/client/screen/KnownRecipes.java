package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.mind.GuRecipeKnowledgeData;
import net.alex.guzhenren.refinement.GuRecipeItem;
import net.alex.guzhenren.refinement.GuRecipeKnowledge;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.alex.guzhenren.refinement.recipe.RefinementRecipe;
import net.alex.guzhenren.registry.item.ModCreativeTabs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 客户端蛊方缓存：将排序后的配方列表缓存起来以避免逐帧重排，并给出本人已学会的子集。
 * <ul>
 *   <li>{@link RefinementRecipe#getSorted} 每次都会按 ID 重新排序全部配方，若不缓存，炼蛊界面与蛊方选择器每帧都会承担
 *       该开销。</li>
 *   <li>当客户端 {@link RecipeManager} 实例更换时自动重建，并在收到 {@link RecipesUpdatedEvent}（数据包重载更新配方而
 *       不更换管理器实例）或玩家登出时失效丢弃；已学会子集另按同步来的 {@link GuRecipeKnowledgeData} 实例重算。</li>
 *   <li>{@link #get} 是全量表，下标与服务端菜单的选中索引一致；{@link #getLearned} 只列已学会的，选择器用它显示，再经
 *       {@link Learned#toGlobal} 换回全量下标发包。</li>
 *   <li>同时给蛊方物品（{@link GuRecipeItem}）供名称与创造栏用的客户端查表，并在已学会的蛊方提示里加绿字「已学
 *       会」。</li>
 *   <li>仅限客户端线程访问；可变静态字段构成客户端唯一的蛊方缓存及其来源，重载或登出时清空并在下次读取时重新填充。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class KnownRecipes {

    private KnownRecipes() {}

    private static final String TOOLTIP_LEARNED = "guzhenren.item.gu_recipe.known";
    private static @Nullable RecipeManager source;
    private static List<RecipeHolder<RefinementRecipe>> sorted = List.of();
    private static List<GuRecipe> cached = List.of();
    private static @Nullable GuRecipeKnowledgeData learnedFrom;
    private static Learned learned = Learned.NONE;

    static List<GuRecipe> get() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return List.of();

        RecipeManager recipes = level.getRecipeManager();
        if (recipes != source) {
            sorted = RefinementRecipe.getSorted(recipes);
            cached = sorted.stream().map(held -> held.value().gu()).toList();
            source = recipes;
            learnedFrom = null;
        }
        return cached;
    }

    static Learned getLearned() {
        LocalPlayer player = Minecraft.getInstance().player;
        get();
        if (player == null) return Learned.NONE;

        GuRecipeKnowledgeData knowledge = PlayerData.get(player).getGuRecipeKnowledge();
        if (knowledge != learnedFrom) {
            List<GuRecipe> recipes = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < sorted.size(); i++) {
                if (!knowledge.knows(sorted.get(i).id().toString())) continue;
                recipes.add(sorted.get(i).value().gu());
                indices.add(i);
            }
            learned = new Learned(List.copyOf(recipes), List.copyOf(indices));
            learnedFrom = knowledge;
        }
        return learned;
    }

    public static Optional<GuRecipe> find(ResourceLocation id) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? Optional.empty() : GuRecipeKnowledge.find(level.getRecipeManager(), id);
    }

    public static List<ResourceLocation> getIds() {
        get();
        return sorted.stream().map(RecipeHolder::id).toList();
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ResourceLocation id = GuRecipeItem.getRecipeId(event.getItemStack());
        if (id == null || event.getEntity() == null || !GuRecipeKnowledge.knows(event.getEntity(), id)) return;
        event.getToolTip().add(Component.translatable(TOOLTIP_LEARNED).withStyle(ChatFormatting.GREEN));
    }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        forget();
        rebuildRecipeShelf();
    }

    // the vanilla tab cache only rebuilds when its parameters change, never on a recipe reload
    private static void rebuildRecipeShelf() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        boolean operator = minecraft.options.operatorItemsTab().get() && minecraft.player.canUseGameMasterBlocks();
        ModCreativeTabs.BOOKLET.get().buildContents(new CreativeModeTab.ItemDisplayParameters(
                minecraft.level.enabledFeatures(), operator, minecraft.level.registryAccess()));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) { forget(); }

    private static void forget() {
        source = null;
        sorted = List.of();
        cached = List.of();
        learnedFrom = null;
        learned = Learned.NONE;
    }

    /** 已学会的蛊方及其在全量表中的下标，两列表一一平行。 */
    record Learned(List<GuRecipe> recipes, List<Integer> indices) {

        static final Learned NONE = new Learned(List.of(), List.of());

        int toLocal(int global) { return indices.indexOf(global); }

        int toGlobal(int local) { return local >= 0 && local < indices.size() ? indices.get(local) : -1; }
    }
}
