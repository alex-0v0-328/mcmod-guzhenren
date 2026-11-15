package net.alex.guzhenren;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HopeGuSpawnTagTest {

    @Test
    @DisplayName("wild Gu spawn tag contains the exact land biome contract, Gu World's four regions as optional")
    void containsExactLandBiomeSet() {
        try (InputStreamReader reader = new InputStreamReader(
                getResource(),
                StandardCharsets.UTF_8)) {
            JsonArray values = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("values");
            Set<String> expected = Set.of(
                    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:cherry_grove",
                    "minecraft:flower_forest", "minecraft:forest", "minecraft:birch_forest", "minecraft:dark_forest",
                    "minecraft:old_growth_birch_forest", "minecraft:windswept_forest", "minecraft:taiga",
                    "minecraft:snowy_taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                    "minecraft:grove", "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna",
                    "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle", "minecraft:desert",
                    "minecraft:badlands", "minecraft:wooded_badlands", "minecraft:eroded_badlands", "minecraft:snowy_plains",
                    "minecraft:ice_spikes", "minecraft:snowy_slopes", "minecraft:frozen_peaks", "minecraft:jagged_peaks",
                    "minecraft:stony_peaks", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills",
                    "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:beach", "minecraft:snowy_beach",
                    "minecraft:stony_shore", "minecraft:mushroom_fields",
                    "guworld:forested_plateau?", "guworld:cloud_forest?", "guworld:peach_grove?",
                    "guworld:misty_mountains?", "guworld:bamboo_sea?", "guworld:rainforest?",
                    "guworld:jungle_mountains?", "guworld:emerald_peaks?", "guworld:plains?", "guworld:plateau?",
                    "guworld:hot_springs?", "guworld:orchid_swamp?", "guworld:alpine_meadow?",
                    "guworld:rocky_peaks?", "guworld:painted_mountains?", "guworld:granite_cliffs?",
                    "guworld:zhangshiyan_cliffs?", "guworld:karst_peak_forest?", "guworld:ancient_sands?",
                    "guworld:arid_highlands?", "guworld:badlands?", "guworld:brushland?", "guworld:bryce_canyon?",
                    "guworld:desert?", "guworld:desert_canyon?", "guworld:desert_oasis?", "guworld:desert_spires?",
                    "guworld:eroded_badlands?", "guworld:gravel_desert?", "guworld:hot_shrubland?",
                    "guworld:lush_desert?", "guworld:red_oasis?", "guworld:sandstone_valley?",
                    "guworld:white_mesa?", "guworld:wooded_badlands?", "guworld:basalt_cliffs?", "guworld:beach?",
                    "guworld:gravel_beach?", "guworld:isles?", "guworld:jungle?", "guworld:mirage_isles?",
                    "guworld:stony_shore?", "guworld:volcanic_crater?", "guworld:volcanic_peaks?",
                    "guworld:white_cliffs?", "guworld:alpine_highlands?", "guworld:birch_taiga?",
                    "guworld:cold_shrubland?", "guworld:frozen_cliffs?", "guworld:frozen_peaks?",
                    "guworld:glacial_chasm?", "guworld:grove?", "guworld:ice_marsh?", "guworld:ice_spikes?",
                    "guworld:shield?", "guworld:siberian_taiga?", "guworld:snowy_plains?", "guworld:snowy_shield?",
                    "guworld:snowy_taiga?", "guworld:steppe?", "guworld:wintry_forest?",
                    "guworld:wintry_lowlands?");
            Set<String> actual = values.asList().stream().map(HopeGuSpawnTagTest::getEntry)
                    .collect(Collectors.toSet());
            assertEquals(expected.size(), values.size());
            assertEquals(expected, actual);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static String getEntry(JsonElement value) {
        if (value instanceof JsonPrimitive id) return id.getAsString();
        JsonObject optional = value.getAsJsonObject();
        return optional.get("id").getAsString() + (optional.get("required").getAsBoolean() ? "" : "?");
    }

    private InputStream getResource() {
        InputStream stream = getClass().getResourceAsStream(
                "/data/guzhenren/tags/worldgen/biome/hope_gu_spawns.json");
        assertNotNull(stream);
        return stream;
    }
}
