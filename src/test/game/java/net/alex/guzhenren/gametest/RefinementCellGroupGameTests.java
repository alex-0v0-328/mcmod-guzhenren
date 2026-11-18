package net.alex.guzhenren.gametest;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.refinement.RefinementMenu;
import net.alex.guzhenren.refinement.recipe.GuIngredient;
import net.alex.guzhenren.refinement.recipe.GuRecipe;
import net.alex.guzhenren.refinement.recipe.GuRecipeInput;
import net.alex.guzhenren.refinement.recipe.RefinementRecipe;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.registry.recipe.ModRecipes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 蛊方格组测试，以白玉蛊[White Jade Gu]蛊方为例。白豕蛊[White Boar Gu]固定中心，一只玉皮蛊[Jade Skin Gu]放其余任一内圈
 * 格；覆盖按已加载蛊方匹配、自动填入格组首格、网络编解码、重叠防护。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RefinementCellGroupGameTests {

    private RefinementCellGroupGameTests() {}

    private static final int CORE_CENTER = GuRecipe.getSlotAt(2, 2);
    private static final int CORE_TOP_LEFT = GuRecipe.getSlotAt(1, 1);

    @GameTest(template = "empty9x9x9")
    public static void jadeSkinFitsEveryInnerCellButTheCenter(GameTestHelper helper) {
        GuRecipe recipe = whiteJade(helper);

        for (int slot = GuRecipe.RING_SIZE; slot < GuRecipe.INPUT_SIZE; slot++) {
            if (slot == CORE_CENTER) continue;

            int cell = slot;
            int[] taken = recipe.claim(grid(ModItems.JADE_SKIN_GU_1.get(), cell));
            assertNotNull(taken, () -> "jade skin refused in inner cell " + cell);
            assertEquals(1, taken[cell]);
            assertEquals(1, taken[CORE_CENTER]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void whiteJadeRefusesAMisplacedOrSurplusInput(GameTestHelper helper) {
        GuRecipe recipe = whiteJade(helper);

        List<ItemStack> swapped = empty();
        swapped.set(CORE_CENTER, new ItemStack(ModItems.JADE_SKIN_GU_1.get()));
        swapped.set(CORE_TOP_LEFT, new ItemStack(ModItems.WHITE_BOAR_GU_1.get()));
        assertNull(recipe.claim(new GuRecipeInput(swapped)));

        List<ItemStack> twoJade = cells(ModItems.JADE_SKIN_GU_1.get(), CORE_TOP_LEFT);
        twoJade.set(CORE_TOP_LEFT + 1, new ItemStack(ModItems.JADE_SKIN_GU_1.get()));
        assertNull(recipe.claim(new GuRecipeInput(twoJade)));

        List<ItemStack> ringJunk = cells(ModItems.JADE_SKIN_GU_1.get(), CORE_TOP_LEFT);
        ringJunk.set(0, new ItemStack(ModItems.PRIMEVAL_STONE.get()));
        assertNull(recipe.claim(new GuRecipeInput(ringJunk)));

        List<ItemStack> noJade = empty();
        noJade.set(CORE_CENTER, new ItemStack(ModItems.WHITE_BOAR_GU_1.get()));
        assertNull(recipe.claim(new GuRecipeInput(noJade)));
        assertEquals(1, recipe.getShortfall(new GuRecipeInput(noJade))[jadeIndex(recipe)]);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void autofillPutsTheJadeSkinInItsGroupsFirstCell(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-cell-group-gametest").at(CENTER);
        player.getInventory().setItem(9, new ItemStack(ModItems.JADE_SKIN_GU_1.get()));
        player.getInventory().setItem(10, new ItemStack(ModItems.WHITE_BOAR_GU_1.get()));
        GameTestRecipes.learnAll(player);
        RefinementMenu menu = new RefinementMenu(1, player.getInventory());

        menu.clickMenuButton(player, RefinementMenu.BUTTON_RECIPE_BASE + whiteJadeIndex(helper));

        assertTrue(menu.getSlot(CORE_TOP_LEFT).getItem().is(ModItems.JADE_SKIN_GU_1.get()),
                "jade skin not in the first inner cell");
        assertTrue(menu.getSlot(CORE_CENTER).getItem().is(ModItems.WHITE_BOAR_GU_1.get()),
                "white boar not in the center");
        assertNotNull(whiteJade(helper).claim(menu.getGrid()), () -> "the autofilled grid does not match");
        menu.removed(player);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void cellGroupsSurviveTheNetworkCodec(GameTestHelper helper) {
        GuRecipe recipe = whiteJade(helper);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess(), ConnectionType.NEOFORGE);

        ModRecipes.REFINEMENT_SERIALIZER.get().streamCodec().encode(buf, new RefinementRecipe(recipe));
        GuRecipe decoded = ModRecipes.REFINEMENT_SERIALIZER.get().streamCodec().decode(buf).gu();

        assertTrue(recipe.slots().equals(decoded.slots()), () -> "cell groups changed: " + decoded.slots());
        assertEquals(8, recipe.slots().get(jadeIndex(recipe)).size());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void overlappingCellGroupsAreRefused(GameTestHelper helper) {
        List<GuIngredient> needs = List.of(GuIngredient.of(Items.STONE, 1), GuIngredient.of(Items.DIRT, 1));
        boolean refused = false;
        try {
            new GuRecipe(needs, List.of(List.of(0, 1), List.of(1)), List.of(), 0L, 0L, List.of(0), 0);
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        assertTrue(refused, "a cell shared by two groups was accepted");
        helper.succeed();
    }

    private static GuRecipe whiteJade(GameTestHelper helper) {
        return RefinementRecipe.getKnown(helper.getLevel().getServer().getRecipeManager()).get(whiteJadeIndex(helper));
    }

    private static int whiteJadeIndex(GameTestHelper helper) {
        List<GuRecipe> known = RefinementRecipe.getKnown(helper.getLevel().getServer().getRecipeManager());
        for (int i = 0; i < known.size(); i++) {
            if (known.get(i).getFirst().is(ModItems.WHITE_JADE_GU_2.get())) return i;
        }
        throw new IllegalStateException("white jade gu recipe missing");
    }

    private static int jadeIndex(GuRecipe recipe) {
        for (int n = 0; n < recipe.ingredients().size(); n++) {
            if (recipe.ingredients().get(n).test(new ItemStack(ModItems.JADE_SKIN_GU_1.get()))) return n;
        }
        throw new IllegalStateException("white jade gu recipe has no jade skin");
    }

    private static GuRecipeInput grid(Item jade, int jadeSlot) { return new GuRecipeInput(cells(jade, jadeSlot)); }

    private static List<ItemStack> cells(Item jade, int jadeSlot) {
        List<ItemStack> slots = empty();
        slots.set(CORE_CENTER, new ItemStack(ModItems.WHITE_BOAR_GU_1.get()));
        slots.set(jadeSlot, new ItemStack(jade));
        return slots;
    }

    private static List<ItemStack> empty() {
        return new ArrayList<>(Collections.nCopies(GuRecipe.INPUT_SIZE, ItemStack.EMPTY));
    }
}
