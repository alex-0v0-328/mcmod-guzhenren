package net.alex.guzhenren.item.gu;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 只读设定库表格读取器的对抗性用例。 */

class WikiTableTest {

    private static final String TABLE = "| Gu | Cost |\n|---|---:|\n| Boar | 3,600 |\n";

    @Test
    void readsNumbersAndReportsTheirSourceLine() {
        WikiTable.Row row = WikiTable.parse("items.md", TABLE).row("Gu", "Boar");
        row.expect("Cost", List.of(3600L));
        assertEquals("items.md:3", row.location());
        WikiTable.parse("items.md", TABLE).expectMatch("\\| Boar \\| ([0-9,]+)", List.of(3600L));
    }

    @Test
    void changedNumberFailsAtItsSourceLine() {
        AssertionError error = assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", TABLE).row("Gu", "Boar").expect("Cost", List.of(800L)));
        assertTrue(error.getMessage().contains("items.md:3"));
        assertTrue(error.getMessage().contains("800"));
        AssertionError inline = assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", "prefix 800\n").expectMatch("(800)", List.of(1L)));
        assertTrue(inline.getMessage().contains("items.md:1"));
    }

    @Test
    void missingRowFailsInsteadOfSkipping() {
        assertThrows(AssertionError.class, () -> WikiTable.parse("items.md", TABLE).row("Gu", "Bear"));
    }

    @Test
    void duplicateRowFailsInsteadOfPickingTheFirst() {
        assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", TABLE + "| Boar | 800 |\n").row("Gu", "Boar"));
    }

    @Test
    void malformedNumberFailsInsteadOfExtractingItsDigits() {
        assertThrows(AssertionError.class, () -> WikiTable.parse("items.md", TABLE.replace("3,600", "about 3600"))
                .row("Gu", "Boar").expect("Cost", List.of(3600L)));
    }

    @Test
    void malformedColumnCountFails() {
        assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", TABLE.replace("3,600 |", "3,600 | extra |")));
    }

    @Test
    void acceptsExplicitListsAndChineseRankRanges() {
        WikiTable.Row row = WikiTable.parse("items.md", TABLE.replace("3,600", "一至五")).row("Gu", "Boar");
        row.expect("Cost", List.of(1L, 2L, 3L, 4L, 5L));
        WikiTable.parse("items.md", TABLE.replace("3,600", "16 / 160 / 1,600"))
                .row("Gu", "Boar").expect("Cost", List.of(16L, 160L, 1600L));
    }

    @Test
    void wikiAliasesDoNotSplitTableCells() {
        WikiTable.Row row = WikiTable.parse("items.md", TABLE.replace("Boar", "[[boar|Boar]]"))
                .row("Gu", "[[boar|Boar]]");
        row.expect("Cost", List.of(3600L));
    }

    @Test
    void missingColumnFailsWithRowLocation() {
        AssertionError error = assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", TABLE).row("Gu", "Boar").cell("Hunger"));
        assertTrue(error.getMessage().contains("items.md:3"));
    }

    @Test
    void missingAndDuplicateItemMappingsFail() {
        assertThrows(AssertionError.class,
                () -> WikiTable.requireCoverage(List.of("boar"), List.of("boar", "bear")));
        assertThrows(AssertionError.class,
                () -> WikiTable.requireCoverage(List.of("boar", "boar"), List.of("boar")));
        WikiTable.requireCoverage(List.of("bear", "boar"), List.of("boar", "bear"));

        String table = "| Kind | Cost |\n|---|---:|\n| boar | 3,600 |\n| bear | 800 |\n";
        WikiTable parsed = WikiTable.parse("items.md", table);
        parsed.requireExactRows(List.of("Kind", "Cost"), List.of("boar", "bear"));
        AssertionError stale = assertThrows(AssertionError.class,
                () -> parsed.requireExactRows(List.of("Kind", "Cost"), List.of("boar")));
        assertTrue(stale.getMessage().contains("items.md:4"));
        AssertionError duplicate = assertThrows(AssertionError.class,
                () -> WikiTable.parse("items.md", table + "| boar | 3,600 |\n")
                        .requireExactRows(List.of("Kind", "Cost"), List.of("boar", "bear")));
        assertTrue(duplicate.getMessage().contains("items.md:5"));
        AssertionError missing = assertThrows(AssertionError.class,
                () -> parsed.requireExactRows(List.of("Kind", "Cost"), List.of("boar", "wolf")));
        assertTrue(missing.getMessage().contains("items.md:1"));

        String sameFirstHeader = """
                | Kind | Cost |
                |---|---:|
                | boar | 3,600 |

                | Kind | Cooldown |
                |---|---:|
                | ghost | 1 |
                """;
        WikiTable.parse("items.md", sameFirstHeader)
                .requireExactRows(List.of("Kind", "Cost"), List.of("boar"));
    }

