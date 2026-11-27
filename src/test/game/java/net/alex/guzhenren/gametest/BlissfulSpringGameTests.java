package net.alex.guzhenren.gametest;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.immortalaperture.BlissfulLands;
import net.alex.guzhenren.gameplay.immortalaperture.managementpanel.OverviewPart;
import net.alex.guzhenren.gameplay.immortalaperture.spring.SpringRules;
import net.alex.guzhenren.gameplay.immortalaperture.spring.SpringState;
import net.alex.guzhenren.gameplay.immortalaperture.spring.SpringWork;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 福地「引泉」：区块扫描登记泉源（流动臂不算）、托管轮每眼折算 768 颗元石入库藏、舀掉与割弃出疆域的泉不付并注销、没加载区块的泉
 * 按登记照付、区块重扫校正过期登记、总览段带泉数过线上编解码；福地之外元泉方块不被地灵「收走」。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulSpringGameTests {

    private static final int HUGE_HALF = 2_000_000;
    private static final int FAR_BLOCK = 1_600_000;

    private BlissfulSpringGameTests() {}

    private static BlissfulLandData wholeLand() {
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        data.setTerritory(Territory.whole(HUGE_HALF, 32));
        return data;
    }

    private static long stones(BlissfulLandData data) {
        return BlissfulStorage.of(data).count(stack -> stack.is(ModItems.PRIMEVAL_STONE.get()));
    }

    private static BlockPos placeSpring(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative.below(), Blocks.STONE);
        helper.setBlock(relative, ModBlocks.SPIRIT_SPRING.get());
        return helper.absolutePos(relative);
    }

    @GameTest(template = "empty9x9x9")
    public static void aChunkScanRegistersSourcesAndTheRoundPaysEach(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos first = placeSpring(helper, CENTER_POS);
        BlockPos second = placeSpring(helper, CENTER_POS.offset(3, 0, 3));
        BlissfulLandData data = wholeLand();

        SpringWork.onChunkLoad(data, level.getChunkAt(first));
        SpringWork.onChunkLoad(data, level.getChunkAt(second));
        SpringState springs = SpringState.of(data);
        helper.assertTrue(springs.contains(first) && springs.contains(second), "both sources are registered");
        helper.assertValueEqual(springs.count(), 2, "only the two sources, no flowing arm");

        helper.assertValueEqual(SpringWork.settle(level, data), 2, "both springs are paid");
        helper.assertValueEqual(stones(data), 2L * SpringRules.STONES_PER_SPRING_PER_ROUND,
                "768 stones a spring land in storage");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void onlyASourceCountsNotAFlowingArm(GameTestHelper helper) {
        BlockState source = ModBlocks.SPIRIT_SPRING.get().defaultBlockState();
        helper.assertTrue(SpringWork.isSource(source), "level 0 is a source");
        helper.assertFalse(SpringWork.isSource(source.setValue(LiquidBlock.LEVEL, 3)), "level 3 is a flowing arm");
        helper.assertFalse(SpringWork.isSource(Blocks.WATER.defaultBlockState()), "water is not a spring");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aScoopedSpringStopsPayingAndIsForgotten(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos spring = placeSpring(helper, CENTER_POS);
        BlissfulLandData data = wholeLand();
        SpringWork.onChunkLoad(data, level.getChunkAt(spring));
        helper.assertValueEqual(SpringState.of(data).count(), 1, "the spring is registered");

        level.setBlockAndUpdate(spring, Blocks.AIR.defaultBlockState());
        helper.assertValueEqual(SpringWork.settle(level, data), 0, "a scooped spring is not paid");
        helper.assertValueEqual(SpringState.of(data).count(), 0, "and it is forgotten");
        helper.assertValueEqual(stones(data), 0L, "nothing reaches storage");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aSpringInACededChunkIsDroppedUnpaid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos spring = placeSpring(helper, CENTER_POS);
        BlissfulLandData data = wholeLand();
        SpringWork.onChunkLoad(data, level.getChunkAt(spring));
        helper.assertValueEqual(SpringState.of(data).count(), 1, "the spring is registered");

        int chunkX = spring.getX() >> 4;
        data.setTerritory(new Territory(HUGE_HALF, 32, 0, HUGE_HALF - chunkX, 0, 0));
        helper.assertFalse(data.territory().contains(chunkX, spring.getZ() >> 4), "the strip now covers the spring");
        helper.assertValueEqual(SpringWork.settle(level, data), 0, "a ceded spring is not paid");
        helper.assertValueEqual(SpringState.of(data).count(), 0, "and it is forgotten");
        helper.assertValueEqual(stones(data), 0L, "nothing reaches storage");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void springsInUnloadedChunksPayFromTheRegistry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlissfulLandData data = wholeLand();
        SpringState springs = SpringState.of(data);
        springs.add(new BlockPos(FAR_BLOCK, 64, FAR_BLOCK));
        springs.add(new BlockPos(FAR_BLOCK + 40, 64, FAR_BLOCK));
        helper.assertTrue(level.getChunkSource().getChunkNow(FAR_BLOCK >> 4, FAR_BLOCK >> 4) == null,
                "the far chunk is not loaded");

        helper.assertValueEqual(SpringWork.settle(level, data), 2, "unloaded springs are trusted");
        helper.assertValueEqual(springs.count(), 2, "and stay registered");
        helper.assertValueEqual(stones(data), 2L * SpringRules.STONES_PER_SPRING_PER_ROUND,
                "paid with no chunk loaded");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void aRescanForgetsAStaleSpring(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos stale = helper.absolutePos(CENTER_POS);
        BlissfulLandData data = wholeLand();
        SpringState.of(data).add(stale);

        SpringWork.onChunkLoad(data, level.getChunkAt(stale));
        helper.assertValueEqual(SpringState.of(data).count(), 0, "the chunk holds no spring, the entry goes");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theHostingRoundPaysTheSprings(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos spring = placeSpring(helper, CENTER_POS);
        BlissfulLandData data = wholeLand();
        SpringWork.onChunkLoad(data, level.getChunkAt(spring));

        BlissfulLands.host(level.getServer(), level, data);
        helper.assertValueEqual(stones(data), (long) SpringRules.STONES_PER_SPRING_PER_ROUND,
                "the assembled hosting round settles the spring");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void springsOutsideALandAreNotCollected(GameTestHelper helper) {
        helper.assertFalse(SpringWork.collects(helper.getLevel()), "no land here, the spring keeps dropping");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void theOverviewSectionCarriesTheSpringCountOnTheWire(GameTestHelper helper) {
        OverviewPart.Section section = new OverviewPart.Section(5L, 3, 9, 4, 8, 7, List.of());
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        OverviewPart.Section.STREAM_CODEC.encode(buffer, section);
        helper.assertValueEqual(OverviewPart.Section.STREAM_CODEC.decode(buffer), section, "an overview round trip");
        helper.succeed();
    }
}
