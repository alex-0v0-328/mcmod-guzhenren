package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.display.InfoModel;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.path.EffectCarrier;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectEndedEvent;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.PathService;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.command;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertSame;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 活玩家灌注类蛊[Channeled Gu]的身体效果：十二炼体档记作永久金道道痕[Path Marks]（以点计），每部位一槽，可替换、可由命
 * 令设置与清除，死亡时抹去或保留；玉皮封于铜皮之上；兽力[Beast Strength]授予与撤销；人力层数随存储计数；B 面板行。无力
 * 量读取这些道痕。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CarvingEffectGameTests {
    private CarvingEffectGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void everyTemperingGradeLaysItsGuPointsOnTheBody(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        long[] points = { 12L, 48L, 192L, 768L };

        for (int rank = 1; rank <= 4; rank++) {
            String bronze = "bronze_skin_gu_" + rank;
            BodyService.wearSkin(player, Skin.valueOf("BRONZE_SKIN_" + rank), 0);
            assertBody(player, ModPathEffectTypes.BODY_SKIN, bronze, points[rank - 1]);
            BodyService.wearSkin(player, Skin.valueOf("IRON_SKIN_" + rank), 0);
            assertBody(player, ModPathEffectTypes.BODY_SKIN, "iron_skin_gu_" + rank, points[rank - 1]);
            BodyService.shedSkin(player);
        }
        BodyService.temperTendon(player, Tendon.STEEL_TENDON_3);
        assertBody(player, ModPathEffectTypes.BODY_TENDON, "steel_tendon_gu_3", 192L);
        BodyService.temperTendon(player, Tendon.STEEL_TENDON_4);
        assertBody(player, ModPathEffectTypes.BODY_TENDON, "steel_tendon_gu_4", 768L);
        BodyService.temperBone(player, Bone.IRON_BONE_3);
        assertBody(player, ModPathEffectTypes.BODY_BONE, "iron_bone_gu_3", 192L);
        BodyService.temperBone(player, Bone.IRON_BONE_4);
        assertBody(player, ModPathEffectTypes.BODY_BONE, "iron_bone_gu_4", 768L);

        assertEquals(PathMarks.ofPoints(1_536L), PathService.getMarks(player, GuPath.METAL));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aNewGradeTakesTheOldEffectAndItsPointsWithIt(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        List<PathEffectEndedEvent> ended = new ArrayList<>();

        GameTestWorld.whileListening(PathEffectEndedEvent.class, event -> {
            if (event.getPlayer() == player) ended.add(event);
        }, () -> {
            BodyService.wearSkin(player, Skin.BRONZE_SKIN_1, 0);
            BodyService.wearSkin(player, Skin.BRONZE_SKIN_3, 0);
            BodyService.temperTendon(player, Tendon.STEEL_TENDON_3);
            BodyService.temperBone(player, Bone.IRON_BONE_3);
        });

        assertEquals(3L, PathEffectService.getEffects(player).size());
        assertEquals(PathMarks.ofPoints(192L * 3L), PathService.getMarks(player, GuPath.METAL));
        assertEquals(1L, ended.size());
        assertSame(PathEffectEndedEvent.Reason.REPLACED, ended.getFirst().getReason());
        assertEquals(Guzhenren.id("bronze_skin_gu_1"), ended.getFirst().getEffect().source());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theBodyCommandsSetAndClearTheEffectWithTheGrade(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        command(player, "gzr body bone set iron_bone 3");
        assertBody(player, ModPathEffectTypes.BODY_BONE, "iron_bone_gu_3", 192L);
        command(player, "gzr body bone set iron_bone 4");
        assertBody(player, ModPathEffectTypes.BODY_BONE, "iron_bone_gu_4", 768L);
        command(player, "gzr body bone set native");
        assertNull(PathEffectService.find(player, ModPathEffectTypes.BODY_BONE));
        assertSame(Bone.NATIVE, BodyService.getBone(player));

        command(player, "gzr body tendon set steel_tendon 4");
        assertBody(player, ModPathEffectTypes.BODY_TENDON, "steel_tendon_gu_4", 768L);
        command(player, "gzr body tendon set native");
        assertNull(PathEffectService.find(player, ModPathEffectTypes.BODY_TENDON));

        command(player, "gzr body skin set bronze_skin 2");
        assertBody(player, ModPathEffectTypes.BODY_SKIN, "bronze_skin_gu_2", 48L);
        command(player, "gzr body skin set iron_skin 4");
        assertBody(player, ModPathEffectTypes.BODY_SKIN, "iron_skin_gu_4", 768L);
        command(player, "gzr body skin set native");
        assertNull(PathEffectService.find(player, ModPathEffectTypes.BODY_SKIN));
        assertTrue(PathEffectService.getEffects(player).isEmpty());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aJadeSkinSetOverABronzeOneDropsOnlyTheBronzeEffect(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.temperBone(player, Bone.IRON_BONE_3);
        BodyService.wearSkin(player, Skin.BRONZE_SKIN_2, 0);

        BodyService.wearSkin(player, Skin.JADE_SKIN, 600);

        assertNull(PathEffectService.find(player, ModPathEffectTypes.BODY_SKIN));
        assertNotNull(PathEffectService.find(player, ModPathEffectTypes.BODY_BONE), () -> "the bone effect went");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aWipingDeathClearsTheEffectsAndAKeptRespawnKeepsThem(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.wearSkin(player, Skin.IRON_SKIN_3, 0);
        BodyService.temperTendon(player, Tendon.STEEL_TENDON_4);
        BodyService.temperBone(player, Bone.IRON_BONE_3);

        BodyService.revive(player);
        assertSame(Skin.IRON_SKIN_3, BodyService.getSkin(player));
        assertEquals(3L, PathEffectService.getEffects(player).size());
        assertEquals(PathMarks.ofPoints(192L + 768L + 192L), PathService.getMarks(player, GuPath.METAL));

        PlayerDataService.resetAll(player);
        assertSame(Skin.NATIVE, BodyService.getSkin(player));
        assertSame(Tendon.NATIVE, BodyService.getTendon(player));
        assertSame(Bone.NATIVE, BodyService.getBone(player));
        assertTrue(PathEffectService.getEffects(player).isEmpty(), "a wiping death kept a body effect");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aBeastStrengthIsOneEffectPerSpeciesAndRevokeTakesIt(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        PathStrengthService.grant(player, BeastStrength.BEAR);
        PathEffect<BeastStrength> boar = PathEffectService.find(player, ModPathEffectTypes.BEAST_STRENGTH,
                Guzhenren.id("strength/white_boar"));
        assertNotNull(boar, () -> "the boar strength laid no effect");
        assertSame(EffectCarrier.BODY, boar.carrier());
        assertSame(BeastStrength.WHITE_BOAR, boar.payload());
        assertEquals(PathMarks.ofPoints(48L + 80L), PathService.getMarks(player, GuPath.STRENGTH));

        PathStrengthService.grant(player, BeastStrength.WHITE_BOAR);
        assertEquals(PathMarks.ofPoints(48L + 80L), PathService.getMarks(player, GuPath.STRENGTH));

        PathStrengthService.revoke(player, BeastStrength.WHITE_BOAR);
        assertNull(PathEffectService.find(player, ModPathEffectTypes.BEAST_STRENGTH, Guzhenren.id("strength/white_boar")));
        assertEquals(PathMarks.ofPoints(80L), PathService.getMarks(player, GuPath.STRENGTH));

        PathStrengthService.clear(player);
        assertTrue(PathEffectService.getEffects(player).isEmpty(), "clearing kept a beast effect");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theTigerStrengthIsARankTwoBeastWithItsOwnFamily(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        PathStrengthService.grant(player, BeastStrength.TIGER);
        PathEffect<BeastStrength> tiger = PathEffectService.find(player, ModPathEffectTypes.BEAST_STRENGTH,
                Guzhenren.id("strength/tiger"));
        assertNotNull(tiger, () -> "the tiger strength laid no effect");
        assertSame(BeastStrength.TIGER, tiger.payload());
        assertEquals(PathMarks.ofPoints(128L), PathService.getMarks(player, GuPath.STRENGTH));
        assertEquals(64, StrengthService.getStrengthPoints(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theTurtleAndHorseStrengthsAreRankThreeBeastsWithTheirOwnFamilies(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        PathStrengthService.grant(player, BeastStrength.STONE_TURTLE);
        PathStrengthService.grant(player, BeastStrength.GALLOPING_HORSE);
        for (BeastStrength beast : List.of(BeastStrength.STONE_TURTLE, BeastStrength.GALLOPING_HORSE)) {
            String id = beast == BeastStrength.STONE_TURTLE ? "strength/stone_turtle"
                    : "strength/galloping_horse";
            PathEffect<BeastStrength> effect = PathEffectService.find(player, ModPathEffectTypes.BEAST_STRENGTH,
                    Guzhenren.id(id));
            assertNotNull(effect, () -> beast + " laid no effect");
            assertSame(beast, effect.payload());
        }
        MortalGuItem turtle = (MortalGuItem) ModItems.STONE_TURTLE_STRENGTH_GU_3.get();
        assertEquals(Rank.THREE, turtle.getGuProperties().getRank());
        assertEquals(turtle.getGuProperties().getEffectPaths().get(GuPath.STRENGTH).add(
                ((MortalGuItem) ModItems.GALLOPING_HORSE_STRENGTH_GU_3.get()).getGuProperties().getEffectPaths()
                        .get(GuPath.STRENGTH)), PathService.getMarks(player, GuPath.STRENGTH));
        assertEquals(96, StrengthService.getStrengthPoints(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRemovedStrengthEffectTakesTheStrengthAndItsPointsWithIt(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        PathStrengthService.grant(player, BeastStrength.TIGER);
        assertEquals(64, StrengthService.getStrengthPoints(player));
        PathEffectService.removeType(player, ModPathEffectTypes.BEAST_STRENGTH);
        assertTrue(!PathStrengthService.has(player, BeastStrength.TIGER), "the beast strength outlived its marks");
        assertEquals(0, StrengthService.getStrengthPoints(player));

        PathStrengthService.addHumanStrength(player, HumanStrength.TEN_JIN, 3);
        assertEquals(30, StrengthService.getStrengthPoints(player));
        PathEffectService.removeType(player, ModPathEffectTypes.HUMAN_STRENGTH);
        assertEquals(0, PathStrengthService.getHumanStrength(player, HumanStrength.TEN_JIN));
        assertEquals(0, StrengthService.getStrengthPoints(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRemovedTemperingEffectDropsThePartToItsNativeGrade(GameTestHelper helper) {
        ServerPlayer player = survival(helper);

        BodyService.wearSkin(player, Skin.IRON_SKIN_3, 0);
        BodyService.temperTendon(player, Tendon.STEEL_TENDON_3);
        BodyService.temperBone(player, Bone.IRON_BONE_3);
        PathEffectService.removeType(player, ModPathEffectTypes.BODY_SKIN);
        PathEffectService.removeType(player, ModPathEffectTypes.BODY_TENDON);
        PathEffectService.removeType(player, ModPathEffectTypes.BODY_BONE);
        assertSame(Skin.NATIVE, BodyService.getSkin(player));
        assertSame(Tendon.NATIVE, BodyService.getTendon(player));
        assertSame(Bone.NATIVE, BodyService.getBone(player));

        BodyService.wearSkin(player, Skin.BRONZE_SKIN_2, 0);
        BodyService.wearSkin(player, Skin.JADE_SKIN, 600);
        assertSame(Skin.JADE_SKIN, BodyService.getSkin(player));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aHumanStrengthEffectFollowsTheStoredLayers(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        ResourceLocation source = Guzhenren.id("strength/ten_jin");

        PathStrengthService.addHumanStrength(player, HumanStrength.TEN_JIN, 1);
        assertHuman(player, source, 1, 20L);
        PathStrengthService.addHumanStrength(player, HumanStrength.TEN_JIN, 1);
        PathStrengthService.addHumanStrength(player, HumanStrength.TEN_JIN, 1);
        assertHuman(player, source, 3, 60L);

        PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 1);
        assertHuman(player, source, 1, 20L);
        PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 5);
        assertHuman(player, source, 5, 100L);

        PathStrengthService.addHumanStrength(player, HumanStrength.JIN, 2);
        assertEquals(PathMarks.ofPoints(100L + 4L), PathService.getMarks(player, GuPath.STRENGTH));
        PathStrengthService.addHumanStrength(player, HumanStrength.JUN, 1);
        PathStrengthService.addHumanStrength(player, HumanStrength.TEN_JUN, 1);
        assertEquals(PathMarks.ofPoints(100L + 4L + 60L + 600L), PathService.getMarks(player, GuPath.STRENGTH));

        PathStrengthService.setHumanStrength(player, HumanStrength.TEN_JIN, 0);
        assertNull(PathEffectService.find(player, ModPathEffectTypes.HUMAN_STRENGTH, source));
        command(player, "gzr path strength clear");
        assertTrue(PathEffectService.getEffects(player).isEmpty(), "clearing kept a human effect");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theBPanelListsABodyEffectUnderTheBodyCarrierAndItsPointsInThePathTotal(GameTestHelper helper) {
        ServerPlayer player = survival(helper);
        BodyService.temperBone(player, Bone.IRON_BONE_4);

        List<InfoModel.Entry> rows = InfoModel.buildPathRows(player).stream().map(InfoModel.Row::entry).toList();

        PathEffect<Bone> effect = PathEffectService.find(player, ModPathEffectTypes.BODY_BONE);
        assertNotNull(effect, () -> "no bone effect");
        int carrier = rows.indexOf(new InfoModel.EffectCarrierRow(EffectCarrier.BODY));
        assertTrue(carrier >= 0, () -> "no body carrier row: " + rows);
        assertEquals(new InfoModel.PathEffectRow(effect, PathEffect.PERMANENT), rows.get(carrier + 1));
        assertTrue(rows.stream().anyMatch(row -> row instanceof InfoModel.PathRow path && path.path() == GuPath.METAL
                && path.marks().equals(PathMarks.ofPoints(768L))), () -> "no metal total: " + rows);
        helper.succeed();
    }

    private static void assertBody(ServerPlayer player, PathEffectType<?> type, String id, long points) {
        PathEffect<?> effect = PathEffectService.find(player, type);
        assertNotNull(effect, () -> type.getId() + " laid no effect for " + id);
        assertEquals(Guzhenren.id(id), effect.source());
        assertSame(EffectCarrier.BODY, effect.carrier());
        assertTrue(effect.isPermanent(), "a tempered grade ended");
        assertEquals(PathMarks.ofPoints(points), effect.getMarks(GuPath.METAL));
        assertEquals(1L, PathEffectService.getEffects(player).stream().filter(other -> other.type() == type).count());
        assertFalse(effect.type().getIcon() != null, "a permanent grade projects an icon");
    }

    private static void assertHuman(ServerPlayer player, ResourceLocation source, int layers, long points) {
        PathEffect<HumanStrength> effect = PathEffectService.find(player, ModPathEffectTypes.HUMAN_STRENGTH, source);
        assertNotNull(effect, () -> "no human strength effect at " + layers + " layers");
        assertEquals(layers, effect.layers());
        assertEquals(PathMarks.ofPoints(points), effect.getMarks(GuPath.STRENGTH));
    }

    private static ServerPlayer survival(GameTestHelper helper) {
        return GameTestPlayers.survival(helper, "gzr-carving-effect-gametest").onGround();
    }
}