    private static final String GU_ENTRY = """
            ### Boar

            | 项目 | 说明 |
            |---|---|
            | 道 | 力 |

            | 蛊虫 | 转 | 炼化真元 |
            |---|---|---|
            | Boar | 一 | 800 |
            | Boar | 二 | 8,000 |

            | 蛊虫 | 转 | 每天扣 |
            |---|---|---|
            | Boar | 一 | 1 |
            | Boar | 二 | 2 |
            """;

    @Test
    void joinsAdjacentGuTablesWithSharedFields() {
        List<WikiTable.Row> rows = WikiTable.parse("gu.md", GU_ENTRY).guRows();
        assertEquals(2, rows.size());
        WikiTable.Row first = rows.getFirst();
        assertEquals("Boar", first.cell("蛊虫"));
        assertEquals("力", first.cell("道"));
        first.expect("炼化真元", List.of(800L));
        first.expect("每天扣", List.of(1L));
        assertEquals("gu.md:9", first.location());
    }

    @Test
    void joinedGuRanksKeepTheirOwnData() {
        WikiTable.Row second = WikiTable.parse("gu.md", GU_ENTRY).guRows().get(1);
        second.expect("转", List.of(2L));
        second.expect("炼化真元", List.of(8000L));
        second.expect("每天扣", List.of(2L));
    }

    @Test
    void duplicateGuFieldsFailEvenWhenTheValuesMatch() {
        String duplicate = GU_ENTRY + "\n| 蛊虫 | 转 | 每天扣 |\n|---|---|---|\n| Boar | 一 | 1 |\n";
        assertThrows(AssertionError.class, () -> WikiTable.parse("gu.md", duplicate).guRows());
    }

    @Test
    void missingGuFieldDoesNotBorrowFromAnotherSection() {
        String next = "\n### Bear\n\n| 项目 | 说明 |\n|---|---|\n| 使用真元 | 16 |\n"
                + "\n| 蛊虫 | 转 | 炼化真元 |\n|---|---|---|\n| Bear | 一 | 800 |\n";
        List<WikiTable.Row> rows = WikiTable.parse("gu.md", GU_ENTRY + next).guRows();
        assertThrows(AssertionError.class, () -> rows.getFirst().cell("使用真元"));
        rows.getLast().expect("使用真元", List.of(16L));
    }

    @Test
    void duplicateGuEntryInAnotherSectionFails() {
        assertThrows(AssertionError.class,
                () -> WikiTable.parse("gu.md", GU_ENTRY + GU_ENTRY.replace("### Boar", "### Duplicate")).guRows());
    }
    @Test
    void readsShortGuFactsWithoutAnAttributeTable() {
        String facts = """
                ### Light

                **转**：一；**道**：光；**使用真元**：0。

                ### Terrain

                **蛊虫**：Mountain、River；**转**：五。

                **道**：宙；**使用真元**：640,000。

                **Note**：First explanation。

                **Note**：Another explanation。
                """;
        List<WikiTable.Row> rows = WikiTable.parse("gu.md", facts).guRows();
        assertEquals(3, rows.size());
        assertEquals("Light", rows.getFirst().cell("蛊虫"));
        rows.getFirst().expect("使用真元", List.of(0L));
        assertEquals("Mountain", rows.get(1).cell("蛊虫"));
        assertEquals("River", rows.get(2).cell("蛊虫"));
        rows.getLast().expect("使用真元", List.of(640000L));
    }

    @Test
    void headingOrFollowingLineCanSupplyFixedGuFacts() {
        String text = """
                ## 一次性蛊虫

                ### 生机叶蛊，一转木道 · 一次性 · 治疗类

                **堆叠**：64；**拒绝**：生机叶效果还在。
                """;
        for (String layout : List.of(text, text.replace("，", "\n"), text.replace("，", "\n\n"))) {
            List<WikiTable.Row> entries = WikiTable.parse("gu.md", layout).guRows();
            assertEquals(1, entries.size());
            WikiTable.Row entry = entries.getFirst();
            assertEquals("生机叶蛊", entry.cell("蛊虫"));
            entry.expect("转", List.of(1L));
            assertEquals("木", entry.cell("道"));
            assertEquals("治疗", entry.cell("类型"));
            assertEquals("一次性", entry.cell("饱食类别"));
            entry.expect("堆叠", List.of(64L));
        }
    }

