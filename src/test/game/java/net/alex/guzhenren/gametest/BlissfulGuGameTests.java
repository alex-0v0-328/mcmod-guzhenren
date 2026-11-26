package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.blissful.gu.GuWork;
import net.alex.guzhenren.gameplay.blissful.land.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.land.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.mountain.MountainWork;
import net.alex.guzhenren.gameplay.secretrealm.RealmSite;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 福地「蛊虫」页：捕获入库捕到只剩保持数、保持数 0 不捕、已命名的不算；托管只捕开着托管的种类；挪生物上山跳过野生蛊。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulGuGameTests {

    private BlissfulGuGameTests() {}

    private static List<Entity> spawnGus(GameTestHelper helper) {
        GameTestPlayers.ground(helper);
        Entity hope1 = helper.spawn(ModEntityTypes.HOPE_GU_ENTITY.get(), new BlockPos(1, 1, 1));
        Entity hope2 = helper.spawn(ModEntityTypes.HOPE_GU_ENTITY.get(), new BlockPos(2, 1, 1));
        Entity hope3 = helper.spawn(ModEntityTypes.HOPE_GU_ENTITY.get(), new BlockPos(3, 1, 1));
        Entity named = helper.spawn(ModEntityTypes.HOPE_GU_ENTITY.get(), new BlockPos(4, 1, 1));
        named.setCustomName(Component.literal("Named"));
        Entity boar = helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), new BlockPos(5, 1, 1));
        return List.of(hope1, hope2, hope3, named, boar);
    }

    @GameTest(template = "empty9x9x9")
    public static void capturingKeepsTheKeptNumberAndStoresUnrefinedGu(GameTestHelper helper) {
        List<Entity> swarm = spawnGus(helper);
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        ResourceLocation hope = GuWork.typeId(swarm.getFirst());
        Map<ResourceLocation, List<Mob>> census = GuWork.census(swarm);
        helper.assertValueEqual(census.get(hope).size(), 3, "a named gu is not counted");
        helper.assertValueEqual(GuWork.captureCount(census, Map.of(hope, 1), GuWork.LIMIT), 2,
                "the preview matches the capture");

        int caught = GuWork.capture(census, Map.of(hope, 1), data.storage(), GuWork.LIMIT);
        helper.assertValueEqual(caught, 2, "three hope gu down to one");
        helper.assertValueEqual(data.storage().count(stack -> stack.is(ModItems.HOPE_GU.get())), 2L,
                "the caught gu go to storage as items");
        helper.assertTrue(swarm.get(0).isRemoved() && swarm.get(1).isRemoved() && !swarm.get(2).isRemoved(),
                "the caught gu leave the world");
        helper.assertFalse(swarm.get(4).isRemoved(), "a kind kept at zero is left alone");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hostingCapturesOnlyHostedKinds(GameTestHelper helper) {
        List<Entity> swarm = spawnGus(helper);
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        ResourceLocation hope = GuWork.typeId(swarm.getFirst());
        ResourceLocation boar = GuWork.typeId(swarm.get(4));
        data.gu().setKeep(hope, 1);
        data.gu().setKeep(boar, 1);
        data.gu().setHosted(boar, true);

        helper.assertValueEqual(GuWork.host(GuWork.census(swarm), data, GuWork.LIMIT), 0,
                "the hosted boar is already at its kept number, the hope gu are not hosted");
        data.gu().setHosted(hope, true);
        helper.assertValueEqual(GuWork.host(GuWork.census(swarm), data, GuWork.LIMIT), 2,
                "hosting the hope gu captures the two extra");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", skyAccess = true)
    public static void movingMobsUpLeavesWildGuAlone(GameTestHelper helper) {
        List<Entity> swarm = spawnGus(helper);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) helper.setBlock(CENTER_POS.offset(x, 0, z), ModBlocks.SOULSUNDER_STONE.get());
        }
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(8, 1, 8));
        BlissfulLandData data = BlissfulLandData.create(BlissfulLand.FOX_IMMORTAL);
        RealmSite site = new RealmSite(helper.absolutePos(CENTER_POS), 2);
        List<Entity> candidates = new ArrayList<>(swarm);
        candidates.add(pig);

        int moved = MountainWork.moveMobsUp(helper.getLevel(), site, data, helper.getLevel().getRandom(), candidates);
        helper.assertValueEqual(moved, 1, "only the pig is moved up the mountain");
        helper.assertTrue(site.contains(pig), "the pig stands on the mountain");
        helper.assertTrue(swarm.stream().noneMatch(site::contains), "no wild gu is moved up to die");
        helper.succeed();
    }
}
