package net.alex.guzhenren.gametest;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualEvents;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.lifecycle.PlayerDataService;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 开窍[Awakening]仪式。
 * <ul>
 *   <li>希望蛊[Hope Gu]一点即开始：收蛊、定悬浮点；{@link AwakeningTimeline#AWAKEN_TICKS} 按点击所掷基数开窍，不早一
 *       tick；{@link AwakeningTimeline#RELEASE_TICKS} 揭晓并解除定身；{@link AwakeningTimeline#END_TICKS} 结束。</li>
 *   <li>十绝天资点击时定体质并开该体质；换维度、登录、死亡立即完成，重置则清除不开窍。</li>
 *   <li>希望蛊拒绝已开窍、仪式中、刚被生物击中、附近有怪（按床判定，虎不算）者；定身中不能温养[Nourish]；沿用旧版持握仪
 *       式留在物品上的掷骰。</li>
 *   <li>模拟玩家无连接驱动 tick，故前移起始 tick 再手调 {@link AwakeningRitualService#tick}；也不在玩家列表，全服公告经
 *       其点名的资质[Aptitude]检查。</li>
 *   <li>客户端部分（闪光、牌匾、定身保持、悬浮蛊）在客户端查。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AwakeningRitualGameTests {

    private static final Vec3 STAND = new Vec3(4.5D, 2.0D, 4.5D);
    private static final int SECOND_GRADE_BASE = 65;
    private static final int TEN_EXTREMES_BASE = 100;
    private static final int LEGACY_BASE = 87;

    private AwakeningRitualGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void clickStartsTheRitualAndTakesTheGu(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = player.getMainHandItem();
        Vec3 expected = player.getEyePosition().add(player.getLookAngle().scale(AwakeningRitualService.ANCHOR_DISTANCE));

        gu.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        AwakeningRitual ritual = AwakeningRitualService.get(player);
        assertTrue(ritual != null, "the click started no ritual");
        assertTrue(player.getMainHandItem().isEmpty(), "the click did not take the Hope Gu");
        assertFalse(player.isUsingItem(), "the Hope Gu still asks for a held charge");
        assertFalse(ApertureService.isAwakened(player), "the click awakened before the ritual");
        assertEquals(helper.getLevel().getGameTime(), ritual.startTick(), "the ritual did not start on the click");
        assertTrue(ritual.base() >= 20 && ritual.base() <= 100, "the rolled base is outside the grades");
        assertTrue(ritual.anchor().distanceTo(expected) < 1.0E-6, "the Gu does not hover before the eyes");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void ritualAwakensRevealsAndEndsOnTheMarks(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        AwakeningRitualService.start(player, SECOND_GRADE_BASE);

        startAgo(player, AwakeningTimeline.AWAKEN_TICKS - 1);
        AwakeningRitualService.tick(player);
        assertTrue(AwakeningRitualService.isInRitual(player), "the ritual ended a tick early");
        assertFalse(ApertureService.isAwakened(player), "the ritual awakened a tick early");

        startAgo(player, AwakeningTimeline.AWAKEN_TICKS);
        AwakeningRitualService.tick(player);
        assertTrue(ApertureService.isAwakened(player), "the ritual did not awaken at the mark");
        assertTrue(AwakeningRitualService.isInRitual(player), "the ritual ended before the stance lifts");
        assertEquals(SECOND_GRADE_BASE, ApertureService.getAperture(player).baseEssence(),
                "the aperture opened at another base than the click rolled");

        startAgo(player, AwakeningTimeline.RELEASE_TICKS - 1);
        AwakeningRitualService.tick(player);
        assertTrue(AwakeningRitualService.holdsStill(player), "the stance lifted a tick before the release");
        assertFalse(AwakeningRitualService.get(player).revealed(), "the aptitude was revealed a tick early");
        startAgo(player, AwakeningTimeline.RELEASE_TICKS);
        AwakeningRitualService.tick(player);
        assertFalse(AwakeningRitualService.holdsStill(player), "the stance outlived the release");
        assertTrue(AwakeningRitualService.get(player).revealed(), "the release did not reveal the aptitude");
        assertEquals(Component.translatable(AwakeningRitualService.APTITUDE,
                        Component.translatable(AwakeningRitualService.GRADE_PREFIX + "second"),
                        Talent.formatPercent(SECOND_GRADE_BASE)),
                AwakeningRitualService.getAptitudeName(player), "the announcement names another grade");

        startAgo(player, AwakeningTimeline.END_TICKS - 1);
        AwakeningRitualService.tick(player);
        assertTrue(AwakeningRitualService.isInRitual(player), "the ritual ended before its plaques faded");
        startAgo(player, AwakeningTimeline.END_TICKS);
        AwakeningRitualService.tick(player);
        assertFalse(AwakeningRitualService.isInRitual(player), "the ritual outlived its end");
        assertEquals(SECOND_GRADE_BASE, ApertureService.getAperture(player).baseEssence(),
                "the end opened the aperture again");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void tenExtremesFixesItsPhysiqueAtTheClick(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        AwakeningRitualService.start(player, TEN_EXTREMES_BASE);
        ExtremePhysique rolled = AwakeningRitualService.get(player).physique();
        assertTrue(rolled != ExtremePhysique.NONE, "a Ten-Extremes ritual rolled no physique");

        startAgo(player, AwakeningTimeline.AWAKEN_TICKS);
        AwakeningRitualService.tick(player);
        assertEquals(Talent.TEN_EXTREMES, ApertureService.getTalent(player), "the ritual opened another talent");
        assertEquals(rolled, BodyService.getExtremePhysique(player), "the awakening rolled another physique");
        assertEquals(Component.translatable(rolled.getTranslationKey()), AwakeningRitualService.getAptitudeName(player),
                "the announcement does not name the physique");

        ServerPlayer graded = holder(helper);
        AwakeningRitualService.start(graded, SECOND_GRADE_BASE);
        assertEquals(ExtremePhysique.NONE, AwakeningRitualService.get(graded).physique(),
                "a graded ritual carries a physique");
        player.discard();
        graded.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void dimensionChangeAndLoginFinishAtOnce(GameTestHelper helper) {
        ServerPlayer traveler = holder(helper);
        AwakeningRitualService.start(traveler, SECOND_GRADE_BASE);
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(traveler, Level.OVERWORLD, Level.NETHER));
        assertFalse(AwakeningRitualService.isInRitual(traveler), "a dimension change left the ritual running");
        assertTrue(ApertureService.isAwakened(traveler), "a dimension change did not awaken");

        ServerPlayer returning = holder(helper);
        AwakeningRitualService.start(returning, SECOND_GRADE_BASE);
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(returning));
        assertFalse(AwakeningRitualService.isInRitual(returning), "a login left a crashed ritual running");
        assertTrue(ApertureService.isAwakened(returning), "a login did not awaken a crashed ritual");
        traveler.discard();
        returning.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void deathFinishesAndResetClears(GameTestHelper helper) {
        ServerPlayer dying = holder(helper);
        AwakeningRitualService.start(dying, SECOND_GRADE_BASE);
        AwakeningRitualEvents.onDeath(new LivingDeathEvent(dying, dying.damageSources().generic()));
        assertFalse(AwakeningRitualService.isInRitual(dying), "a death left the ritual running");
        assertTrue(ApertureService.isAwakened(dying), "a death did not awaken");

        ServerPlayer reset = holder(helper);
        AwakeningRitualService.start(reset, SECOND_GRADE_BASE);
        PlayerDataService.resetAll(reset);
        assertFalse(AwakeningRitualService.isInRitual(reset), "a reset left the ritual running");
        AwakeningRitualService.tick(reset);
        assertFalse(ApertureService.isAwakened(reset), "a reset ritual still awakened");
        dying.discard();
        reset.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hopeGuRefusesTheAwakenedAndTheRitual(GameTestHelper helper) {
        ServerPlayer awakened = holder(helper);
        ApertureService.awaken(awakened);
        ItemStack kept = awakened.getMainHandItem();
        kept.use(helper.getLevel(), awakened, InteractionHand.MAIN_HAND);
        assertFalse(AwakeningRitualService.isInRitual(awakened), "an awakened player started a ritual");
        assertEquals(1, kept.getCount(), "a refusal took the Hope Gu");

        ServerPlayer inRitual = holder(helper);
        AwakeningRitualService.start(inRitual, SECOND_GRADE_BASE);
        AwakeningRitual first = AwakeningRitualService.get(inRitual);
        ItemStack second = inRitual.getMainHandItem();
        second.use(helper.getLevel(), inRitual, InteractionHand.MAIN_HAND);
        assertEquals(first, AwakeningRitualService.get(inRitual), "a second Hope Gu restarted the ritual");
        assertEquals(1, second.getCount(), "a second Hope Gu was taken mid-ritual");
        awakened.discard();
        inRitual.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hopeGuRefusesInDanger(GameTestHelper helper) {
        ServerPlayer hit = holder(helper);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, STAND.add(0.0D, 0.0D, 3.0D));
        assertTrue(AwakeningRitualService.isInDanger(hit), "a monster 3 blocks away is no danger");
        zombie.discard();
        assertFalse(AwakeningRitualService.isInDanger(hit), "a clear room is still danger");

        TigerEntity tiger = helper.spawnWithNoFreeWill(ModEntityTypes.TIGER.get(), STAND.add(0.0D, 0.0D, 3.0D));
        assertFalse(AwakeningRitualService.isInDanger(hit), "a tiger, which no bed refuses sleep for, is danger");
        tiger.discard();

        Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, STAND.add(0.0D, 0.0D, 3.0D));
        hit.setLastHurtByMob(attacker);
        attacker.discard();
        ItemStack kept = hit.getMainHandItem();
        kept.use(helper.getLevel(), hit, InteractionHand.MAIN_HAND);
        assertFalse(AwakeningRitualService.isInRitual(hit), "a player a mob just hit started a ritual");
        assertEquals(1, kept.getCount(), "the danger refusal took the Hope Gu");

        ServerPlayer creative = GameTestPlayers.survival(helper, "gzr-awakening-gametest").creative(() -> true)
                .at(STAND);
        creative.setLastHurtByMob(helper.spawnWithNoFreeWill(EntityType.ZOMBIE, STAND.add(0.0D, 0.0D, 3.0D)));
        assertFalse(AwakeningRitualService.isInDanger(creative), "creative met the danger gate");
        hit.discard();
        creative.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void nourishingWaitsForTheStance(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ApertureService.openSecond(player, Rank.ONE);
        assertTrue(ApertureNourishService.canNourish(player, 0), "the second aperture cannot nourish to begin with");

        AwakeningRitualService.start(player, SECOND_GRADE_BASE);
        assertFalse(ApertureNourishService.canNourish(player, 0), "nourishing started inside the stance");
        startAgo(player, AwakeningTimeline.RELEASE_TICKS);
        assertTrue(ApertureNourishService.canNourish(player, 0), "nourishing still waits after the stance lifted");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void ritualTakesALegacyRoll(GameTestHelper helper) {
        ServerPlayer player = holder(helper);
        ItemStack gu = player.getMainHandItem();
        gu.set(ModDataComponents.AWAKEN_BASE, LEGACY_BASE);

        gu.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        AwakeningRitual ritual = AwakeningRitualService.get(player);
        assertTrue(ritual != null, "the click started no ritual");
        assertEquals(LEGACY_BASE, ritual.base(), "the ritual re-rolled a stack's old roll");
        player.discard();
        helper.succeed();
    }

    private static ServerPlayer holder(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survival(helper, "gzr-awakening-gametest").at(STAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HOPE_GU.get()));
        assertNull(PlayerData.get(player).getAwakeningRitual());
        return player;
    }

    private static void startAgo(ServerPlayer player, int ticks) {
        AwakeningRitual ritual = AwakeningRitualService.get(player);
        assertTrue(ritual != null, "no ritual to move back");
        PlayerData.get(player).setAwakeningRitual(new AwakeningRitual(player.level().getGameTime() - ticks,
                ritual.base(), ritual.physique(), ritual.anchor(), ritual.revealed()));
    }
}
