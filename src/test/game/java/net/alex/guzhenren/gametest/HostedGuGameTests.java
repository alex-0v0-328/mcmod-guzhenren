package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.ActionEntity;
import net.alex.guzhenren.entity.beast.BearEntity;
import net.alex.guzhenren.entity.beast.BeastEntity;
import net.alex.guzhenren.entity.beast.CattleEntity;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.alex.guzhenren.item.gu.mortal.path.strength.HostedBeastGu;
import net.alex.guzhenren.item.gu.tending.GuHealth;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static net.alex.guzhenren.gametest.BeastScenarios.player;
import static net.alex.guzhenren.gametest.GameTestPlayers.CENTER_POS;
import static net.alex.guzhenren.gametest.GameTestPlayers.PLAYER_MAX_HEALTH;
import static net.alex.guzhenren.gametest.GameTestPlayers.ground;
import static net.alex.guzhenren.gametest.GameTestWorld.firstHit;

/**
 * 野兽寄宿蛊[Hosted Gu]测试：出生掷骰、生命翻倍与存档、伤害 ×1.5、玩家击杀才掉无伤的野生蛊。
 * <ul>
 *   <li>掷骰用定种子随机数，不依赖世界随机；出生接线经 {@link EventHooks#finalizeMobSpawn} 掷 {@value #SPAWNS} 只野猪，
 *       全未寄宿的概率约 10⁻⁹。</li>
 *   <li>掉落各杀 {@value #KILLS} 只寄宿野猪，玩家击杀一只不掉的概率约 6 × 10⁻⁸；非玩家击杀与未寄宿者必不掉。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HostedGuGameTests {

    private static final int ROLLS = 2000;
    private static final int SPAWNS = 200;
    private static final int KILLS = 24;

    private HostedGuGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void spawnRollPicksEachBeastsOwnGu(GameTestHelper helper) {
        WildBoarEntity boar = helper.spawn(ModEntityTypes.WILD_BOAR.get(), CENTER_POS);
        TigerEntity tiger = helper.spawn(ModEntityTypes.WHITE_TIGER.get(), new BlockPos(1, 1, 1));
        BearEntity bear = helper.spawn(ModEntityTypes.ALBINO_BEAR.get(), new BlockPos(7, 1, 1));
        CattleEntity ox = helper.spawn(ModEntityTypes.YELLOW_OX.get(), new BlockPos(1, 1, 7));
        RandomSource random = RandomSource.create(20261114L);

        Map<Item, Integer> boarGu = new HashMap<>();
        for (int i = 0; i < ROLLS; i++) {
            Item gu = HostedBeastGu.roll(boar, random);
            if (gu != null) boarGu.merge(gu, 1, Integer::sum);
            assertRollIs(helper, HostedBeastGu.roll(tiger, random), ModItems.TIGER_STRENGTH_GU_2.get(), "tiger");
            assertRollIs(helper, HostedBeastGu.roll(bear, random), ModItems.BEAR_STRENGTH_GU_1.get(), "bear");
            helper.assertTrue(HostedBeastGu.roll(ox, random) == null, "an ox was hosted");
        }
        helper.assertValueEqual(boarGu.keySet(), Set.of(ModItems.WHITE_BOAR_GU_1.get(), ModItems.BLACK_BOAR_GU_1.get()),
                "boar hosted Gu kinds");
        int hosted = boarGu.values().stream().mapToInt(Integer::intValue).sum();
        helper.assertTrue(hosted >= ROLLS * 0.08D && hosted <= ROLLS * 0.12D,
                "boar host rate outside 8-12%: " + hosted + " of " + ROLLS);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void finalizedSpawnHostsSomeBoarsAtDoubleHealth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DifficultyInstance difficulty = level.getCurrentDifficultyAt(helper.absolutePos(CENTER_POS));
        int hosted = 0;
        for (int i = 0; i < SPAWNS; i++) {
            WildBoarEntity boar = ModEntityTypes.WILD_BOAR.get().create(level);
            helper.assertTrue(boar != null, "wild boar was not created");
            EventHooks.finalizeMobSpawn(boar, level, difficulty, MobSpawnType.NATURAL, null);
            float max = boar.getHostedGu() == null ? 30.0F : 60.0F;
            if (boar.getHostedGu() != null) hosted++;
            helper.assertValueEqual(boar.getMaxHealth(), max, "boar max health");
            helper.assertValueEqual(boar.getHealth(), max, "boar spawn health");
        }
        helper.assertTrue(hosted >= 1 && hosted <= SPAWNS / 4, "finalized boars hosted: " + hosted);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void hostedTigerKeepsDoubleHealthAcrossSave(GameTestHelper helper) {
        TigerEntity tiger = helper.spawn(ModEntityTypes.TIGER.get(), CENTER_POS);
        tiger.hostGu(ModItems.TIGER_STRENGTH_GU_2.get());
        helper.assertValueEqual(tiger.getMaxHealth(), 96.0F, "hosted tiger max health");
        helper.assertValueEqual(tiger.getHealth(), 96.0F, "hosted tiger health");
        tiger.setHealth(70.0F);
        CompoundTag saved = tiger.saveWithoutId(new CompoundTag());
        tiger.discard();

        TigerEntity restored = helper.spawn(ModEntityTypes.TIGER.get(), new BlockPos(6, 1, 6));
        restored.load(saved);
        helper.assertValueEqual(restored.getMaxHealth(), 96.0F, "reloaded hosted tiger max health");
        helper.assertValueEqual(restored.getHealth(), 70.0F, "reloaded hosted tiger health");
        helper.assertTrue(restored.getHostedGu() == ModItems.TIGER_STRENGTH_GU_2.get(),
                "reloaded tiger lost its hosted Gu");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void summonTagHostsAtFullDoubleHealth(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", EntityType.getKey(ModEntityTypes.BROWN_BEAR.get()).toString());
        tag.putString("HostedGu", "guzhenren:bear_strength_gu_1");
        Entity summoned = EntityType.loadEntityRecursive(tag, helper.getLevel(), entity -> entity);
        helper.assertTrue(summoned instanceof BearEntity, "summon tag did not load a bear");
        BearEntity bear = (BearEntity) summoned;
        helper.assertValueEqual(bear.getMaxHealth(), 108.0F, "summoned hosted brown bear max health");
        helper.assertValueEqual(bear.getHealth(), 108.0F, "summoned hosted brown bear health");
        helper.assertTrue(bear.getHostedGu() == ModItems.BEAR_STRENGTH_GU_1.get(), "summoned bear hosted Gu");
        bear.discard();
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 60)
    public static void hostedTigerSwipeDealsHalfAgain(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 2.5D);
        TigerEntity tiger = helper.spawn(ModEntityTypes.TIGER.get(), CENTER_POS);
        tiger.hostGu(ModItems.TIGER_STRENGTH_GU_2.get());
        tiger.setTarget(player);
        tiger.tryStartAction(BeastEntity.Action.ATTACK_SWIPE);
        int[] hitTick = firstHit(helper, player, () -> (int) tiger.getActionTicks());
        helper.runAtTickTime(12L, () -> {
            helper.assertValueEqual(hitTick[0], TigerEntity.SWIPE_HIT_TICK, "hosted tiger swipe hit tick");
            helper.assertValueEqual(player.getHealth(),
                    PLAYER_MAX_HEALTH - TigerEntity.SWIPE_DAMAGE * ActionEntity.HOSTED_DAMAGE_MULTIPLIER,
                    "hosted tiger swipe damage");
            helper.succeed();
        });
    }

    @GameTest(template = "empty9x9x9")
    public static void hostedBoarDropsSoundGuOnlyToPlayerKills(GameTestHelper helper) {
        ground(helper);
        ServerPlayer player = player(helper, 4.5D, 2.5D);
        DamageSource byPlayer = helper.getLevel().damageSources().playerAttack(player);
        DamageSource byWorld = helper.getLevel().damageSources().generic();
        Set<Item> boarGu = Set.of(ModItems.WHITE_BOAR_GU_1.get(), ModItems.BLACK_BOAR_GU_1.get());

        List<ItemStack> playerKills = killBoars(helper, true, byPlayer);
        helper.assertTrue(!playerKills.isEmpty(), "no hosted boar dropped its Gu in " + KILLS + " player kills");
        for (ItemStack stack : playerKills) {
            helper.assertTrue(boarGu.contains(stack.getItem()), "hosted boar dropped " + stack.getItem());
            TendedGuItem gu = (TendedGuItem) stack.getItem();
            helper.assertTrue(GuHealth.get(stack) == null, "dropped boar Gu was wounded");
            helper.assertTrue(!gu.isRefined(stack), "dropped boar Gu was refined");
        }
        helper.assertTrue(killBoars(helper, true, byWorld).isEmpty(), "a non-player kill dropped a Gu");
        helper.assertTrue(killBoars(helper, false, byPlayer).isEmpty(), "an unhosted boar dropped a Gu");
        helper.succeed();
    }

    private static void assertRollIs(GameTestHelper helper, Item rolled, Item expected, String name) {
        helper.assertTrue(rolled == null || rolled == expected, name + " hosted " + rolled);
    }

    private static List<ItemStack> killBoars(GameTestHelper helper, boolean hosted, DamageSource killer) {
        List<ItemStack> dropped = new ArrayList<>();
        for (int i = 0; i < KILLS; i++) {
            WildBoarEntity boar = helper.spawn(ModEntityTypes.WILD_BOAR.get(), CENTER_POS);
            if (hosted) boar.hostGu(i % 2 == 0 ? ModItems.WHITE_BOAR_GU_1.get() : ModItems.BLACK_BOAR_GU_1.get());
            for (Entity added : GameTestWorld.entitiesAddedBy(helper, () -> boar.hurt(killer, Float.MAX_VALUE))) {
                if (added instanceof ItemEntity item && item.getItem().getItem() instanceof TendedGuItem) {
                    dropped.add(item.getItem());
                }
            }
        }
        return dropped;
    }
}