    @Test
    void seriesRowsInheritTheFixedRankFromHeadingOrFollowingLine() {
        String text = """
                ## 灌注型蛊虫

                ### 白豕蛊与黑豕蛊，一转力道 · 道痕蚀刻类

                **炼化真元**：800。

                | 蛊虫 | 食物 |
                |---|---|
                | 白豕蛊 | 生猪排 |
                | 黑豕蛊 | 生猪排 |
                """;
        for (String layout : List.of(text, text.replace("，", "\n"), text.replace("，", "\n\n"))) {
            List<WikiTable.Row> entries = WikiTable.parse("gu.md", layout).guRows();
            assertEquals(2, entries.size());
            entries.getFirst().expect("转", List.of(1L));
            entries.getLast().expect("炼化真元", List.of(800L));
            assertEquals("灌注型", entries.getLast().cell("饱食类别"));
        }
    }

    @Test
    void aConflictingHeadingRankIsRejected() {
        String text = """
                ### 白豕蛊，一转力道 · 道痕蚀刻类

                | 蛊虫 | 转 | 炼化真元 |
                |---|---|---|
                | 白豕蛊 | 二 | 800 |
                """;
        assertThrows(AssertionError.class, () -> WikiTable.parse("gu.md", text).guRows());
    }

    @Test
    void rankOnlyRowsUseTheSeriesNameFromTheHeading() {
        String text = """
                ### 元老蛊，宇道 · 存储类

                **饱食类别**：无饱食条。

                | 转 | 炼化真元 | 容量 |
                |---|---|---|
                | 一 | 16 | 1,000 |
                | 二 | 160 | 10,000 |
                """;
        List<WikiTable.Row> entries = WikiTable.parse("gu.md", text).guRows();
        assertEquals(2, entries.size());
        assertEquals("元老蛊", entries.getFirst().cell("蛊虫"));
        entries.getLast().expect("转", List.of(2L));
        entries.getLast().expect("容量", List.of(10000L));
    }


    @Test
    void transposedGuTablesKeepNamesRanksAndCostsTogether() {
        String text = """
                ## 灌注型蛊虫

                ### 兽力

                | 属性 | 白豕蛊 | 虎力蛊 |
                |---|---|---|
                | 转 | 一 | 二 |
                | 流派 | 力道 | 力道 |
                | 类型 | 道痕蚀刻 | 道痕蚀刻 |
                | 炼化真元 | 800 | 1,600 |

                ### 元老蛊

                | 属性 | 元老蛊（一转） | 元老蛊（二转） |
                |---|---|---|
                | 转 | 一 | 二 |
                | 炼化真元 | 800 | 1,600 |
                """;
        List<WikiTable.Row> rows = WikiTable.parse("gu.md", text).guRows();
        assertEquals(4, rows.size());
        assertEquals("白豕蛊", rows.getFirst().cell("蛊虫"));
        assertEquals("虎力蛊", rows.get(1).cell("蛊虫"));
        assertEquals("力", rows.get(1).cell("道"));
        assertEquals("道痕蚀刻", rows.get(1).cell("类型"));
        rows.get(1).expect("转", List.of(2L));
        rows.get(1).expect("炼化真元", List.of(1600L));
        assertEquals("元老蛊", rows.getLast().cell("蛊虫"));
        rows.getLast().expect("转", List.of(2L));
        assertThrows(AssertionError.class, () -> rows.getLast().cell("流派"));
        assertThrows(AssertionError.class, () -> rows.getLast().expect("炼化真元", List.of(800L)));
    }

    @Test
    void transposedGuTablesRejectAmbiguousFactsAndMissingRanks() {
        String text = """
                ### 元老蛊

                | 属性 | 元老蛊（一转） |
                |---|---|
                | 转 | 一 |
                | 炼化真元 | 800 |
                """;
        assertThrows(AssertionError.class,
                () -> WikiTable.parse("gu.md", text + "| 炼化真元 | 800 |\n").guRows());
        assertThrows(AssertionError.class,
                () -> WikiTable.parse("gu.md", text.replace("| 转 | 一 |\n", "")).guRows());
        assertThrows(AssertionError.class,
                () -> WikiTable.parse("gu.md", text.replace("| 转 | 一 |", "| 转 | 二 |")).guRows());
    }

    private static final String TYPED_HEADINGS = """
            ### 道痕蚀刻类

            | 蛊虫 | 转 | 流派 | 效果 |
            |---|---|---|---|
            | 铜皮蛊 | 一 | 土道 | 永久减 1 |

            ### 攻击类

            | 蛊虫 | 转 | 流派 | 效果 |
            |---|---|---|---|
            | 月刃蛊 | 一 | 月道 | 一道月刃 |

            ### 无类型

            | 蛊虫 | 转 | 流派 | 效果 |
            |---|---|---|---|
            | 希望蛊 | 一 | 人道 | 开窍 |
            """;

