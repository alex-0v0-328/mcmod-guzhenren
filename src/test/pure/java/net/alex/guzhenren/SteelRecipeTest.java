package net.alex.guzhenren;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SteelRecipeTest {

    private static final String RECIPES = "/data/guzhenren/recipe/";

    @Test
    @DisplayName("a steel ingot is one iron ingot blasted for 5 seconds, and an ordinary furnace cannot")
    void steelIsBlastedIronOnly() {
        JsonObject blasting = read("steel_ingot_from_blasting_iron_ingot.json");
        assertEquals("minecraft:blasting", blasting.get("type").getAsString());
        assertEquals("minecraft:iron_ingot", blasting.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals("guzhenren:steel_ingot", blasting.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, blasting.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(100, blasting.get("cookingtime").getAsInt());
        assertEquals(0.1F, blasting.get("experience").getAsFloat());
        assertNull(SteelRecipeTest.class.getResourceAsStream(RECIPES + "steel_ingot_from_smelting_iron_ingot.json"));
    }

    @Test
    @DisplayName("nine steel ingots craft a steel block and the block crafts back into nine")
    void nineIngotsPackIntoABlockAndBack() {
        JsonObject block = read("steel_block.json");
        assertEquals("minecraft:crafting_shaped", block.get("type").getAsString());
        assertEquals(List.of("###", "###", "###"), block.getAsJsonArray("pattern").asList().stream()
                .map(JsonElement::getAsString).toList());
        assertEquals("guzhenren:steel_ingot", block.getAsJsonObject("key").getAsJsonObject("#").get("item")
                .getAsString());
        assertEquals("guzhenren:steel_block", block.getAsJsonObject("result").get("id").getAsString());

        JsonObject ingots = read("steel_ingot_from_steel_block.json");
        assertEquals("minecraft:crafting_shapeless", ingots.get("type").getAsString());
        assertEquals("guzhenren:steel_block", ingots.getAsJsonArray("ingredients").get(0).getAsJsonObject()
                .get("item").getAsString());
        assertEquals(9, ingots.getAsJsonObject("result").get("count").getAsInt());
    }

    private static JsonObject read(String name) {
        InputStream stream = SteelRecipeTest.class.getResourceAsStream(RECIPES + name);
        assertNotNull(stream, name);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
