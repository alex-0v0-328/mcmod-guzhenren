package net.alex.guzhenren.item.gu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.mind.Brilliance;
import net.alex.guzhenren.gameplay.mind.MindPoolType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 部分手写设定库表格与枚举阶梯的对比：物品表需启动注册，见 {@code WikiItemsGameTests}；正文、食物标签、玩
 * 法行为另审；本测试不写笔记；生成的中英对照表页须逐行重复语言文件、含所列枚举组全部词条，其 Gu World 行与选词由
 * {@code tools/wiki_glossary.py} 查。
 */

class WikiConsistencyTest {

    private static final String APERTURE_PAGE = "玩家向/1.0.0/蛊真人Mod-玩家.md";
    private static final String MIND_PAGE = "玩家向/1.0.0/蛊真人Mod-玩家.md";
    private static final String GLOSSARY_PAGE = "中英对照表.md";
    private Path wiki;

    @BeforeEach
    void requireExplicitWikiDirectory() {
        String configured = System.getProperty("gzr.wikiDir");
        assumeTrue(configured != null, "Wiki check SKIPPED: supply -PwikiDir=<absolute wiki directory>");
        assertFalse(configured.isBlank(), "wikiDir must not be blank");
        wiki = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(wiki), () -> "Wiki directory does not exist: " + wiki);
    }

    @Test
    void talentBrillianceAndMindCapacitiesMatchEnums() throws IOException {
        WikiTable aperturePage = page(APERTURE_PAGE);
        String[] talents = { "十绝", "甲等", "乙等", "丙等", "丁等" };
        for (int i = 0; i < talents.length; i++) {
            Talent talent = Talent.getSettable()[i];
            WikiTable.Row row = aperturePage.row("资质", talents[i]);
            row.expect("基数", talent.getMinPercent() == talent.getMaxPercent()
                    ? List.of((long) talent.getMinPercent())
                    : List.of((long) talent.getMinPercent(), (long) talent.getMaxPercent()));
            row.expect("出现权重", List.of((long) talent.getWeight()));
            row.expect("自然回复倍率", List.of((long) talent.getRegenRate()));
        }
        WikiTable mindPage = page(MIND_PAGE);
        String[] brillianceNames = { "普通", "尚可", "不俗", "卓越", "旷世" };
        for (int i = 0; i < brillianceNames.length; i++) {
            Brilliance brilliance = Brilliance.values()[i];
            WikiTable.Row row = mindPage.row("才情", brillianceNames[i]);
            row.expect("念 / 自身时间秒", List.of(brilliance.getThoughtsPerSecond()));
            row.expect("权重", List.of((long) brilliance.getWeight()));
        }
        String[] wisdomNames = { "念", "意", "情" };
        for (int i = 0; i < wisdomNames.length; i++) {
            mindPage.row("内容", wisdomNames[i]).expect("默认上限", List.of(MindPoolType.values()[i].getDefaultCapacity()));
        }
    }

    @Test
    void essenceTableMatchesRankAndStageMultipliers() throws IOException {
        WikiTable page = page(APERTURE_PAGE);
        String[] stages = { "初阶", "中阶", "高阶", "巅峰" };
        String[] ranks = { "一转", "二转", "三转", "四转", "五转" };
        for (int i = 0; i < stages.length; i++) {
            Stage stage = Stage.getSettable()[i];
            WikiTable.Row row = page.row("小境界倍率", stages[i] + " ×" + stage.getEssenceMultiplier());
            for (int j = 0; j < ranks.length; j++) {
                Rank rank = Rank.getSettable()[j];
                String column = ranks[j] + " ×" + String.format(java.util.Locale.ROOT, "%,d", rank.getRankBase());
                row.expect(column, List.of((long) Talent.TEN_EXTREMES.getMinPercent()
                        * rank.getRankBase() * stage.getEssenceMultiplier()));
            }
        }
    }

    @Test
    void glossaryRowsMatchTheLangFiles() throws IOException {
        Path lang = Path.of(System.getProperty("gzr.projectDir"), "src/generated/resources/assets/guzhenren/lang");
        JsonObject chinese = JsonParser.parseString(Files.readString(lang.resolve("zh_cn.json"))).getAsJsonObject();
        String text = Files.readString(wiki.resolve(GLOSSARY_PAGE));
        int manual = text.indexOf("\n## 设定词\n");
        assertTrue(manual >= 0, GLOSSARY_PAGE + " has no 设定词 section");
        Matcher row = Pattern.compile("(?m)^\\| (.+?) \\| `([^`]+)` \\|$").matcher(text.substring(0, manual));
        Set<String> keys = new TreeSet<>();
        while (row.find()) {
            String key = row.group(2);
            assertTrue(keys.add(key), () -> GLOSSARY_PAGE + " lists " + key + " twice");
            if (key.startsWith("dimension.guworld.")) continue;
            assertTrue(chinese.has(key), () -> GLOSSARY_PAGE + " lists a key the lang files lack: " + key);
            assertEquals(chinese.get(key).getAsString(), row.group(1), key);
        }
        assertFalse(keys.isEmpty(), GLOSSARY_PAGE + " has no rows");
        Set<String> groups = new TreeSet<>();
        for (String key : keys) {
            if (key.startsWith("guzhenren.enum.")) groups.add(key.substring(0, key.lastIndexOf('.') + 1));
        }
        Set<String> missing = new TreeSet<>();
        for (String key : chinese.keySet()) {
            String value = chinese.get(key).getAsString();
            if (groups.contains(key.substring(0, key.lastIndexOf('.') + 1)) && !value.isBlank()
                    && !value.contains("%") && !keys.contains(key)) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(), () -> GLOSSARY_PAGE + " lacks " + missing
                + "; run tools/wiki_glossary.py --write");
    }

    private WikiTable page(String relative) throws IOException {
        Path path = wiki.resolve(relative);
        assertTrue(Files.isRegularFile(path), () -> "Missing mapped wiki page: " + path);
        return WikiTable.parse(path.toString(), Files.readString(path));
    }
}
