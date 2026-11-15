package net.alex.guzhenren.gametest;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.gu.BoarGuEntity;
import net.alex.guzhenren.entity.gu.FlyingGuEntity;
import net.alex.guzhenren.entity.gu.HopeGuEntity;
import net.alex.guzhenren.entity.gu.WildGuEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static net.alex.guzhenren.gametest.GameTestApertures.mortal;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;

/**
 * 野生蛊实体测试。希望蛊[Hope Gu]寻人窗口；豕蛊[Boar Gu]游荡、逃跑、徒手捕捉；wild_gu 标签恰列全部 WildGuEntity 类型。
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildGuEntityGameTests {

    private WildGuEntityGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void hopeGuSeeksUnawakenedPlayer(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        Vec3 spot = helper.absoluteVec(new Vec3(4.5D, 1.5D, 0.5D));
        player.moveTo(spot.x, spot.y, spot.z, 0.0F, 0.0F);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);

        HopeGuEntity gu = helper.spawn(ModEntityTypes.HOPE_GU_ENTITY.get(), CENTER_POS);
        Vec3 start = gu.position();
        Vec3 towardPlayer = player.position().subtract(start);
        double startDistance = towardPlayer.length();
        helper.assertTrue(startDistance > FlyingGuEntity.HOVER_RANGE, "player started inside hover range");
        helper.assertTrue(startDistance <= FlyingGuEntity.DETECT_RANGE, "player started outside detect range");

        helper.succeedWhen(() -> {
            helper.assertTrue(gu.isAlive(), "hope gu vanished");
            Vec3 travel = gu.position().subtract(start);
            helper.assertTrue(travel.dot(towardPlayer) > 1.0D, "hope gu did not fly toward the player");
            helper.assertTrue(gu.distanceTo(player) < startDistance - 1.0D, "hope gu distance did not shrink");
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 1000)
    public static void boarGuVariantsWanderWithoutSeekingPlayers(GameTestHelper helper) {
        GameTestPlayers.survival(helper, "gzr-gametest-mock").unconnected().build();
        List<BoarGuEntity> variants = List.of(
                helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), new BlockPos(2, 1, 2)),
                helper.spawn(ModEntityTypes.BLACK_BOAR_GU_ENTITY.get(), new BlockPos(4, 1, 2)),
                helper.spawn(ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get(), new BlockPos(6, 1, 2)));
        List<Vec3> starts = variants.stream().map(BoarGuEntity::position).toList();
        for (BoarGuEntity variant : variants) {
            variant.getRandom().setSeed(42L);
            variant.setYRot(0.0F);
        }
        boolean[] horizontal = new boolean[variants.size()];
        boolean[] vertical = new boolean[variants.size()];
        helper.succeedWhen(() -> {
            for (int i = 0; i < variants.size(); i++) {
                Vec3 travel = variants.get(i).position().subtract(starts.get(i));
                horizontal[i] |= Math.sqrt(travel.x * travel.x + travel.z * travel.z) > 0.05D;
                vertical[i] |= Math.abs(travel.y) > 0.05D;
            }
            for (int i = 0; i < variants.size(); i++) {
                helper.assertTrue(horizontal[i], "boar gu did not wander horizontally: " + i);
                helper.assertTrue(vertical[i], "boar gu did not wander vertically: " + i);
            }
        });
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 200)
    public static void boarGuFleesNearbyPlayer(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5D, 1.5D, 1.5D)));
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);

        BoarGuEntity gu = helper.spawn(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), CENTER_POS);
        helper.assertTrue(gu.distanceTo(player) <= BoarGuEntity.FLEE_RANGE, "player started outside flee range");

        helper.succeedWhen(() -> helper.assertTrue(gu.distanceTo(player) > BoarGuEntity.FLEE_RANGE,
                "boar gu stayed within flee range of the player"));
    }

    @GameTest(template = "empty9x9x9")
    public static void boarGuVariantsCatchAsMatchingItems(GameTestHelper helper) {
        ServerPlayer player = mortal(helper);
        catchBoar(helper, player, ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), ModItems.WHITE_BOAR_GU_1.get());
        catchBoar(helper, player, ModEntityTypes.BLACK_BOAR_GU_ENTITY.get(), ModItems.BLACK_BOAR_GU_1.get());
        catchBoar(helper, player, ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get(), ModItems.FLOWER_BOAR_GU_1.get());
        helper.succeed();
    }

    private static void catchBoar(GameTestHelper helper, ServerPlayer player, EntityType<BoarGuEntity> type,
                                  Item item) {
        BoarGuEntity gu = helper.spawn(type, CENTER_POS);
        List<Entity> added = GameTestWorld.entitiesAddedBy(helper, () -> helper.assertTrue(
                gu.interact(player, InteractionHand.MAIN_HAND).consumesAction(),
                "boar gu interaction was not consumed"));
        helper.assertValueEqual(player.getInventory().countItem(item), 1, "matching gu item missing");
        helper.assertTrue(!gu.isAlive(), "caught boar gu was not discarded");
        helper.assertTrue(added.stream().noneMatch(ItemEntity.class::isInstance),
                "caught boar gu dropped an item entity");
    }

    @GameTest(template = "empty9x9x9")
    public static void wildGuTagNamesExactlyTheWildGuTypes(GameTestHelper helper) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!Guzhenren.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) continue;

            Entity entity = type.create(helper.getLevel());
            if (entity == null) continue;

            boolean wild = entity instanceof WildGuEntity;
            entity.discard();
            helper.assertTrue(wild == type.is(ModEntityTypeTags.WILD_GU),
                    BuiltInRegistries.ENTITY_TYPE.getKey(type) + (wild ? " is a wild gu outside the tag" : " is tagged wild_gu but is not one"));
        }
        helper.succeed();
    }
}
