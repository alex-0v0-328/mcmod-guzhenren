package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.awakenedMortal;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;

/**
 * 体质写入即时生效测试。兽力[Beast Strength]与僵尸层级立即改攻击力；十绝天资掷出体质，十绝体拉满资质[Aptitude]基数。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhysiqueGameTests {

    private PhysiqueGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void attackFollowsStrengthAndZombieWritesAtOnce(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        PathStrengthService.grant(player, BeastStrength.BEAR);
        helper.assertValueEqual(player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                base + BeastStrength.BEAR.getPoints() * PathStrengthData.ATTACK_PER_POINT,
                "beast strength did not lift attack at once");
        PathStrengthService.revoke(player, BeastStrength.BEAR);
        helper.assertValueEqual(player.getAttributeValue(Attributes.ATTACK_DAMAGE), base,
                "revoked beast strength kept its attack");
        BodyService.turnZombie(player, 0);
        helper.assertValueEqual(player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                base + AttackDamageService.ZOMBIE_ATTACK_BASE, "zombie tier did not lift attack at once");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void tenExtremesTalentRollsAPhysique(GameTestHelper helper) {
        ServerPlayer set = awakenedMortal(helper);
        ApertureService.setTalent(set, ApertureData.PRIMARY, Talent.TEN_EXTREMES);
        assertRolledTenExtreme(helper, set, "talent set");

        ApertureService.setTalent(set, ApertureData.PRIMARY, Talent.SECOND);
        helper.assertTrue(!BodyService.isExtreme(set), "a lower talent kept the ten-extreme physique");
        helper.assertValueEqual(ApertureService.getTalent(set), Talent.SECOND,
                "a lower talent was forced back to the full base");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void extremePhysiqueResetsFirstApertureAtOnce(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);

        BodyService.setExtremePhysique(player, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL);
        helper.assertValueEqual(ApertureService.getAperture(player).baseEssence(), Aperture.MAX_BASE,
                "extreme physique left the base below full");
        helper.assertTrue(PathEffectService.getMarks(player, GuPath.STRENGTH).marks() > 0L,
                "extreme physique granted no talent marks");
        BodyService.setExtremePhysique(player, ExtremePhysique.NONE);
        helper.assertValueEqual(ApertureService.getAperture(player).baseEssence(),
                Aperture.MAX_BASE - 1, "cleared physique kept the full base");
        helper.assertTrue(PathEffectService.getMarks(player, GuPath.STRENGTH).isEmpty(),
                "cleared physique kept its talent marks");
        helper.succeed();
    }

    private static void assertRolledTenExtreme(GameTestHelper helper, ServerPlayer player, String via) {
        ExtremePhysique physique = BodyService.getExtremePhysique(player);
        helper.assertTrue(physique != ExtremePhysique.NONE && physique != ExtremePhysique.PURE_DREAM_REALITY_SEEKER,
                via + " to ten extremes rolled no birth physique: " + physique);
        helper.assertValueEqual(ApertureService.getAperture(player).baseEssence(), Aperture.MAX_BASE,
                via + " to ten extremes left the base below full");
    }
}
