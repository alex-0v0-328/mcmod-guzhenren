package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureEssenceService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.clock.Beat;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.mind.Brilliance;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.awakenedMortal;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;

/**
 * 资源加法饱和测试。肉身、真元[Essence]、魂魄、脑海、道[Path]加法在 long 范围内饱和，不回绕。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SaturationGameTests {

    private SaturationGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void bodyAndDistilledEssenceAdditionsDoNotWrap(GameTestHelper helper) {
        ServerPlayer player = awakenedMortal(helper);
        BodyService.addAge(player, Long.MAX_VALUE);
        helper.assertValueEqual(BodyService.get(player).ageParts(), Long.MAX_VALUE,
                "age addition saturates");
        BodyService.addLifespan(player, Long.MAX_VALUE);
        helper.assertValueEqual(BodyService.get(player).lifespanParts(), Long.MAX_VALUE,
                "lifespan addition saturates");
        BodyService.setLifespan(player, Long.MIN_VALUE);
        BodyService.addLifespan(player, Long.MAX_VALUE);
        helper.assertValueEqual(BodyService.get(player).lifespanParts(), Long.MAX_VALUE,
                "year conversion must not saturate before adding signed lifespan");
        var aperture = ApertureService.getAperture(player).withDistilling(true).withDistilledEssence(1L);
        ApertureService.set(player, ApertureData.PRIMARY, aperture);
        ApertureEssenceService.addDistilled(player, Long.MAX_VALUE);
        helper.assertTrue(ApertureService.getAperture(player).distilledEssence() > 0L,
                "positive distilled essence addition emptied the pool");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void soulAddCannotWrapIntoDeath(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        SoulService.addMax(player, Long.MAX_VALUE);
        helper.assertValueEqual(SoulService.get(player).maxSoul(), Long.MAX_VALUE,
                "soul cap saturates");
        SoulService.addCurrent(player, Long.MAX_VALUE);
        helper.assertValueEqual(SoulService.get(player).currentSoul(), Long.MAX_VALUE,
                "soul current saturates");
        SoulService.addCurrent(player, Long.MIN_VALUE);
        helper.assertValueEqual(SoulService.get(player).currentSoul(), 0L,
                "negative delta clamps to zero");
        GameTestPlayers.command(player, "gzr soul max sub -9223372036854775808");
        helper.assertValueEqual(SoulService.get(player).maxSoul(), Long.MAX_VALUE,
                "subtraction cannot negate MIN");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void mindAddAndRegenerationSaturate(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        var thoughts = MindPoolType.THOUGHTS;
        MindService.addMax(player, thoughts, Long.MAX_VALUE);
        helper.assertValueEqual(MindService.getMax(player, thoughts), Long.MAX_VALUE,
                "mind cap saturates");
        MindService.setCurrent(player, thoughts, Long.MAX_VALUE - 1L);
        MindService.setBrilliance(player, Brilliance.OUTSTANDING);
        MindService.regenStep(player, new Beat(0L, TimeFlow.NORMAL_RATE, player.getRandom()));
        helper.assertValueEqual(MindService.getCurrent(player, thoughts), Long.MAX_VALUE,
                "regen saturates");
        MindService.addCurrent(player, thoughts, 1L);
        helper.assertValueEqual(MindService.getCurrent(player, thoughts), Long.MAX_VALUE,
                "mind current saturates");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void pathResourceAdditionSaturates(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        PathService.setBarePoints(player, GuPath.STRENGTH, 1L);
        PathService.addBarePoints(player, GuPath.STRENGTH, Long.MAX_VALUE);
        helper.assertValueEqual(PathService.getBareMarks(player, GuPath.STRENGTH).points(), Long.MAX_VALUE,
                "points saturate");
        PathService.setBareMarks(player, GuPath.STRENGTH, Long.MAX_VALUE);
        BodyService.setExtremePhysique(player, ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL);
        helper.assertValueEqual(PathService.getMarks(player, GuPath.STRENGTH).marks(), Long.MAX_VALUE,
                "the whole-mark total saturates");
        helper.succeed();
    }
}