    @Test
    void aTableWithoutATypeColumnTakesItsTypeFromTheHeading() {
        List<WikiTable.Row> rows = WikiTable.parse("gu.md", TYPED_HEADINGS).guRows();
        assertEquals(3, rows.size());
        assertEquals("道痕蚀刻", rows.get(0).cell("类型"));
        assertEquals("攻击", rows.get(1).cell("类型"));
        assertEquals("无", rows.get(2).cell("类型"));
    }

    private static final String LABELED_GU = """
            ### 防御

            **类型**：防御

            | 蛊虫 | 转 | 流派 | 养护 | 冷却 | 效果 |
            |---|---|---|---|---|---|
            | 玉皮蛊 | 一 | 土道 | 饱食上限：14<br>食物：玉、翡翠 | 30 秒（物品 1 秒）<br>冷却中再催：等完整冷却 | 分支：无<br>每次受伤减 2 |
            | 玉皮蛊 | 二 | 土道 | 饱食上限：26<br>食物：玉 | 1 秒<br>冷却中再催：— | 减 4 |
            """;

    @Test
    void labeledCellLinesBecomeGuFieldsAndTheColumnKeepsTheRest() {
        List<WikiTable.Row> rows = WikiTable.parse("gu.md", LABELED_GU).guRows();
        assertEquals(2, rows.size());
        WikiTable.Row first = rows.getFirst();
        assertEquals("土", first.cell("道"));
        assertEquals("防御", first.cell("类型"));
        first.expect("饱食上限", List.of(14L));
        assertEquals("玉、翡翠", first.cell("食物"));
        assertEquals("30 秒（物品 1 秒）", first.cell("冷却"));
        assertEquals("等完整冷却", first.cell("冷却中再催"));
        assertEquals("", first.cell("养护"));
        assertEquals("分支：无<br>每次受伤减 2", first.cell("效果"));
        assertThrows(AssertionError.class, () -> first.cell("流派"));
        rows.getLast().expect("饱食上限", List.of(26L));
        assertEquals("1 秒", rows.getLast().cell("冷却"));
    }

    @Test
    void aLabeledFieldThatRepeatsAColumnOrAnotherLabelFails() {
        assertThrows(AssertionError.class,
                () -> WikiTable.parse("gu.md", LABELED_GU.replace("食物：玉、翡翠", "冷却：1 秒")));
        assertThrows(AssertionError.class,
                () -> WikiTable.parse("gu.md", LABELED_GU.replace("食物：玉、翡翠", "饱食上限：14")));
    }

    private static final String SLASH_GU = """
            ### 防御

            **类型**：防御

            | 蛊虫 | 流派 | 真元<br>炼化/使用 | 饱食度<br>上限/每次/每天 | 生命值 | 食物 | CD | 条件 |
            |---|---|---|---|---|---|---|---|
            | 一转玉皮蛊 | 土道 | 800 / 12 | 14 / 3 / 1 | 8 | 玉<br>翡翠 | 30 秒 | 条件：<br>未开窍<br>拒绝：<br>皮肤非原生<br>在宝黄天 |
            | 二转元老蛊 | 宇道 | 1,600 / 0 | 9 / 2 / N/A | 12<br>修复：不能修复 | N/A | N/A | 拒绝：<br>满了<br>条件：<br>空手 |
            """;

    @Test
    void slashColumnsAndRankedNamesBecomeGuFields() {
        List<WikiTable.Row> rows = WikiTable.parse("gu.md", SLASH_GU).guRows();
        assertEquals(2, rows.size());
        WikiTable.Row first = rows.getFirst();
        assertEquals("玉皮蛊", first.cell("蛊虫"));
        first.expect("转", List.of(1L));
        first.expect("炼化真元", List.of(800L));
        first.expect("使用真元", List.of(12L));
        first.expect("饱食上限", List.of(14L));
        first.expect("每次扣", List.of(3L));
        first.expect("每天扣", List.of(1L));
        first.expect("生命上限", List.of(8L));
        assertEquals("玉<br>翡翠", first.cell("食物"));
        assertEquals("30 秒", first.cell("冷却"));
        assertEquals("皮肤非原生<br>在宝黄天", first.cell("拒绝"));
        assertEquals("条件：<br>未开窍", first.cell("条件"));
        WikiTable.Row last = rows.getLast();
        assertEquals("元老蛊", last.cell("蛊虫"));
        last.expect("饱食上限", List.of(9L));
        assertEquals("N/A", last.cell("每天扣"));
        assertEquals("满了", last.cell("拒绝"));
        assertEquals("条件：<br>空手", last.cell("条件"));
        last.expect("生命上限", List.of(12L));
        assertEquals("不能修复", last.cell("修复"));
    }

    @Test
    void aSlashCellWithTheWrongValueCountFails() {
        assertThrows(AssertionError.class, () -> WikiTable.parse("gu.md", SLASH_GU.replace("14 / 3 / 1", "14 / 3")));
    }

}
