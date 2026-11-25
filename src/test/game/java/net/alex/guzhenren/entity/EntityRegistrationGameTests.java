package net.alex.guzhenren.entity;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BearEntity;
import net.alex.guzhenren.entity.beast.BearSpecies;
import net.alex.guzhenren.entity.beast.CattleEntity;
import net.alex.guzhenren.entity.beast.CattleSpecies;
import net.alex.guzhenren.entity.beast.ElephantEntity;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.entity.beast.TortoiseEntity;
import net.alex.guzhenren.entity.beast.TortoiseSpecies;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.alex.guzhenren.entity.gu.CrashGuEntity;
import net.alex.guzhenren.gameplay.trade.SoulTrader;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertFalse;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 模组生物注册钉子：id、类别、尺寸、基础属性、野生蛊生成位置；冲撞蛊[Crash Gu]悬停判定（其类需注册表绑定才能加载）。打
 * 包资源契约见 {@code EntityResourcesTest}。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EntityRegistrationGameTests {

    private EntityRegistrationGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void wildBoarRegistrationKeepsCreatureCategoryDimensionsAndAttributes(GameTestHelper helper) {
        EntityType<WildBoarEntity> type = ModEntityTypes.WILD_BOAR.get();
        assertEquals(Guzhenren.id("wild_boar"), BuiltInRegistries.ENTITY_TYPE.getKey(type));
        assertEquals(MobCategory.CREATURE, type.getCategory());
        assertEquals(1.1F, type.getWidth(), 1.0e-6F);
        assertEquals(1.2F, type.getHeight(), 1.0e-6F);

        var attributes = WildBoarEntity.createAttributes().build();
        assertEquals(30.0, attributes.getBaseValue(Attributes.MAX_HEALTH), 1.0e-6);
        assertEquals(0.25, attributes.getBaseValue(Attributes.MOVEMENT_SPEED), 1.0e-6);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void bearRegistrationKeepsCreatureCategoryDimensionsAndAttributes(GameTestHelper helper) {
        double[] expectedHealth = { 54.0, 42.0, 40.0, 40.0 };
        BearSpecies[] species = BearSpecies.values();
        EntityType<?>[] types = { ModEntityTypes.BROWN_BEAR.get(), ModEntityTypes.ASIAN_BLACK_BEAR.get(),
                ModEntityTypes.AMERICAN_BLACK_BEAR.get(), ModEntityTypes.ALBINO_BEAR.get() };
        for (int i = 0; i < species.length; i++) {
            EntityType<?> type = types[i];
            assertEquals(Guzhenren.id(species[i].getId()), BuiltInRegistries.ENTITY_TYPE.getKey(type));
            assertEquals(MobCategory.CREATURE, type.getCategory());
            assertEquals(1.2F, type.getWidth(), 1.0e-6F);
            assertEquals(1.4F, type.getHeight(), 1.0e-6F);

            var attributes = BearEntity.createAttributes(species[i]).build();
            assertEquals(expectedHealth[i], attributes.getBaseValue(Attributes.MAX_HEALTH), 1.0e-6);
            assertEquals(0.25, attributes.getBaseValue(Attributes.MOVEMENT_SPEED), 1.0e-6);
            assertEquals(16.0, attributes.getBaseValue(Attributes.FOLLOW_RANGE), 1.0e-6);
        }
        assertTrue(BearSpecies.ASIAN_BLACK.isHostile());
        assertFalse(BearSpecies.BROWN.isHostile());
        assertFalse(BearSpecies.AMERICAN_BLACK.isHostile());
        assertFalse(BearSpecies.ALBINO.isHostile());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void tigerRegistrationKeepsCreatureCategoryDimensionsAndAttributes(GameTestHelper helper) {
        EntityType<?>[] types = { ModEntityTypes.TIGER.get(), ModEntityTypes.WHITE_TIGER.get() };
        String[] ids = { "tiger", "white_tiger" };
        for (int i = 0; i < types.length; i++) {
            assertEquals(Guzhenren.id(ids[i]), BuiltInRegistries.ENTITY_TYPE.getKey(types[i]));
            assertEquals(MobCategory.CREATURE, types[i].getCategory());
            assertEquals(1.3F, types[i].getWidth(), 1.0e-6F);
            assertEquals(1.4F, types[i].getHeight(), 1.0e-6F);
        }

        var attributes = TigerEntity.createAttributes().build();
        assertEquals(48.0, attributes.getBaseValue(Attributes.MAX_HEALTH), 1.0e-6);
        assertEquals(0.28, attributes.getBaseValue(Attributes.MOVEMENT_SPEED), 1.0e-6);
        assertEquals(24.0, attributes.getBaseValue(Attributes.FOLLOW_RANGE), 1.0e-6);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void cattleRegistrationKeepsCreatureCategoryDimensionsAndAttributes(GameTestHelper helper) {
        EntityType<?>[] types = { ModEntityTypes.YELLOW_OX.get(), ModEntityTypes.WATER_BUFFALO.get(),
                ModEntityTypes.AZURE_OX.get() };
        CattleSpecies[] species = CattleSpecies.values();
        for (int i = 0; i < types.length; i++) {
            EntityType<?> type = types[i];
            assertEquals(Guzhenren.id(species[i].getId()), BuiltInRegistries.ENTITY_TYPE.getKey(type));
            assertEquals(MobCategory.CREATURE, type.getCategory());
            assertEquals(1.7F, type.getWidth(), 1.0e-6F);
            assertEquals(1.75F, type.getHeight(), 1.0e-6F);
            CattleEntity cattle = (CattleEntity) type.create(helper.getLevel());
            assertTrue(cattle != null, species[i].getId());
            assertEquals(species[i], cattle.getSpecies());
            cattle.discard();
        }
        var attributes = CattleEntity.createAttributes().build();
        assertEquals(20.0, attributes.getBaseValue(Attributes.MAX_HEALTH), 1.0e-6);
        assertEquals(0.2, attributes.getBaseValue(Attributes.MOVEMENT_SPEED), 1.0e-6);
        assertEquals(16.0, attributes.getBaseValue(Attributes.FOLLOW_RANGE), 1.0e-6);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void elephantRegistrationKeepsCreatureCategoryDimensionsAndAttributes(GameTestHelper helper) {
        EntityType<?>[] types = { ModEntityTypes.ELEPHANT.get(), ModEntityTypes.WHITE_ELEPHANT.get() };
        String[] ids = { "elephant", "white_elephant" };
        for (int i = 0; i < types.length; i++) {
            assertEquals(Guzhenren.id(ids[i]), BuiltInRegistries.ENTITY_TYPE.getKey(types[i]));
            assertEquals(MobCategory.CREATURE, types[i].getCategory());
            assertEquals(2.5F, types[i].getWidth(), 1.0e-6F);
            assertEquals(3.7F, types[i].getHeight(), 1.0e-6F);
        }
        var attributes = ElephantEntity.createAttributes().build();
        assertEquals(120.0, attributes.getBaseValue(Attributes.MAX_HEALTH), 1.0e-6);
        assertEquals(4.0, attributes.getBaseValue(Attributes.ARMOR), 1.0e-6);
        assertEquals(1.0, attributes.getBaseValue(Attributes.KNOCKBACK_RESISTANCE), 1.0e-6);
        assertEquals(0.1715, attributes.getBaseValue(Attributes.MOVEMENT_SPEED), 1.0e-6);
        assertEquals(1.0, attributes.getBaseValue(Attributes.STEP_HEIGHT), 1.0e-6);
        assertEquals(16.0, attributes.getBaseValue(Attributes.FOLLOW_RANGE), 1.0e-6);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void tortoiseRegistrationKeepsCreatureCategoryDimensionsAndSpeciesAttributes(GameTestHelper helper) {
        EntityType<?>[] types = { ModEntityTypes.TORTOISE.get(), ModEntityTypes.STONE_TORTOISE.get() };
        TortoiseSpecies[] species = TortoiseSpecies.values();
        double[] expectedHealth = { 20.0, 40.0 };
        double[] expectedArmor = { 4.0, 10.0 };
        double[] expectedKnockbackResistance = { 0.0, 1.0 };
        double[] expectedSpeed = { 0.12, 0.10 };
        for (int i = 0; i < types.length; i++) {
            assertEquals(Guzhenren.id(species[i].getId()), BuiltInRegistries.ENTITY_TYPE.getKey(types[i]));
            assertEquals(MobCategory.CREATURE, types[i].getCategory());
            assertEquals(1.3F, types[i].getWidth(), 1.0e-6F);
            assertEquals(0.8F, types[i].getHeight(), 1.0e-6F);

            var attributes = TortoiseEntity.createAttributes(species[i]).build();
            assertEquals(expectedHealth[i], attributes.getBaseValue(Attributes.MAX_HEALTH), 1.0e-6);
            assertEquals(expectedArmor[i], attributes.getBaseValue(Attributes.ARMOR), 1.0e-6);
            assertEquals(expectedKnockbackResistance[i], attributes.getBaseValue(Attributes.KNOCKBACK_RESISTANCE),
                    1.0e-6);
            assertEquals(expectedSpeed[i], attributes.getBaseValue(Attributes.MOVEMENT_SPEED), 1.0e-6);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void wildGuSpawnAboveGroundButNeverInOrOverWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ground = new BlockPos(2, 1, 2);
        BlockPos water = new BlockPos(6, 1, 6);
        helper.setBlock(ground, Blocks.GRASS_BLOCK);
        helper.setBlock(water.below(), Blocks.STONE);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            helper.setBlock(water.relative(side), Blocks.STONE);
        }
        helper.setBlock(water, Blocks.WATER);
        assertTrue(helper.absolutePos(ground).getY() >= level.getChunkSource().getGenerator().getSeaLevel(),
                "the scene must sit at or above sea level");

        EntityType<?>[] types = { ModEntityTypes.HOPE_GU_ENTITY.get(), ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(),
                ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get() };
        for (EntityType<?> type : types) {
            assertTrue(canSpawn(helper, type, ground.above()));
            assertTrue(canSpawn(helper, type, ground.above(4)));
            assertFalse(canSpawn(helper, type, water), "in water");
            assertFalse(canSpawn(helper, type, water.above()), "on the water surface");
            assertFalse(canSpawn(helper, type, water.above(4)), "in the air over water");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void beetleHoverNeedsTheSlowerSpeedToStartAndTheFasterOneToEnd(GameTestHelper helper) {
        double enter = CrashGuEntity.HOVER_ENTER_SPEED;
        double exit = CrashGuEntity.HOVER_EXIT_SPEED;
        //noinspection ConstantValue
        assertTrue(enter < exit);
        assertTrue(CrashGuEntity.isHovering(false, enter * 0.5));
        assertFalse(CrashGuEntity.isHovering(false, (enter + exit) / 2));
        assertTrue(CrashGuEntity.isHovering(true, (enter + exit) / 2));
        assertFalse(CrashGuEntity.isHovering(true, exit));
        assertFalse(CrashGuEntity.isHovering(false, exit * 2));
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void soulTraderRegistrationIsAFireImmuneMiscEntity(GameTestHelper helper) {
        EntityType<SoulTraderEntity> type = ModEntityTypes.TEST_TRADE_GU_IMMORTAL.get();
        assertEquals(Guzhenren.id(SoulTrader.TEST_TRADE_GU_IMMORTAL.getId()),
                BuiltInRegistries.ENTITY_TYPE.getKey(type));
        assertEquals(MobCategory.MISC, type.getCategory());
        assertEquals(0.6F, type.getWidth(), 1.0e-6F);
        assertEquals(1.95F, type.getHeight(), 1.0e-6F);
        assertTrue(type.fireImmune());
        helper.succeed();
    }

    private static boolean canSpawn(GameTestHelper helper, EntityType<?> type, BlockPos pos) {
        return SpawnPlacements.checkSpawnRules(type, helper.getLevel(), MobSpawnType.NATURAL, helper.absolutePos(pos),
                helper.getLevel().getRandom());
    }
}
