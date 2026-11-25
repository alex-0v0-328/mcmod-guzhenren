package net.alex.guzhenren.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import net.alex.guzhenren.entity.beast.BeastPhantomVisual;
import net.alex.guzhenren.entity.gu.CrashGuEntity;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.trade.SoulTraderEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.bernie.geckolib.animation.Animation.LoopType;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 各 GeckoLib 生物的打包资源契约，每实体一嵌套组；各组读同一 jar 布局，故共用辅助方法；注册与甲虫悬停选择需加载模组，见
 * {@code EntityRegistrationGameTests}。
 */

class EntityResourcesTest {

    private static final String ROOT = "/assets/guzhenren/";

    @Nested
    class WildBoar {

        @Test
        void modelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
            BakedGeoModel baked = model("wild_boar", 35, 25);
            assertClips(animations("wild_boar", baked), Map.ofEntries(
                    Map.entry("idle", loop(120.0)),
                    Map.entry("walk", loop(28.0)),
                    Map.entry("run", loop(12.0)),
                    Map.entry("charge", loop(16.0)),
                    Map.entry("graze", loop(100.0)),
                    Map.entry("alert", once(16.0)),
                    Map.entry("attack_charge", once(24.0)),
                    Map.entry("attack_toss", once(16.0)),
                    Map.entry("hurt", once(7.0)),
                    Map.entry("death", hold(28.0)),
                    Map.entry("appear", once(20.0))));
        }

        @Test
        void textureIsTheExpectedNearestNeighborCutoutTexture() throws IOException {
            assertCutoutTexture("wild_boar");
        }

        @Test
        void generatedSpawnTagContainsTheForestBiomes() throws IOException {
            List<String> biomes = tagEntries("tags/worldgen/biome/wild_boar_spawns.json");
            assertEquals(List.of("minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest",
                            "minecraft:old_growth_birch_forest", "minecraft:dark_forest"), biomes);
        }

        @Test
        void generatedSpawnModifierUsesTheWildBoarWeightAndPackSize() throws IOException {
            assertSpawnModifier("#guzhenren:wild_boar_spawns", "spawn_wild_boar", "guzhenren:wild_boar", 8, 3);
        }

