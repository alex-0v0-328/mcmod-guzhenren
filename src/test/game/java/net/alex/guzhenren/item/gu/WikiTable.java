package net.alex.guzhenren.item.gu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;

/**
 * wiki 设定库表格的只读解析器，不改内容。每个 {@link Row} 带 {@code section}：表格上方最近的标题行，无则为空。
 * <ul>
 *   <li>蛊表（首列“蛊虫”）一格可含多字段：以蛊字段名加 {@code ：} 开头的 {@code <br>} 行成为该字段，列留其余行；
 *       冒号后为空的字段行收下其后各行，直到下一标签。</li>
 *   <li>“流派”列即去掉末尾“道”的道[Path]字段，同转置属性表；“生命值”即“生命上限”，“CD”即“冷却”；斜杠列（如“真元
 *       炼化/使用”）按 {@code " / "} 把值拆进所列字段，整格单个 N/A 时全为 N/A。</li>
 *   <li>无“转”列时，以转数开头的名称（如“一转玉皮蛊”）同时给出两字段；无“类型”列时由点明类型的标题（“防御类”至“无类
 *       型”）给出，因玩家表按类型分组，类型列只会重复标题；无“类”后缀的标题把类型留给该节事实行。</li>
 * </ul>
 */

final class WikiTable {

    private static final String GU_PATH = "(冰雪|变化|[天律宇宙人金木水火土雷云气音光暗月毒力梦炼智信偷运杀血魂奴食阵画])道";

    private static final java.util.Set<String> GU_FIELDS = java.util.Set.of(
            "蛊虫", "转", "道", "类型", "饱食类别", "炼化真元", "使用真元", "真元 / 轮", "每次扣", "一轮耗饱食",
            "每天扣", "饱食上限", "保质上限", "点数上限", "生命上限", "食物", "单食回", "修复", "回点", "回生命",
            "冷却", "冷却中再催", "扣饱食时机", "堆叠", "用后", "拒绝");

    private static final Map<String, String> GU_COLUMNS = Map.of("流派", "道", "生命值", "生命上限", "CD", "冷却");

    private static final Map<String, List<String>> GU_SLASH_COLUMNS = Map.of(
            "真元<br>炼化/使用", List.of("炼化真元", "使用真元"),
            "饱食度<br>上限/每次/每天", List.of("饱食上限", "每次扣", "每天扣"));

    private static final Pattern RANKED_GU = Pattern.compile("([一二三四五])转(.+)");
    private static final Pattern GU_TYPE_HEADING = Pattern.compile(
            "#+ (?:(道痕蚀刻|防御|攻击|移动|调查|隐匿|治疗|存储|修炼|增幅)类|无类型)");

    private final String source;
    private final String text;
    private final List<Row> rows;

    private WikiTable(String source, String text, List<Row> rows) {
        this.source = source;
        this.text = text;
        this.rows = List.copyOf(rows);
    }

