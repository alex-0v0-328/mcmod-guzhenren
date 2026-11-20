package net.alex.guzhenren;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.alex.guzhenren.datagen.lang.TraditionalGlyphs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 四份生成语言文件的契约，从类路径直读：游戏内文本只有中文，en_us 须与 zh_cn 逐字相同（原版只回退到 en_us，非中文客
 * 户端由此显示简体），zh_hk 须与 zh_tw 逐字相同；值不得空，三个 {@code none} 枚举显示键除外（Rank.NONE、Stage.NONE、
 * ExtremePhysique.NONE 不作名称渲染）；中文方括号 {@code [...]} 内无空白；zh_tw 须逐键等于 zh_cn 经
 * {@code TraditionalGlyphs.convert} 的结果，改字形表未跑 {@code runData} 即在此失败，不发过期文件；
 * {@link #HAND_WRITTEN_TRADITIONAL} 的键自有繁体措辞（换词非换字），只须异于 zh_cn。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

class LangContractTest {

    private static final Pattern BRACKETS = Pattern.compile("\\[[^\\[\\]]*\\]");
    private static final Set<String> BLANK_BY_DESIGN = Set.of(
            "guzhenren.enum.aperture.rank.none",
            "guzhenren.enum.aperture.stage.none",
            "guzhenren.enum.body.extreme_physique.none");
    private static final Set<String> HAND_WRITTEN_TRADITIONAL = Set.of(
            "guzhenren.command.travel.missing_dimension");

    @Test
    @DisplayName("en_us repeats zh_cn and zh_hk repeats zh_tw, entry for entry")
    void fallbackAndHongKongFilesAreCopies() {
        assertEquals(load("zh_cn"), load("en_us"), "en_us must carry the Simplified text -- rerun runData");
        assertEquals(load("zh_tw"), load("zh_hk"), "zh_hk must carry the Traditional text -- rerun runData");
    }

    @Test
    @DisplayName("zh_cn and zh_tw carry exactly the same key set")
    void languagesShareKeySets() {
        JsonObject simplified = load("zh_cn");
        JsonObject traditional = load("zh_tw");
        List<String> onlySimplified = keysMissingFrom(simplified, traditional);
        List<String> onlyTraditional = keysMissingFrom(traditional, simplified);
        assertTrue(onlySimplified.isEmpty() && onlyTraditional.isEmpty(),
                () -> "key sets differ -- only in zh_cn: " + onlySimplified + ", only in zh_tw: " + onlyTraditional);
    }

    @Test
    @DisplayName("zh_tw is zh_cn converted glyph by glyph")
    void zhTwIsDerivedFromZhCn() {
        JsonObject simplified = load("zh_cn");
        JsonObject traditional = load("zh_tw");
        for (Map.Entry<String, JsonElement> entry : simplified.entrySet()) {
            if (HAND_WRITTEN_TRADITIONAL.contains(entry.getKey())) {
                assertNotEquals(entry.getValue().getAsString(), traditional.get(entry.getKey()).getAsString(),
                        () -> "hand-written zh_tw equals zh_cn under key " + entry.getKey());
                continue;
            }
            assertEquals(TraditionalGlyphs.convert(entry.getValue().getAsString()),
                    traditional.get(entry.getKey()).getAsString(),
                    () -> "zh_tw is stale under key " + entry.getKey() + " -- rerun runData");
        }
    }

    @Test
    @DisplayName("Chinese bracket annotations never contain whitespace")
    void zhBracketsHaveNoWhitespace() {
        for (String language : List.of("zh_cn", "zh_tw")) {
            JsonObject zh = load(language);
            for (Map.Entry<String, JsonElement> entry : zh.entrySet()) {
                String value = entry.getValue().getAsString();
                Matcher matcher = BRACKETS.matcher(value);
                while (matcher.find()) {
                    String inside = matcher.group().substring(1, matcher.group().length() - 1);
                    for (int i = 0; i < inside.length(); i++) {
                        char current = inside.charAt(i);
                        assertFalse(Character.isWhitespace(current),
                                () -> "bracket annotation carries whitespace under " + language + " key "
                                        + entry.getKey() + ": [" + inside + "]");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("only the none enum keys may be blank, and identically in both scripts")
    void onlyNoneKeysAreBlank() {
        for (String language : List.of("zh_cn", "zh_tw")) {
            JsonObject lang = load(language);
            Set<String> blanks = new TreeSet<>();
            for (Map.Entry<String, JsonElement> entry : lang.entrySet()) {
                assertTrue(entry.getValue().isJsonPrimitive(),
                        () -> "value of " + entry.getKey() + " in " + language + " is not a plain string");
                if (entry.getValue().getAsString().isBlank()) blanks.add(entry.getKey());
            }
            assertEquals(BLANK_BY_DESIGN, blanks,
                    () -> "blank values in " + language + " deviate from the none enum keys");
        }
    }

    private static List<String> keysMissingFrom(JsonObject source, JsonObject target) {
        List<String> missing = new ArrayList<>();
        for (String key : source.keySet()) {
            if (!target.has(key)) missing.add(key);
        }
        return missing;
    }

    private static JsonObject load(String language) {
        InputStream stream = LangContractTest.class.getResourceAsStream(
                "/assets/guzhenren/lang/" + language + ".json");
        assertNotNull(stream, () -> "lang file missing from the classpath: " + language);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new AssertionError("cannot read lang file " + language, e);
        }
    }
}
