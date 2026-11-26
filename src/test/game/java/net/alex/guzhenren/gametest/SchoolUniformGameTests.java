package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.armor.SchoolUniformDyeRecipe;
import net.alex.guzhenren.item.armor.UniformColor;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 学堂制服[School Uniform]的 GameTest：覆盖皮革对齐数值、工作台 16 色重映射换色（含保留组件、防同色误染、靴子禁
 * 染）与水炼药锅洗回默认玄墨炭黑。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SchoolUniformGameTests {

    private SchoolUniformGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void schoolUniformStatsMatchLeatherBaseline(GameTestHelper helper) {
        var chest = ModItems.SCHOOL_UNIFORM_CHESTPLATE.get();
        var legs = ModItems.SCHOOL_UNIFORM_LEGGINGS.get();
        var boots = ModItems.SCHOOL_UNIFORM_BOOTS.get();

        assertEquals(EquipmentSlot.CHEST, chest.getEquipmentSlot());
        assertEquals(EquipmentSlot.LEGS, legs.getEquipmentSlot());
        assertEquals(EquipmentSlot.FEET, boots.getEquipmentSlot());

        assertEquals(3, chest.getDefense());
        assertEquals(2, legs.getDefense());
        assertEquals(1, boots.getDefense());

        assertEquals(80, new ItemStack(chest).getMaxDamage());
        assertEquals(75, new ItemStack(legs).getMaxDamage());
        assertEquals(65, new ItemStack(boots).getMaxDamage());

        assertTrue(chest.isDyeable(), "chestplate must be dyeable");
        assertTrue(legs.isDyeable(), "leggings must be dyeable");
        assertFalse(boots.isDyeable(), "boots must not be dyeable");
        assertTrue(chest.isValidRepairItem(new ItemStack(chest), new ItemStack(Items.WHITE_WOOL)),
                "wool must repair school uniform");

        // Epic Fight's WearableItemLayer iterates ArmorMaterial.layers(); both dyeable and base layers must exist.
        var layers = chest.getMaterial().value().layers();
        assertEquals(2, layers.size());
        assertTrue(layers.get(0).dyeable(), "layer 0 must be dyeable");
        assertFalse(layers.get(1).dyeable(), "layer 1 must be fixed-color base detail");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void dyeRecipeSwitchesColorAndPreservesComponents(GameTestHelper helper) {
        SchoolUniformDyeRecipe recipe = new SchoolUniformDyeRecipe(CraftingBookCategory.EQUIPMENT);

        ItemStack chest = new ItemStack(ModItems.SCHOOL_UNIFORM_CHESTPLATE.get());
        chest.setDamageValue(12);
        chest.set(DataComponents.CUSTOM_NAME, Component.literal("学堂旧衣"));
        assertEquals(DyeColor.BLACK, UniformColor.getColor(chest));

        // Dyeing with the current color (black) must be rejected so players do not waste dye.
        CraftingInput sameColor = CraftingInput.of(2, 1, List.of(chest.copy(), new ItemStack(Items.BLACK_DYE)));
        assertFalse(recipe.matches(sameColor, helper.getLevel()), "same-color dye should not match");

        // Dyeing blue succeeds and preserves damage + custom name.
        CraftingInput toBlue = CraftingInput.of(2, 1, List.of(chest.copy(), new ItemStack(Items.BLUE_DYE)));
        assertTrue(recipe.matches(toBlue, helper.getLevel()), "blue dye should match");
        ItemStack dyed = recipe.assemble(toBlue, helper.getLevel().registryAccess());
        assertEquals(DyeColor.BLUE, UniformColor.getColor(dyed));
        assertEquals(12, dyed.getDamageValue());
        assertEquals(Component.literal("学堂旧衣"), dyed.get(DataComponents.CUSTOM_NAME));

        // Dyeing back to default black clears the component cleanly.
        CraftingInput backToBlack = CraftingInput.of(2, 1, List.of(dyed.copy(), new ItemStack(Items.BLACK_DYE)));
        assertTrue(recipe.matches(backToBlack, helper.getLevel()), "black dye should match non-default uniform");
        ItemStack reset = recipe.assemble(backToBlack, helper.getLevel().registryAccess());
        assertEquals(DyeColor.BLACK, UniformColor.getColor(reset));
        assertNull(reset.get(ModDataComponents.UNIFORM_COLOR.get()));

        // Boots are fixed color and cannot be dyed.
        CraftingInput bootsInput = CraftingInput.of(2, 1,
                List.of(new ItemStack(ModItems.SCHOOL_UNIFORM_BOOTS.get()), new ItemStack(Items.BLUE_DYE)));
        assertFalse(recipe.matches(bootsInput, helper.getLevel()), "boots must not match dye recipe");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void waterCauldronWashesUniformBackToDefaultBlack(GameTestHelper helper) {
        BlockPos relPos = new BlockPos(4, 2, 4);
        BlockPos absPos = helper.absolutePos(relPos);
        BlockState cauldron = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
        helper.setBlock(relPos, cauldron);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack leggings = new ItemStack(ModItems.SCHOOL_UNIFORM_LEGGINGS.get());
        UniformColor.setColor(leggings, DyeColor.PURPLE);
        player.setItemInHand(InteractionHand.MAIN_HAND, leggings);

        CauldronInteraction interaction = CauldronInteraction.WATER.map().get(ModItems.SCHOOL_UNIFORM_LEGGINGS.get());
        ItemInteractionResult washed = interaction.interact(
                cauldron, helper.getLevel(), absPos, player, InteractionHand.MAIN_HAND, leggings);
        assertTrue(washed.consumesAction(), "washing dyed uniform should succeed");
        assertEquals(DyeColor.BLACK, UniformColor.getColor(leggings));
        assertEquals(2, (int) helper.getBlockState(relPos).getValue(LayeredCauldronBlock.LEVEL));

        // Washing an already-default uniform passes without lowering water level again.
        ItemInteractionResult noop = interaction.interact(
                helper.getBlockState(relPos), helper.getLevel(), absPos, player, InteractionHand.MAIN_HAND, leggings);
        assertFalse(noop.consumesAction(), "washing default uniform should pass");
        assertEquals(2, (int) helper.getBlockState(relPos).getValue(LayeredCauldronBlock.LEVEL));
        helper.succeed();
    }
}
