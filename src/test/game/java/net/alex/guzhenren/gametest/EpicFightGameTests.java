package net.alex.guzhenren.gametest;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.compat.EpicFightRuleSeed;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.registry.entries.EpicFightItems;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.gamerule.EpicFightGameRules;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * Epic Fight 兼容：{@code keepSkills} 预置、野生蛊目标选择、僵尸的耐力豁免、技能书掉落。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EpicFightGameTests {

    private EpicFightGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void epicFightKeepSkillsIsSeededOnlyOnce(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        boolean was = EpicFightGameRules.KEEP_SKILLS.getRuleValue(server.overworld());
        try {
            EpicFightGameRules.KEEP_SKILLS.setRuleValue(server.overworld(), false);
            EpicFightRuleSeed.seed(server);
            helper.assertTrue(!EpicFightGameRules.KEEP_SKILLS.getRuleValue(server.overworld()),
                    "a restart re-seeded keepSkills over the server's choice");
        } finally {
            EpicFightGameRules.KEEP_SKILLS.setRuleValue(server.overworld(), was);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void epicFightDoesNotTargetWildGu(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        helper.assertTrue(patch != null, "Epic Fight player patch missing");
        helper.assertTrue(GameTestEpicFight.targetCanceled(patch,
                helper.spawn(ModEntityTypes.HOPE_GU_ENTITY.get(), CENTER_POS)), "Hope Gu target was not canceled");
        helper.assertTrue(GameTestEpicFight.targetCanceled(patch,
                        helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), CENTER_POS)),
                "boar Gu target was not canceled");
        helper.assertTrue(!GameTestEpicFight.targetCanceled(patch, helper.spawn(EntityType.PIG, CENTER_POS)),
                "ordinary mob target was canceled");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void epicFightOnlyWaivesUndeadStamina(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        helper.assertTrue(patch != null, "Epic Fight player patch missing");
        helper.assertValueEqual(skillCost(patch, Skill.Resource.STAMINA), 5.0F,
                "living stamina cost changed");
        BodyService.addPhysique(player, Physique.TEMP_ZOMBIE);
        helper.assertValueEqual(skillCost(patch, Skill.Resource.STAMINA), 0.0F,
                "temp zombie still pays stamina");
        BodyService.addPhysique(player, Physique.ZOMBIE);
        helper.assertValueEqual(skillCost(patch, Skill.Resource.STAMINA), 0.0F,
                "zombie still pays stamina");
        helper.assertValueEqual(skillCost(patch, Skill.Resource.HEALTH), 5.0F,
                "non-stamina cost was waived");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void epicFightSkillBooksLeaveLoot(GameTestHelper helper) {
        LootContext context = new LootContext.Builder(new LootParams.Builder(helper.getLevel())
                .create(LootContextParamSets.EMPTY)).create(Optional.empty());
        ObjectArrayList<ItemStack> loot = CommonHooks.modifyLoot(BuiltInLootTables.DESERT_PYRAMID.location(),
                ObjectArrayList.of(new ItemStack(EpicFightItems.SKILLBOOK.get()), new ItemStack(Items.STICK)), context);
        helper.assertTrue(loot.stream().noneMatch(stack -> stack.is(EpicFightItems.SKILLBOOK)),
                "a skill book survived the loot modifiers");
        helper.assertTrue(loot.stream().anyMatch(stack -> stack.is(Items.STICK)), "the rest of the loot was stripped");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void epicFightSkillBooksLeaveMobDropsOnly(GameTestHelper helper) {
        helper.assertValueEqual(droppedSkillBooks(helper, helper.spawn(EntityType.ZOMBIE, CENTER_POS)), 0,
                "a mob dropped a skill book");
        helper.assertValueEqual(droppedSkillBooks(helper, mortal(helper)), 1,
                "a player's death drops lost its skill book");
        helper.succeed();
    }

    private static float skillCost(ServerPlayerPatch patch, Skill.Resource resource) {
        return GameTestEpicFight.skillCost(patch, resource, 5.0F);
    }

    private static int droppedSkillBooks(GameTestHelper helper, LivingEntity entity) {
        List<ItemEntity> drops = new ArrayList<>(List.of(new ItemEntity(helper.getLevel(),
                entity.getX(), entity.getY(), entity.getZ(), new ItemStack(EpicFightItems.SKILLBOOK.get()))));
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(entity, helper.getLevel().damageSources().generic(), drops, true));
        return (int) drops.stream().filter(drop -> drop.getItem().is(EpicFightItems.SKILLBOOK)).count();
    }
}
