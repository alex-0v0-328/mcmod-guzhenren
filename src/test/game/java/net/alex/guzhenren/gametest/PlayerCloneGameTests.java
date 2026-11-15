package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.body.Physique;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.awakenedMortal;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;

/**
 * 玩家数据跨克隆、重生、登录测试：生命上限、空窍[Aperture]、脑海、空窍存储、死亡重置。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlayerCloneGameTests {

    private PlayerCloneGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void maxHealthSurvivesANonDeathClone(GameTestHelper helper) {
        ServerPlayer from = rankThreePlayer(helper);
        from.setHealth(55.0F);
        ServerPlayer to = mortal(helper);
        to.setHealth(20.0F);

        cloneOnto(from, to, false, false);

        helper.assertValueEqual(to.getMaxHealth(), 60.0F, "max health after the clone");
        helper.assertValueEqual(to.getHealth(), 55.0F, "health carried across the End exit");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void maxHealthFillsOnAKeepInventoryRespawn(GameTestHelper helper) {
        ServerPlayer from = rankThreePlayer(helper);
        from.setHealth(5.0F);
        ServerPlayer to = mortal(helper);
        to.setHealth(20.0F);

        cloneOnto(from, to, true, true);

        helper.assertValueEqual(to.getHealth(), 60.0F, "a respawn starts at the rank's full health");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void maxHealthSurvivesARelog(GameTestHelper helper) {
        ServerPlayer player = rankThreePlayer(helper);
        player.setHealth(55.0F);
        PlayerDataService.onLogout(player);
        player.setHealth(20.0F);

        PlayerDataService.onJoin(player);

        helper.assertValueEqual(player.getHealth(), 55.0F,
                "health above 20 restored after the load clamp");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void deathCloneKeepsDataAndCopiesStorage(GameTestHelper helper) {
        ServerPlayer from = awakenedMortal(helper);
        PlayerDataService.onBirth(from);
        BodyService.setExtremePhysique(from, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL);
        BodyService.addPhysique(from, Physique.ZOMBIE);
        ApertureStorageService.set(from, 0, List.of(new ItemStack(ModItems.SECOND_APERTURE_GU_1.get())));
        ServerPlayer to = mortal(helper);
        cloneOnto(from, to, true, true);
        PlayerDataService.onRespawn(to);
        helper.assertValueEqual(ApertureService.get(to), ApertureService.get(from),
                "death with keepInventory lost apertures");
        helper.assertValueEqual(PlayerData.get(to).getMind().brilliance(),
                PlayerData.get(from).getMind().brilliance(),
                "retained death rerolled brilliance");
        helper.assertTrue(PlayerData.get(to).isBorn(), "clone lost birth latch");
        helper.assertTrue(BodyService.isExtreme(to) && !BodyService.isUndead(to),
                "revival lost extreme or kept undead");
        ApertureStorageService.getItems(to, 0).getFirst().shrink(1);
        helper.assertValueEqual(ApertureStorageService.getItems(from, 0).getFirst().getCount(), 1,
                "cloned storage shares mutable stacks with original");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void deathResetClearsExtremeAndNourishment(GameTestHelper helper) {
        ServerPlayer from = awakenedMortal(helper);
        BodyService.setExtremePhysique(from, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL);
        ApertureNourishService.start(from, 0);
        ServerPlayer to = mortal(helper);
        cloneOnto(from, to, true, false);
        PlayerDataService.onRespawn(to);
        helper.assertTrue(!ApertureService.hasAperture(to), "reset retained apertures");
        helper.assertTrue(!BodyService.isExtreme(to), "reset retained extreme physique");
        helper.assertTrue(!ApertureNourishService.isCultivating(to), "reset retained nourishment");
        helper.assertTrue(PlayerData.get(to).isBorn(), "reset did not initialize newborn");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void nonDeathCloneIgnoresKeepInventory(GameTestHelper helper) {
        ServerPlayer from = awakenedMortal(helper);
        ApertureService.openSecond(from, Rank.THREE);
        PlayerDataService.onBirth(from);
        ServerPlayer to = mortal(helper);
        cloneOnto(from, to, false, false);
        helper.assertValueEqual(ApertureService.get(to), ApertureService.get(from),
                "dimension clone lost apertures");
        helper.assertValueEqual(PlayerData.get(to).getMind(), PlayerData.get(from).getMind(),
                "dimension clone lost mind");
        helper.assertTrue(PlayerData.get(to).isBorn(), "dimension clone lost birth latch");
        helper.succeed();
    }

    private static ServerPlayer rankThreePlayer(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        ApertureService.setRank(player, ApertureData.PRIMARY, Rank.THREE);
        return player;
    }

    // The loader copies the inherited player data before PlayerDataEvents.onClone runs at LOW.
    private static void cloneOnto(ServerPlayer from, ServerPlayer to, boolean wasDeath, boolean keepInventory) {
        to.copyAttachmentsFrom(from, wasDeath);
        PlayerDataService.onClone(from, to, wasDeath, keepInventory);
    }
}