    static WikiTable parse(String source, String text) {
        List<Row> rows = new ArrayList<>();
        List<String> headers = List.of();
        List<String> previous = List.of();
        String section = "";
        String parent = "";
        int headerLine = 0;
        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (!line.startsWith("|")) {
                if (line.startsWith("## ")) parent = line;
                if (line.startsWith("#")) {
                    section = line;
                    String location = source + ":" + (i + 1);
                    for (var field : headingFacts(section, parent, lines, i).entrySet()) {
                        rows.add(new Row(location, location, section, List.of("项目", "说明"),
                                List.of(field.getKey(), field.getValue())));
                    }
                }
                if (line.startsWith("**") && section.startsWith("### ")) {
                    Map<String, String> facts = new LinkedHashMap<>();
                    String sentence = line.endsWith("。") ? line.substring(0, line.length() - 1) : line;
                    Matcher fact = Pattern.compile("\\*\\*([^*]+)\\*\\*：(.*?)(?=；\\*\\*|$)").matcher(sentence);
                    while (fact.find()) {
                        if (!GU_FIELDS.contains(fact.group(1))) continue;
                        if (facts.putIfAbsent(fact.group(1), fact.group(2)) != null) {
                            throw new AssertionError(source + ":" + (i + 1) + ": duplicate Gu fact " + fact.group(1));
                        }
                    }
                    String location = source + ":" + (i + 1);
                    if (facts.containsKey("转")) {
                        String names = facts.getOrDefault("蛊虫", guName(section));
                        for (String name : names.split("、")) {
                            for (String rank : facts.get("转").split("、")) {
                                rows.add(new Row(location, location, section, List.of("蛊虫", "转"),
                                        List.of(name, rank)));
                            }
                        }
                    }
                    for (var field : facts.entrySet()) {
                        if (field.getKey().equals("蛊虫") || field.getKey().equals("转")) continue;
                        rows.add(new Row(location, location, section, List.of("项目", "说明"),
                                List.of(field.getKey(), field.getValue())));
                    }
                }
                headers = List.of();
                previous = List.of();
                continue;
            }
            List<String> cells = split(line);
            if (cells.stream().allMatch(c -> c.matches(":?-+:?"))) {
                if (previous.size() != cells.size() || previous.isEmpty()) {
                    throw new AssertionError(source + ":" + (i + 1) + " invalid table header");
                }
                headers = previous;
                headerLine = i;
            } else if (!headers.isEmpty()) {
                if (cells.size() != headers.size()) {
                    throw new AssertionError(source + ":" + (i + 1) + " expected " + headers.size()
                            + " columns, found " + cells.size());
                }
                Row row = new Row(source + ":" + (i + 1), source + ":" + headerLine, section, headers, cells);
                rows.add(headers.getFirst().equals("蛊虫") ? expandGuCells(row) : row);
            }
            previous = cells;
        }
        return new WikiTable(source, text, transposeGuAttributes(rows));
    }

    private static Row expandGuCells(Row row) {
        List<String> fields = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (int column = 0; column < row.headers().size(); column++) {
            String header = row.headers().get(column);
            String cell = row.cells().get(column);
            Matcher ranked = RANKED_GU.matcher(cell);
            if (header.equals("蛊虫") && !row.headers().contains("转") && ranked.matches()) {
                fields.addAll(List.of("蛊虫", "转"));
                values.addAll(List.of(ranked.group(2), ranked.group(1)));
                continue;
            }
            if (header.equals("流派")) cell = cell.replaceFirst("道$", "");
            header = GU_COLUMNS.getOrDefault(header, header);
            List<String> own = new ArrayList<>();
            int open = -1;
            for (String raw : cell.split("<br>", -1)) {
                String line = raw.strip();
                int colon = line.indexOf('：');
                String field = colon < 0 ? "" : line.substring(0, colon).strip();
                if (!List.of("蛊虫", "转").contains(header) && GU_FIELDS.contains(field)) {
                    fields.add(field);
                    values.add(line.substring(colon + 1).strip());
                    open = values.getLast().isEmpty() ? values.size() - 1 : -1;
                } else if (open >= 0 && !line.endsWith("：")) {
                    values.set(open, values.get(open).isEmpty() ? line : values.get(open) + "<br>" + line);
                } else {
                    open = -1;
                    own.add(line);
                }
            }
            List<String> slash = GU_SLASH_COLUMNS.get(header);
            if (slash == null) {
                fields.add(header);
                values.add(String.join("<br>", own));
                continue;
            }
            List<String> parts = own.equals(List.of("N/A")) ? java.util.Collections.nCopies(slash.size(), "N/A")
                    : Arrays.stream(String.join("<br>", own).split(" / ", -1)).map(String::strip).toList();
            if (parts.size() != slash.size()) {
                throw new AssertionError(row.location() + ": expected " + slash + " in " + header + ", found " + own);
            }
            fields.addAll(slash);
            values.addAll(parts);
        }
        if (!fields.contains("类型")) {
            Matcher type = GU_TYPE_HEADING.matcher(row.section());
            if (type.matches()) {
                fields.add("类型");
                values.add(type.group(1) == null ? "无" : type.group(1));
            }
        }
        if (new HashSet<>(fields).size() != fields.size()) {
            throw new AssertionError(row.location() + ": duplicate Gu field in " + fields);
        }
        return new Row(row.location(), row.headerLocation(), row.section(), fields, values);
    }

    /** 把属性表行规整为蛊行，其他表格及其源位置保持不变。 */
    private static List<Row> transposeGuAttributes(List<Row> rows) {
        List<Row> result = new ArrayList<>(rows);
        Map<String, List<Row>> tables = new LinkedHashMap<>();
        for (Row row : rows) {
            if (row.section().startsWith("### ") && row.headers().getFirst().equals("属性")) {
                tables.computeIfAbsent(row.headerLocation(), ignored -> new ArrayList<>()).add(row);
            }
        }
        for (List<Row> table : tables.values()) {
            Row first = table.getFirst();
            List<String> fields = new ArrayList<>(List.of("蛊虫"));
            for (Row attribute : table) {
                String field = attribute.cells().getFirst();
                if (field.equals("流派")) field = "道";
                if (fields.contains(field)) {
                    throw new AssertionError(attribute.location() + ": duplicate Gu attribute " + field);
                }
                fields.add(field);
            }
            for (int column = 1; column < first.headers().size(); column++) {
                String label = first.headers().get(column);
                Matcher rankedName = Pattern.compile("(.+)（([一二三四五])转）").matcher(label);
                boolean ranked = rankedName.matches();
                List<String> values = new ArrayList<>(List.of(ranked ? rankedName.group(1) : label));
                for (Row attribute : table) {
                    String value = attribute.cells().get(column);
                    if (attribute.cells().getFirst().equals("流派")) value = value.replaceFirst("道$", "");
                    values.add(value);
                }
                if (ranked && (!fields.contains("转") || !rankedName.group(2).equals(values.get(fields.indexOf("转"))))) {
                    throw new AssertionError(first.location() + ": column rank conflicts with the Gu rank: " + label);
                }
                result.add(new Row(first.location(), first.headerLocation(), first.section(),
                        List.copyOf(fields), List.copyOf(values)));
            }
        }
        return result;
    }

    /** 拼接同一系列内相邻的蛊/转表与共享字段，拒绝重复事实。 */
    List<Row> guRows() {
        Map<String, Map<String, String>> shared = new LinkedHashMap<>();
        for (Row row : rows) {
            if (!row.headers().equals(List.of("项目", "说明"))) continue;
            Map<String, String> fields = shared.computeIfAbsent(row.section(), ignored -> new LinkedHashMap<>());
            String field = row.cells().getFirst();
            if (fields.putIfAbsent(field, row.cells().get(1)) != null) {
                throw new AssertionError(row.location() + ": duplicate shared Gu field " + field);
            }
        }
        Map<List<String>, Map<String, String>> entries = new LinkedHashMap<>();
        Map<List<String>, Row> sources = new LinkedHashMap<>();
        for (Row row : rows) {
            if (!List.of("蛊虫", "转").contains(row.headers().getFirst())) continue;
            String name = row.headers().contains("蛊虫") ? row.cell("蛊虫") : guName(row.section());
            String rank = row.headers().contains("转") ? row.cell("转")
                    : shared.getOrDefault(row.section(), Map.of()).get("转");
            if (rank == null || !rank.matches("[一二三四五]")) {
                throw new AssertionError(row.location() + ": missing Gu rank for " + name);
            }
            List<String> key = List.of(name, rank);
            Row previous = sources.putIfAbsent(key, row);
            if (previous != null && !previous.section().equals(row.section())) {
                throw new AssertionError(row.location() + ": duplicate Gu entry " + key);
            }
            Map<String, String> fields = entries.computeIfAbsent(key, ignored -> {
                Map<String, String> initial = new LinkedHashMap<>();
                initial.put("蛊虫", key.getFirst());
                initial.put("转", key.get(1));
                return initial;
            });
            for (int i = 0; i < row.headers().size(); i++) {
                String field = row.headers().get(i);
                if (field.equals("蛊虫") || field.equals("转")) continue;
                if (fields.putIfAbsent(field, row.cells().get(i)) != null) {
                    throw new AssertionError(row.location() + ": duplicate Gu field " + field + " for " + key);
                }
            }
        }
        // A single-rank Gu needs no identity table when its heading already names it and its rank.
        shared.forEach((section, facts) -> {
            if (!section.startsWith("### ") || !facts.containsKey("转")
                    || sources.values().stream().anyMatch(row -> row.section().equals(section))) return;
            Row origin = rows.stream().filter(row -> row.section().equals(section)).findFirst().orElseThrow();
            for (String name : guName(section).split("、|与")) {
                List<String> key = List.of(name, facts.get("转"));
                if (sources.putIfAbsent(key, origin) != null) {
                    throw new AssertionError(origin.location() + ": duplicate Gu entry " + key);
                }
                Map<String, String> fields = new LinkedHashMap<>();
                fields.put("蛊虫", name);
                fields.put("转", facts.get("转"));
                entries.put(key, fields);
            }
        });
        List<Row> result = new ArrayList<>();
        entries.forEach((key, fields) -> {
            Row origin = sources.get(key);
            shared.getOrDefault(origin.section(), Map.of()).forEach((field, value) -> {
                if (field.equals("转")) {
                    if (!value.equals(fields.get("转"))) {
                        throw new AssertionError(origin.location() + ": heading rank conflicts with the Gu row");
                    }
                    return;
                }
                if (fields.putIfAbsent(field, value) != null) {
                    throw new AssertionError(origin.location() + ": shared Gu field also declared per rank: " + field);
                }
            });
            result.add(new Row(origin.location(), origin.headerLocation(), origin.section(),
                    List.copyOf(fields.keySet()), List.copyOf(fields.values())));
        });
        return List.copyOf(result);
    }

    private static String guName(String section) {
        return section.substring(4).split("，", 2)[0].strip();
    }

    /** 固定事实取自标题或其下一行，从不取自被校验的注册。 */
    private static Map<String, String> headingFacts(String section, String parent, String[] lines, int heading) {
        Map<String, String> facts = new LinkedHashMap<>();
        if (!section.startsWith("### ")) return facts;
        String metadata;
        if (section.contains("，")) {
            metadata = section.substring(section.indexOf('，') + 1);
        } else {
            int next = heading + 1;
            while (next < lines.length && lines[next].isBlank()) next++;
            if (next == lines.length) return facts;
            metadata = lines[next].strip();
            if (!metadata.matches("(?:[一二三四五]转)?" + GU_PATH + "(?:、" + GU_PATH + ")*(?: · [^。]+)*")) return facts;
        }
        Matcher rank = Pattern.compile("([一二三四五])转").matcher(metadata);
        if (rank.find()) facts.put("转", rank.group(1));
        List<String> paths = Pattern.compile(GU_PATH)
                .matcher(metadata).results().map(match -> match.group(1)).distinct().toList();
        if (paths.size() == 1) facts.put("道", paths.getFirst());
        Matcher type = Pattern.compile("(道痕蚀刻|防御|攻击|移动|调查|隐匿|治疗|存储|修炼|增幅)类").matcher(metadata);
        facts.put("类型", type.find() ? type.group(1) : "无");
        if (metadata.contains("一次性") || parent.equals("## 一次性蛊虫")) facts.put("饱食类别", "一次性");
        else if (parent.equals("## 灌注型蛊虫")) facts.put("饱食类别", "灌注型");
        return facts;
    }

    Row row(String table, String label) {
        List<Row> found = rows.stream().filter(r -> r.headers().getFirst().equals(table)
                && r.cells().getFirst().equals(label)).toList();
        if (found.size() != 1) {
            throw new AssertionError(source + ": expected one row [" + table + "/" + label
                    + "], found " + found.stream().map(Row::location).toList());
        }
        return found.getFirst();
    }

    Row row(List<String> headers, String label) {
        List<Row> found = rows.stream().filter(r -> r.headers().equals(headers)
                && r.cells().getFirst().equals(label)).toList();
        if (found.size() != 1) {
            throw new AssertionError(source + ": expected one row [" + headers + "/" + label
                    + "], found " + found.stream().map(Row::location).toList());
        }
        return found.getFirst();
    }

    void requireExactRows(List<String> headers, List<String> expectedLabels) {
        List<Row> tableRows = rows.stream().filter(r -> r.headers().equals(headers)).toList();
        if (tableRows.isEmpty()) {
            throw new AssertionError(source + ": missing table [" + headers + "]");
        }
        List<String> duplicateExpected = expectedLabels.stream()
                .filter(label -> expectedLabels.indexOf(label) != expectedLabels.lastIndexOf(label))
                .distinct().toList();
        List<String> duplicateRows = tableRows.stream().map(r -> r.cells().getFirst())
                .filter(label -> tableRows.stream().filter(r -> r.cells().getFirst().equals(label)).count() > 1)
                .distinct().toList();
        List<String> missing = expectedLabels.stream()
                .filter(label -> tableRows.stream().noneMatch(r -> r.cells().getFirst().equals(label))).toList();
        List<String> stale = tableRows.stream().filter(r -> !expectedLabels.contains(r.cells().getFirst()))
                .map(Row::location).toList();
        if (!duplicateExpected.isEmpty() || !duplicateRows.isEmpty() || !missing.isEmpty() || !stale.isEmpty()) {
            String headerLocation = tableRows.getFirst().headerLocation();
            throw new AssertionError(source + ": row coverage for [" + headers + "] at " + headerLocation
                    + " duplicateMapping=" + duplicateExpected + ", duplicateRows=" + duplicateRows
                    + ", missing=" + missing + ", stale=" + stale + ", rows="
                    + tableRows.stream().map(Row::location).toList());
        }
    }

    void expectMatch(String expression, List<Long> expected) {
        Matcher matcher = Pattern.compile(expression).matcher(text);
        if (!matcher.find()) throw new AssertionError(source + ": missing numeric mapping " + expression);
        long line = text.substring(0, matcher.start()).chars().filter(c -> c == '\n').count() + 1;
        String location = source + ":" + line;
        List<Long> actual = new ArrayList<>();
        for (int i = 1; i <= matcher.groupCount(); i++) actual.add(number(matcher.group(i), location));
        compare(location, expression, expected, actual);
        if (matcher.find()) throw new AssertionError(source + ": duplicate numeric mapping " + expression);
    }

    static void requireCoverage(List<String> mapped, List<String> registered) {
        if (new HashSet<>(mapped).size() != mapped.size()) throw new AssertionError("Duplicate item mapping");
        if (!new HashSet<>(mapped).equals(new HashSet<>(registered))) {
            List<String> missing = registered.stream().filter(id -> !mapped.contains(id)).toList();
            List<String> stale = mapped.stream().filter(id -> !registered.contains(id)).toList();
            throw new AssertionError("Item mapping missing=" + missing + ", stale=" + stale);
        }
    }

    private static List<String> split(String line) {
        List<String> result = new ArrayList<>();
        int start = 1;
        boolean wiki = false;
        for (int i = 1; i < line.length(); i++) {
            if (line.startsWith("[[", i)) wiki = true;
            if (line.startsWith("]]", i)) wiki = false;
            if (line.charAt(i) == '|' && !wiki && line.charAt(i - 1) != '\\') {
                result.add(line.substring(start, i).strip());
                start = i + 1;
            }
        }
        if (start < line.length()) result.add(line.substring(start).strip());
        return List.copyOf(result);
    }

    private static long number(String value, String location) {
        String normalized = value.strip().replaceAll("^[×+]", "").replaceAll("%$", "");
        int rank = "一二三四五六七八九".indexOf(normalized);
        if (normalized.length() == 1 && rank >= 0) return rank + 1L;
        if (!normalized.matches("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)")) {
            throw new AssertionError(location + ": invalid integer '" + value + "'");
        }
        try {
            return Long.parseLong(normalized.replace(",", ""));
        } catch (NumberFormatException error) {
            throw new AssertionError(location + ": integer out of range '" + value + "'", error);
        }
    }

    private static void compare(String location, String field, List<Long> expected, List<Long> actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(location + " [" + field + "] code=" + expected + ", wiki=" + actual);
        }
    }

    record Row(String location, String headerLocation, String section, List<String> headers, List<String> cells) {

        String cell(String column) {
            int index = headers.indexOf(column);
            if (index < 0 || index != headers.lastIndexOf(column)) {
                throw new AssertionError(location + ": missing or duplicate column " + column);
            }
            return cells.get(index);
        }

        void expect(String column, List<Long> expected) { expectValue(column, cell(column), expected); }

        void expectValue(String field, String value, List<Long> expected) {
            List<Long> actual;
            if (value.matches("[一二三四五六七八九]至[一二三四五六七八九]")) {
                long from = number(value.substring(0, 1), location);
                long to = number(value.substring(2), location);
                actual = LongStream.rangeClosed(from, to).boxed().toList();
            } else {
                actual = Arrays.stream(value.split("<br>|/|至", -1))
                        .map(v -> number(v, location)).toList();
            }
            compare(location, field, expected, actual);
        }
    }
}