        @Test
        void generatedLootUsesVanillaPorkSmeltingAndLootingFunctions() throws IOException {
            JsonObject loot = dataJson("loot_table/entities/wild_boar.json");
            JsonObject pool = loot.getAsJsonArray("pools").get(0).getAsJsonObject();
            JsonObject entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("minecraft:item", entry.get("type").getAsString());
            assertEquals("minecraft:porkchop", entry.get("name").getAsString());

            Map<String, JsonObject> functions = new HashMap<>();
            for (JsonElement function : entry.getAsJsonArray("functions")) {
                functions.putIfAbsent(function.getAsJsonObject().get("function").getAsString(),
                        function.getAsJsonObject());
            }
            assertTrue(functions.containsKey("minecraft:furnace_smelt"));
            assertTrue(functions.containsKey("minecraft:enchanted_count_increase"));
            assertNotNull(functions.get("minecraft:set_count"), "no set_count function");
            JsonObject uniform = functions.get("minecraft:set_count").getAsJsonObject("count");
            assertEquals("minecraft:uniform", uniform.get("type").getAsString());
            assertEquals(1.0, uniform.get("min").getAsDouble(), 1.0e-6);
            assertEquals(3.0, uniform.get("max").getAsDouble(), 1.0e-6);
        }
    }

    @Nested
    class Bear {

        @Test
        void modelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
            BakedGeoModel baked = model("bear", 20, 22);
            assertClips(animations("bear", baked), Map.ofEntries(
                    Map.entry("animation.idle", loop(120.0)),
                    Map.entry("animation.walk", loop(36.0)),
                    Map.entry("animation.run", loop(14.0)),
                    Map.entry("animation.charge", loop(16.0)),
                    Map.entry("animation.attack_swipe", once(20.0)),
                    Map.entry("animation.attack_rear", once(36.0)),
                    Map.entry("animation.roar", once(52.0)),
                    Map.entry("animation.back_scratch", once(100.0)),
                    Map.entry("animation.sit", loop(80.0)),
                    Map.entry("animation.lie_down", hold(28.0)),
                    Map.entry("animation.lie", loop(120.0)),
                    Map.entry("animation.sleep", loop(120.0)),
                    Map.entry("animation.get_up", once(24.0)),
                    Map.entry("animation.roll", once(74.0)),
                    Map.entry("animation.hurt_left", once(11.0)),
                    Map.entry("animation.hurt_right", once(11.0)),
                    Map.entry("animation.death", hold(36.0)),
                    Map.entry("animation.appear", once(20.0))));
        }

        @Test
        void texturesAreTheExpectedNearestNeighborCutoutTextures() throws IOException {
            for (String name : List.of("brown_bear", "asian_black_bear", "american_black_bear", "albino_bear")) {
                assertCutoutTexture(name);
            }
        }

        @Test
        void generatedSpawnTagContainsEveryForestFamilyBiome() throws IOException {
            List<String> biomes = tagEntries("tags/worldgen/biome/bear_spawns.json");
            assertEquals(List.of("minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest",
                            "minecraft:old_growth_birch_forest", "minecraft:dark_forest", "minecraft:taiga",
                            "minecraft:snowy_taiga", "minecraft:old_growth_pine_taiga",
                            "minecraft:old_growth_spruce_taiga", "minecraft:jungle", "minecraft:sparse_jungle",
                            "minecraft:bamboo_jungle"), biomes);
        }

        @Test
        void generatedSpawnModifiersUseTheSpeciesWeights() throws IOException {
            assertSpawnModifier("#guzhenren:bear_spawns", "spawn_brown_bear", "guzhenren:brown_bear", 4, 1);
            assertSpawnModifier("#guzhenren:bear_spawns", "spawn_asian_black_bear", "guzhenren:asian_black_bear",
                    4, 1);
            assertSpawnModifier("#guzhenren:bear_spawns", "spawn_american_black_bear",
                    "guzhenren:american_black_bear", 4, 1);
            assertSpawnModifier("#guzhenren:bear_spawns", "spawn_albino_bear", "guzhenren:albino_bear", 1, 1);
        }

        @Test
        void generatedPreyTagListsOnlyVanillaFarmAndSmallAnimals() throws IOException {
            assertEquals(List.of("minecraft:pig", "minecraft:cow", "minecraft:sheep", "minecraft:chicken",
                    "minecraft:rabbit"), tagEntries("tags/entity_type/predator_prey.json"));
        }
    }

    @Nested
    class Tiger {

        @Test
        void modelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
            BakedGeoModel baked = model("tiger", 21, 23);
            assertClips(animations("tiger", baked), Map.ofEntries(
                    Map.entry("animation.idle", loop(120.0)),
                    Map.entry("animation.walk", loop(32.0)),
                    Map.entry("animation.run", loop(12.0)),
                    Map.entry("animation.charge", loop(16.0)),
                    Map.entry("animation.attack_pounce", once(28.0)),
                    Map.entry("animation.attack_swipe", once(16.0)),
                    Map.entry("animation.roar", once(40.0)),
                    Map.entry("animation.lie_down", hold(24.0)),
                    Map.entry("animation.lie", loop(120.0)),
                    Map.entry("animation.sleep", loop(120.0)),
                    Map.entry("animation.get_up", once(20.0)),
                    Map.entry("animation.sit", loop(80.0)),
                    Map.entry("animation.hurt_left", once(11.0)),
                    Map.entry("animation.hurt_right", once(11.0)),
                    Map.entry("animation.death", hold(32.0)),
                    Map.entry("animation.appear", once(20.0))));
        }

        @Test
        void texturesAreTheExpectedNearestNeighborCutoutTextures() throws IOException {
            assertCutoutTexture("tiger");
            assertCutoutTexture("white_tiger");
        }

        @Test
        void generatedSpawnTagContainsTheJungleBiomes() throws IOException {
            List<String> biomes = tagEntries("tags/worldgen/biome/tiger_spawns.json");
            assertEquals(List.of("minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle"), biomes);
        }

        @Test
        void generatedSpawnModifiersUseTheCoatWeights() throws IOException {
            assertSpawnModifier("#guzhenren:tiger_spawns", "spawn_tiger", "guzhenren:tiger", 3, 1);
            assertSpawnModifier("#guzhenren:tiger_spawns", "spawn_white_tiger", "guzhenren:white_tiger", 1, 1);
        }
    }

    @Nested
    class Tortoise {

        @Test
        void modelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
            BakedGeoModel baked = model("tortoise", 8, 8);
            assertClips(animations("tortoise", baked), Map.of(
                    "animation.idle", loop(120.0),
                    "animation.walk", loop(40.0),
                    "animation.sleep", loop(120.0),
                    "animation.death", hold(32.0),
                    "animation.hurt", once(10.0),
                    "animation.eat", once(40.0),
                    "animation.charge", loop(16.0)));
        }

        @Test
        void texturesAreTheExpectedNearestNeighborCutoutTextures() throws IOException {
            assertCutoutTexture("tortoise");
            assertCutoutTexture("stone_tortoise");
        }

        @Test
        void generatedSpawnTagContainsTheSavannasAndTheSparseJungle() throws IOException {
            List<String> biomes = tagEntries("tags/worldgen/biome/tortoise_spawns.json");
            assertEquals(List.of("minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna",
                    "minecraft:sparse_jungle"), biomes);
        }

        @Test
        void generatedSpawnModifiersUseTheSpeciesWeights() throws IOException {
            assertSpawnModifier("#guzhenren:tortoise_spawns", "spawn_tortoise", "guzhenren:tortoise", 4, 2);
            assertSpawnModifier("#guzhenren:tortoise_spawns", "spawn_stone_tortoise", "guzhenren:stone_tortoise",
                    1, 1);
        }
    }

    @Nested
    class Cattle {

        @Test
        void modelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
            BakedGeoModel baked = model("buffalo", 21, 18);
            assertClips(animations("buffalo", baked), Map.ofEntries(
                    Map.entry("animation.idle", loop(120.0)),
                    Map.entry("animation.walk", loop(20.0)),
                    Map.entry("animation.run", loop(14.0)),
                    Map.entry("animation.graze", loop(100.0)),
                    Map.entry("animation.alert", once(16.0)),
                    Map.entry("animation.attack_charge", once(24.0)),
                    Map.entry("animation.attack_toss", once(16.0)),
                    Map.entry("animation.hurt", once(8.0)),
                    Map.entry("animation.death", hold(32.0)),
                    Map.entry("animation.lie_down", hold(32.0)),
                    Map.entry("animation.lie", loop(120.0)),
                    Map.entry("animation.get_up", once(28.0)),
                    Map.entry("animation.charge", loop(16.0)),
                    Map.entry("animation.appear", once(20.0))));
        }

        @Test
        void texturesAreTheExpectedNearestNeighborCutoutTextures() throws IOException {
            for (String name : List.of("yellow_ox", "water_buffalo", "azure_ox")) {
                assertCutoutTexture(name);
            }
        }

        @Test
        void generatedSpawnTagContainsEveryVanillaCowBiome() throws IOException {
            List<String> biomes = tagEntries("tags/worldgen/biome/cattle_spawns.json");
            assertEquals(List.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:forest",
                    "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                    "minecraft:dark_forest", "minecraft:taiga", "minecraft:snowy_taiga",
                    "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                    "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
                    "minecraft:windswept_savanna", "minecraft:savanna", "minecraft:savanna_plateau",
                    "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle", "minecraft:swamp"),
                    biomes);
        }

        @Test
        void generatedSpawnModifiersUseTheSpeciesWeightsAndPacks() throws IOException {
            JsonObject yellow = dataJson("neoforge/biome_modifier/spawn_yellow_ox.json").getAsJsonObject("spawners");
            assertEquals(2, yellow.get("minCount").getAsInt());
            assertEquals(4, yellow.get("maxCount").getAsInt());
            assertEquals(4, yellow.get("weight").getAsInt());
            assertEquals("guzhenren:yellow_ox", yellow.get("type").getAsString());
            assertSpawnModifier("#guzhenren:cattle_spawns", "spawn_water_buffalo", "guzhenren:water_buffalo", 2, 3);
            assertSpawnModifier("#guzhenren:cattle_spawns", "spawn_azure_ox", "guzhenren:azure_ox", 1, 1);
        }

        @Test
        void generatedLootDropsLeatherAndBeefWithVanillaFunctions() throws IOException {
            for (String name : List.of("yellow_ox", "water_buffalo", "azure_ox")) {
                JsonObject loot = dataJson("loot_table/entities/" + name + ".json");
                JsonObject leather = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                        .getAsJsonArray("entries").get(0).getAsJsonObject();
                JsonObject beef = loot.getAsJsonArray("pools").get(1).getAsJsonObject()
                        .getAsJsonArray("entries").get(0).getAsJsonObject();
                assertEquals("minecraft:leather", leather.get("name").getAsString(), name);
                assertEquals("minecraft:beef", beef.get("name").getAsString(), name);
                List<String> functions = new ArrayList<>();
                for (JsonElement function : beef.getAsJsonArray("functions")) {
                    functions.add(function.getAsJsonObject().get("function").getAsString());
                }
                assertEquals(List.of("minecraft:set_count", "minecraft:furnace_smelt",
                        "minecraft:enchanted_count_increase"), functions, name);
            }
        }
    }

    @Nested
    class Elephant {

        @Test
        void modelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
            BakedGeoModel baked = model("elephant", 21, 19);
            assertClips(animations("elephant", baked), Map.ofEntries(
                    Map.entry("animation.idle", loop(120.0)),
                    Map.entry("animation.walk", loop(28.0)),
                    Map.entry("animation.run", loop(16.0)),
                    Map.entry("animation.graze", loop(100.0)),
                    Map.entry("animation.alert", once(16.0)),
                    Map.entry("animation.attack_charge", once(24.0)),
                    Map.entry("animation.attack_toss", once(16.0)),
                    Map.entry("animation.attack_stomp", once(24.0)),
                    Map.entry("animation.attack_trunk", once(20.0)),
                    Map.entry("animation.hurt", once(8.0)),
                    Map.entry("animation.death", hold(32.0)),
                    Map.entry("animation.lie_down", hold(36.0)),
                    Map.entry("animation.lie", loop(120.0)),
                    Map.entry("animation.get_up", once(32.0)),
                    Map.entry("animation.charge", loop(16.0))));
        }

        @Test
        void texturesAreTheExpectedNearestNeighborCutoutTextures() throws IOException {
            for (String name : List.of("elephant", "white_elephant")) {
                assertCutoutTexture(name);
            }
        }

        @Test
        void generatedSpawnTagContainsTheSavannasAndTheJungles() throws IOException {
            assertEquals(List.of("minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna",
                    "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle"),
                    tagEntries("tags/worldgen/biome/elephant_spawns.json"));
        }

        @Test
        void generatedSpawnModifiersUseTheHerdAndTheRareWhiteElephant() throws IOException {
            JsonObject grey = dataJson("neoforge/biome_modifier/spawn_elephant.json").getAsJsonObject("spawners");
            assertEquals(2, grey.get("minCount").getAsInt());
            assertEquals(3, grey.get("maxCount").getAsInt());
            assertEquals(2, grey.get("weight").getAsInt());
            assertEquals("guzhenren:elephant", grey.get("type").getAsString());
            assertSpawnModifier("#guzhenren:elephant_spawns", "spawn_white_elephant", "guzhenren:white_elephant",
                    1, 1);
        }

        @Test
        void generatedLootDropsOnlyLeatherWithLooting() throws IOException {
            for (String name : List.of("elephant", "white_elephant")) {
                JsonObject loot = dataJson("loot_table/entities/" + name + ".json");
                assertEquals(1, loot.getAsJsonArray("pools").size(), name);
                JsonObject leather = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                        .getAsJsonArray("entries").get(0).getAsJsonObject();
                assertEquals("minecraft:leather", leather.get("name").getAsString(), name);
                JsonObject count = leather.getAsJsonArray("functions").get(0).getAsJsonObject()
                        .getAsJsonObject("count");
                assertEquals(2.0, count.get("min").getAsDouble(), name);
                assertEquals(4.0, count.get("max").getAsDouble(), name);
                assertEquals("minecraft:enchanted_count_increase", leather.getAsJsonArray("functions").get(1)
                        .getAsJsonObject().get("function").getAsString(), name);
            }
        }
    }

    @Nested
    class BeastPhantom {

        @Test
        void phantomsAppearOnceThenIdleAndChargesLoopOnClipsTheirModelsHave() throws IOException {
            // The horse is posed on vanilla's horse model and has no animation file.
            Map<BeastStrengthFamily, String> models = Map.of(
                    BeastStrengthFamily.BOAR, "wild_boar",
                    BeastStrengthFamily.OX, "buffalo",
                    BeastStrengthFamily.BEAR, "bear",
                    BeastStrengthFamily.TIGER, "tiger",
                    BeastStrengthFamily.TURTLE, "tortoise");
            for (var entry : models.entrySet()) {
                BeastStrengthFamily family = entry.getKey();
                String prefix = family == BeastStrengthFamily.BOAR ? "" : "animation.";
                List<RawAnimation.Stage> phantom = BeastPhantomVisual.getAppear(family, "idle").getAnimationStages();
                List<RawAnimation.Stage> charge = BeastPhantomVisual.getLoop(family, "charge").getAnimationStages();
                List<String> expected = family == BeastStrengthFamily.TURTLE
                        ? List.of(prefix + "idle") : List.of(prefix + "appear", prefix + "idle");
                assertEquals(expected, phantom.stream().map(RawAnimation.Stage::animationName).toList(),
                        family.name());
                assertSame(LoopType.LOOP, phantom.getLast().loopType(), family.name());
                // thenPlay defers to the clip's own loop, which the clip tables above pin to play once.
                if (phantom.size() > 1) assertSame(LoopType.DEFAULT, phantom.getFirst().loopType(), family.name());
                assertEquals(List.of(prefix + "charge"),
                        charge.stream().map(RawAnimation.Stage::animationName).toList(), family.name());

                Set<String> clips = json("animations/entity/" + entry.getValue() + ".animation.json")
                        .getAsJsonObject("animations").keySet();
                List<RawAnimation.Stage> stages = new ArrayList<>(phantom);
                stages.addAll(charge);
                for (RawAnimation.Stage stage : stages) {
                    assertTrue(clips.contains(stage.animationName()), family + " plays " + stage.animationName());
                }
            }
        }
    }

    @Nested
    class BoarGu {

        @Test
        void ladybugModelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
            BakedGeoModel baked = model("boar_gu", 8, 7);
            for (String bone : List.of("body", "head", "shell_left", "shell_right", "antenna_left", "antenna_right")) {
                assertTrue(baked.getBone(bone).isPresent(), bone);
            }
            BakedAnimations animations = animations("boar_gu", baked);
            assertEquals(Set.of("animation.idle", "animation.lift", "animation.fly", "animation.land"),
                    animations.animations().keySet());
            for (var animation : animations.animations().values()) {
                assertTrue(Double.isFinite(animation.length()) && animation.length() > 0, animation.name());
            }
        }

        @Test
        void allThreeEntityTexturesDecodeAtTheModelUvResolution() throws IOException {
            for (String variant : List.of("white_boar_gu", "black_boar_gu", "flower_boar_gu")) {
                texture(variant, 64);
            }
        }
    }

    @Nested
    class RhinocerosBeetle {

        @Test
        void modelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
            BakedGeoModel baked = model("rhinoceros_beetle", 25, 21);
            assertTrue(baked.getBone("wing_left").isEmpty());
            assertTrue(baked.getBone("wing_right").isEmpty());
            assertClips(animations("rhinoceros_beetle", baked), Map.of(
                    "animation.idle", loop(80.0),
                    "animation.walk", anyLoop(24.0),
                    "animation.lift", once(16.0),
                    "animation.fly", loop(8.0),
                    "animation.hover", loop(8.0),
                    "animation.land", once(16.0),
                    "animation.hurt", once(6.0),
                    "animation.death", hold(CrashGuEntity.DEATH_TICKS)));
        }

        @Test
        void theThreeRankTexturesDecodeAtTheModelUvResolution() throws IOException {
            for (String variant : List.of("crash_gu", "charging_crash_gu", "charging_crash_gu_5")) {
                texture(variant, 64);
            }
            for (String retired : List.of("horizontal_crash_gu", "vertical_crash_gu")) {
                assertNull(EntityResourcesTest.class.getResourceAsStream(ROOT + "textures/entity/" + retired + ".png"),
                        retired + " should be retired");
            }
        }
    }

    @Nested
    class HumanSoul {

        @Test
        void modelAndEveryAnimationBakeWithTheInstalledGeckoLibVersion() throws IOException {
            BakedGeoModel baked = model("human_soul", 28, 15);
            assertTrue(baked.getBone("head").isPresent());
            BakedAnimations animations = animations("human_soul", baked);
            assertEquals(Set.of("animation.idle", "animation.fly_forward", "animation.fly_backward",
                            "animation.ascend", "animation.descend", "animation.turn_left", "animation.turn_right",
                            "animation.reach", "animation.beckon", "animation.cross_arms_nod",
                            "animation.hands_hips_shake", "animation.appear", "animation.disappear"),
                    animations.animations().keySet());
            assertEquals(LoopType.LOOP, animations.getAnimation("animation.idle").loopType());
            assertClip(animations, "animation.beckon", once(SoulTraderEntity.BECKON_TICKS));
            assertClip(animations, "animation.cross_arms_nod", once(SoulTraderEntity.REACTION_TICKS));
            assertClip(animations, "animation.hands_hips_shake", once(SoulTraderEntity.REACTION_TICKS));
            assertClip(animations, "animation.appear", once(SoulTraderEntity.APPEAR_TICKS));
            assertClip(animations, "animation.disappear", hold(SoulTraderEntity.DISAPPEAR_TICKS));
        }

        @Test
        void blueTextureAndGlowMaskShareTheEyePixels() throws IOException {
            BufferedImage blue = texture("human_soul_blue", 128);
            BufferedImage glow = texture("human_soul_glowmask", 128);
            Set<Integer> eyes = Set.of(11 * 128 + 74, 11 * 128 + 77, 12 * 128 + 74, 12 * 128 + 77);
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    int expected = eyes.contains(y * 128 + x) ? 0xFFFFFFFF : 0;
                    assertEquals(expected, glow.getRGB(x, y), "glow mask pixel " + x + "," + y);
                    if (expected != 0) assertEquals(0xFFFFFFFF, blue.getRGB(x, y), "blue eye pixel " + x + "," + y);
                }
            }
        }
    }

    /** 单个动画的预期循环类型（null 为不钉）与时长（tick）。 */
    private record Clip(LoopType loop, double length) {}

    private static Clip loop(double length) { return new Clip(LoopType.LOOP, length); }

    private static Clip once(double length) { return new Clip(LoopType.PLAY_ONCE, length); }

    private static Clip hold(double length) { return new Clip(LoopType.HOLD_ON_LAST_FRAME, length); }

    /** 不钉循环类型的动画片段。 */
    private static Clip anyLoop(double length) { return new Clip(null, length); }

    /** 烘焙 {@code geo/entity/<name>.geo.json}，钉住标识符、立方体数、骨骼数。 */
    private static BakedGeoModel model(String name, int cubes, int bones) throws IOException {
        Model model = KeyFramesAdapter.GEO_GSON.fromJson(json("geo/entity/" + name + ".geo.json"), Model.class);
        BakedGeoModel baked = BakedModelFactory.getForNamespace("guzhenren")
                .constructGeoModel(GeometryTree.fromModel(model));
        assertEquals("geometry." + name, baked.properties().identifier());
        assertEquals(cubes, cubeCount(baked.topLevelBones()));
        assertEquals(bones, boneCount(baked.topLevelBones()));
        return baked;
    }

    /** 烘焙实体动画；每个至少驱动一根骨骼，且只驱动模型已有骨骼。 */
    private static BakedAnimations animations(String name, BakedGeoModel baked) throws IOException {
        BakedAnimations animations = KeyFramesAdapter.GEO_GSON.fromJson(
                json("animations/entity/" + name + ".animation.json").getAsJsonObject("animations"),
                BakedAnimations.class);
        for (var animation : animations.animations().values()) {
            assertTrue(animation.boneAnimations().length > 0, animation.name());
            for (var bone : animation.boneAnimations()) {
                assertTrue(baked.getBone(bone.boneName()).isPresent(),
                        animation.name() + " references missing bone " + bone.boneName());
            }
        }
        return animations;
    }

    private static void assertClips(BakedAnimations animations, Map<String, Clip> expected) {
        assertEquals(expected.keySet(), animations.animations().keySet());
        expected.forEach((name, clip) -> assertClip(animations, name, clip));
    }

    private static void assertClip(BakedAnimations animations, String name, Clip clip) {
        if (clip.loop() != null) assertEquals(clip.loop(), animations.getAnimation(name).loopType(), name);
        assertEquals(clip.length(), animations.getAnimation(name).length(), 1.0e-6, name);
    }

    /** 128x128 贴图，像素全透明或全不透明，采样不模糊、不钳制。 */
    private static void assertCutoutTexture(String name) throws IOException {
        BufferedImage texture = texture(name, 128);
        for (int y = 0; y < texture.getHeight(); y++) {
            for (int x = 0; x < texture.getWidth(); x++) {
                int alpha = (texture.getRGB(x, y) >>> 24) & 0xff;
                assertTrue(alpha == 0 || alpha == 0xff, name + " non-binary alpha at " + x + "," + y + ": " + alpha);
            }
        }
        JsonObject mcmeta = json("textures/entity/" + name + ".png.mcmeta").getAsJsonObject("texture");
        assertFalse(mcmeta.get("blur").getAsBoolean(), name);
        assertFalse(mcmeta.get("clamp").getAsBoolean(), name);
    }

    private static BufferedImage texture(String name, int size) throws IOException {
        try (InputStream input = resource("textures/entity/" + name + ".png")) {
            BufferedImage texture = ImageIO.read(input);
            assertNotNull(texture, name);
            assertEquals(size, texture.getWidth(), name);
            assertEquals(size, texture.getHeight(), name);
            return texture;
        }
    }

    private static List<String> tagEntries(String path) throws IOException {
        List<String> entries = new ArrayList<>();
        for (JsonElement value : dataJson(path).getAsJsonArray("values")) {
            if (value.isJsonPrimitive()) {
                entries.add(value.getAsString());
                continue;
            }
            JsonObject optional = value.getAsJsonObject();
            entries.add(optional.get("id").getAsString() + (optional.get("required").getAsBoolean() ? "" : "?"));
        }
        return entries;
    }

    private static void assertSpawnModifier(String biomes, String name, String entityId, int weight, int maxCount)
            throws IOException {
        JsonObject modifier = dataJson("neoforge/biome_modifier/" + name + ".json");
        assertEquals("neoforge:add_spawns", modifier.get("type").getAsString(), name);
        assertEquals(biomes, modifier.get("biomes").getAsString(), name);
        JsonObject spawner = modifier.getAsJsonObject("spawners");
        assertEquals(entityId, spawner.get("type").getAsString(), name);
        assertEquals(weight, spawner.get("weight").getAsInt(), name);
        assertEquals(1, spawner.get("minCount").getAsInt(), name);
        assertEquals(maxCount, spawner.get("maxCount").getAsInt(), name);
    }

    private static int cubeCount(List<GeoBone> bones) {
        return bones.stream().mapToInt(bone -> bone.getCubes().size() + cubeCount(bone.getChildBones())).sum();
    }

    private static int boneCount(List<GeoBone> bones) {
        return bones.stream().mapToInt(bone -> 1 + boneCount(bone.getChildBones())).sum();
    }

    private static JsonObject json(String path) throws IOException {
        return parse(resource(path));
    }

    private static JsonObject dataJson(String path) throws IOException {
        InputStream input = EntityResourcesTest.class.getResourceAsStream("/data/guzhenren/" + path);
        assertNotNull(input, "Missing packaged data resource " + path);
        return parse(input);
    }

    private static JsonObject parse(InputStream input) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static InputStream resource(String path) {
        InputStream input = EntityResourcesTest.class.getResourceAsStream(ROOT + path);
        assertNotNull(input, "Missing packaged resource " + path);
        return input;
    }
}
