package net.alex.guzhenren.refinement;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 蛊方物品：一种物品，组件 {@code recipe_id} 记它是哪张蛊方，名称显示为「蛊方 • 产物」。
 * <ul>
 *   <li>右键单点（{@link GuItem#TAP_TICKS} 内松开）在客户端打开蛊方图册；未学会时按住研读，满
 *       {@link GuRecipeKnowledge#getStudyTicks} 后服务端学会，进度条由客户端 {@code ChargeHud} 画；已学会则按下即开图
 *       册，不再研读。书永不消耗，研读不要求开窍。</li>
 *   <li>客户端回调经 {@link #readOnClient} 与 {@link #lookUpOnClient} 在客户端初始化时注入（同 {@code BookletItem}），
 *       隔离客户端类；专用服务端上二者保持空操作，名称经 {@link ServerLifecycleHooks} 查服务端配方。</li>
 *   <li>⚠ 物品名需要产物，产物要查配方表：服务端线程查服务器，其余线程查客户端世界；两边都查不到时只显示「蛊方」。
 *       </li>
 * </ul>
 */

public final class GuRecipeItem extends Item {

    private static final String NAMED = "item.guzhenren.gu_recipe.named";
    private static final String LEARNED = "guzhenren.item.gu_recipe.learned";
    private static final String MISSING = "guzhenren.item.failed.gu_recipe_missing";
    private static Consumer<ResourceLocation> reader = id -> {};
    private static Function<ResourceLocation, Optional<GuRecipe>> clientLookup = id -> Optional.empty();
    private static Supplier<List<ResourceLocation>> clientIds = List::of;

    public GuRecipeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void readOnClient(Consumer<ResourceLocation> reader) { GuRecipeItem.reader = reader; }

    public static void lookUpOnClient(Function<ResourceLocation, Optional<GuRecipe>> lookup,
                                      Supplier<List<ResourceLocation>> ids) {
        GuRecipeItem.clientLookup = lookup;
        GuRecipeItem.clientIds = ids;
    }

    /** 创造栏每张蛊方列一本：创造栏只在客户端构建，读客户端世界的配方表。 */
    public static List<ResourceLocation> listForCreativeTab() { return clientIds.get(); }

    public static ItemStack of(@NotNull Item item, @NotNull ResourceLocation recipe) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.RECIPE_ID.get(), recipe);
        return stack;
    }

    public static @Nullable ResourceLocation getRecipeId(@NotNull ItemStack stack) {
        return stack.get(ModDataComponents.RECIPE_ID.get());
    }

    //region read and study [阅读与研读]
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation id = getRecipeId(stack);
        if (id == null || GuRecipeKnowledge.find(level.getRecipeManager(), id).isEmpty()) {
            if (player instanceof ServerPlayer server) ActionBar.refuse(server, MISSING);
            return InteractionResultHolder.fail(stack);
        }
        if (GuRecipeKnowledge.knows(player, id)) {
            read(level, player, id);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity,
                             int remainingTicks) {
        ResourceLocation id = getRecipeId(stack);
        if (id == null || !(entity instanceof Player player)) return;
        if (getUseDuration(stack, entity) - remainingTicks > GuItem.TAP_TICKS) return;

        read(level, player, id);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                              @NotNull LivingEntity entity) {
        ResourceLocation id = getRecipeId(stack);
        if (id != null && entity instanceof ServerPlayer player && GuRecipeKnowledge.learn(player, id)) {
            ActionBar.inform(player, LEARNED, getResultName(stack));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return stack;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        ResourceLocation id = getRecipeId(stack);
        if (id == null) return 0;
        return GuRecipeKnowledge.find(entity.level().getRecipeManager(), id)
                .map(GuRecipeKnowledge::getStudyTicks).orElse(0);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) { return UseAnim.NONE; }

    private static void read(Level level, Player player, ResourceLocation id) {
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        if (level.isClientSide()) reader.accept(id);
    }
    //endregion

    //region name [名称]
    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        Component result = getResultName(stack);
        return result == null ? super.getName(stack) : Component.translatable(NAMED, result);
    }

    private static @Nullable Component getResultName(ItemStack stack) {
        ResourceLocation id = getRecipeId(stack);
        if (id == null) return null;
        return find(id).map(recipe -> recipe.getFirst().getHoverName()).orElse(null);
    }

    private static Optional<GuRecipe> find(ResourceLocation id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) return GuRecipeKnowledge.find(server.getRecipeManager(), id);
        return clientLookup.apply(id);
    }
    //endregion
}
